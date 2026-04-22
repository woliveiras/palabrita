# Spec 03 — AI Engine

## Summary

The `core/ai` module encapsulates all interaction with the local LLM via LiteRT-LM. It is responsible for: managing the Engine lifecycle, generating puzzles in batch, conducting post-guess conversations, parsing JSON responses, and validating puzzles.

## Components

### LlmEngineManager

Singleton (Hilt `@Singleton`) that manages the LiteRT-LM Engine lifecycle.

**Responsibilities:**
- Initialize Engine with `EngineConfig` (modelPath, backend, cacheDir)
- Select backend: GPU preferred, fallback to CPU
- Maintain reference to the active Engine
- Destroy Engine when no longer needed (e.g., model switch)
- Expose state via formal state machine

**State Machine — Engine Lifecycle:**

```
┌───────────────┐
│ UNINITIALIZED │
└──────┬────────┘
       │ Initialize
       ▼
┌───────────────┐
│ INITIALIZING  │
└──┬─────────┬──┘
   │         │
 Success   Failure
   │         │
   ▼         ▼
┌───────┐ ┌───────┐
│ READY │ │ ERROR │
└──┬────┘ └──┬────┘
   │         │
 Destroy   Retry ──→ INITIALIZING
   │
   ▼
┌───────────────┐
│ UNINITIALIZED │
└───────────────┘
```

```kotlin
// Engine state machine definition
val engineStateMachine = StateMachine<EngineState, EngineEvent>(
    initialState = EngineState.Uninitialized,
    transitions = mapOf(
        (EngineState.Uninitialized to EngineEvent.Initialize) to EngineState.Initializing,
        (EngineState.Initializing to EngineEvent.Success) to EngineState.Ready,
        (EngineState.Initializing to EngineEvent.Failure) to EngineState.Error,
        (EngineState.Ready to EngineEvent.Destroy) to EngineState.Uninitialized,
        (EngineState.Error to EngineEvent.Retry) to EngineState.Initializing,
        (EngineState.Error to EngineEvent.Destroy) to EngineState.Uninitialized,
    )
)

sealed class EngineState {
    data object Uninitialized : EngineState()
    data object Initializing : EngineState()
    data object Ready : EngineState()
    data class Error(val message: String) : EngineState()
}

sealed class EngineEvent {
    data object Initialize : EngineEvent()
    data object Success : EngineEvent()
    data class Failure(val message: String) : EngineEvent()
    data object Destroy : EngineEvent()
    data object Retry : EngineEvent()
}
```

**Interface:**

```kotlin
interface LlmEngineManager {
    val state: StateFlow<EngineState>

    suspend fun initialize(modelPath: String)
    suspend fun createConversation(config: ConversationConfig? = null): Conversation
    fun destroy()
    fun isReady(): Boolean
}
```

**Rules:**
- `initialize()` MUST run on Dispatchers.IO (may take ~10s)
- Invalid transitions (e.g., `Initialize` when already `Ready`) are ignored by the state machine
- If the model does not exist at the path, `Failure` transition with a clear message
- When switching models: `Destroy` → `Initialize` with new path
- `AutoCloseable`: Engine and Conversation must be properly closed

### PuzzleGenerator

Generates puzzles in batch using the LLM.

**Interface:**

```kotlin
interface PuzzleGenerator {
    suspend fun generateBatch(
        count: Int,
        language: String,
        targetDifficulty: Int,
        recentWords: List<String>,
        allExistingWords: Set<String>
    ): List<Puzzle>
}
```

**Word exclusion strategy:**
- `recentWords`: last ~50 played words, injected into the prompt as a variety hint for the LLM
- `allExistingWords`: all previously generated words (can reach thousands over the years), used only by `PuzzleValidator` for deterministic checking
- **Why not send everything in the prompt?** With ~3650 words (10 years of use), the exclusion list would consume thousands of context window tokens, degrading inference quality and speed — especially on Gemma 3 1B. The LLM is not reliable at avoiding large lists anyway.

**Generation flow (per puzzle):**

1. Build prompt based on the active model (including `recentWords` in the exclusion list):
   - **Gemma 4 E2B**: system role + function calling + thinking mode
   - **Gemma 3 1B**: prompt-only with JSON instruction
2. Create Conversation (new for each puzzle to avoid contamination)
3. Send prompt via `conversation.sendMessage()`
4. Parse response via `LlmResponseParser`
5. Validate via `PuzzleValidator` (checking against `allExistingWords`)
6. If invalid: retry up to 3x with slightly varied prompts
7. If still invalid: skip (do not add to batch)
8. Words accepted in the batch are added to `allExistingWords` to avoid intra-batch duplicates
9. Close Conversation

**Retry strategy:**
- Attempt 1: standard prompt
- Attempt 2: add instruction "The previous response was invalid. Try again."
- Attempt 3: simplify prompt (remove `recentWords`, relax constraints)

### ChatEngine

Manages post-guess conversations.

**Interface:**

```kotlin
interface ChatEngine {
    suspend fun startConversation(word: String, category: String, language: String): ChatSession
}

interface ChatSession : AutoCloseable {
    fun sendMessage(userMessage: String): Flow<String>  // streaming tokens
    val messageCount: Int
    val isAtLimit: Boolean  // true quando messageCount >= 10
}
```

**Rules:**
- System prompt contextualized with the word and category
- Gemma 4: uses native `system` role in `ConversationConfig.systemInstruction`
- Gemma 3: prepend to the first user message
- Streaming via `conversation.sendMessageAsync(message).collect {}`
- Limit: 10 user messages per session
- Not available in Light mode (caller must check beforehand)

### PromptTemplates

Prompt constants organized by model and use case.

**Prompt language rule:**
- Instructions to the model: always in **English** (better instruction following in Gemma 3/4)
- Generated content (word, hints, category, chat): in the user's language, controlled by the `language` parameter

```kotlin
object PromptTemplates {

    // --- Puzzle Generation ---

    fun puzzleSystemPromptGemma4(): String = """
        You are a word generator for a guessing game.
        Always respond using the provided function. Never add text outside the function call.
    """.trimIndent()

    fun puzzleUserPromptGemma4(
        language: String,
        difficulty: Int,
        minLength: Int,
        maxLength: Int,
        recentWords: List<String>
    ): String {
        val rarity = difficultyToRarity(difficulty)
        return """
            Generate a word for the game.
            Output language: $language
            Difficulty: $difficulty (1=easy, 5=hard)
            Length: $minLength-$maxLength letters
            Word rarity: $rarity
            The word, category, and hints MUST be in $language.
            Avoid these recent words: ${recentWords.joinToString(", ")}
        """.trimIndent()
    }

    fun puzzlePromptGemma3(
        language: String,
        difficulty: Int,
        minLength: Int,
        maxLength: Int,
        recentWords: List<String>
    ): String {
        val rarity = difficultyToRarity(difficulty)
        return """
            You are a word generator for a game. Return ONLY valid JSON, no extra text.

            Schema:
            {"word": "string", "category": "string", "difficulty": number, "hints": ["string","string","string","string","string"]}

            Rules:
            - The word MUST be a common noun in $language, $minLength-$maxLength letters
            - Word rarity: $rarity
            - No proper nouns, no accents, lowercase only
            - difficulty: $difficulty (1=easy, 5=hard)
            - 5 progressive hints: from vaguest to most specific, written in $language
            - Hints MUST NOT contain the word
            - Avoid these recent words: ${recentWords.joinToString(", ")}
        """.trimIndent()
    }

    private fun difficultyToRarity(difficulty: Int): String = when (difficulty) {
        1 -> "very common, everyday word"
        2 -> "common word"
        3 -> "less frequent word"
        4 -> "uncommon word"
        5 -> "rare or technical word"
        else -> "common word"
    }

    // --- Chat ---

    fun chatSystemPrompt(word: String, category: String, language: String): String = """
        You are an educational assistant. The player just guessed the word "$word" (category: $category).
        Answer questions about: word origin, etymology, fun facts, usage in sentences, synonyms, translations to other languages.
        Keep responses short (max 3 paragraphs). Always respond in $language.
    """.trimIndent()
}
```

### LlmResponseParser

Extracts structured JSON from the LLM response.

**Interface:**

```kotlin
interface LlmResponseParser {
    fun parsePuzzle(rawResponse: String): ParseResult<PuzzleResponse>
}

data class PuzzleResponse(
    val word: String,
    val category: String,
    val difficulty: Int,
    val hints: List<String>
)

sealed class ParseResult<T> {
    data class Success<T>(val data: T) : ParseResult<T>()
    data class Error<T>(val reason: String, val rawResponse: String) : ParseResult<T>()
}
```

**Parsing strategy:**
1. Try `kotlinx.serialization.json.Json.decodeFromString<PuzzleResponse>()`
2. If it fails: try to extract JSON via regex (`\{.*\}` with dotAll)
3. If extracted: try to decode again from the substring
4. If everything fails: return `ParseResult.Error` with the reason

### PuzzleValidator

Deterministic validation (without LLM).

**Validation rules:**

| Rule | Criterion | Action if failed |
|---|---|---|
| Word length | Within the range for the difficulty (see `difficultyToWordLength`) | Reject |
| Valid characters | Only `[a-z]` (no accents, no spaces, no hyphens) | Reject |
| Lowercase | Entire word in lowercase | Normalize (toLowerCase) |
| Not duplicated | Word does not exist in the database | Reject |
| Hints count | Exactly 5 hints | Reject |
| Hints do not reveal | No hint contains the word | Reject |
| Difficulty range | 1-5 | Clamp |
| Category not empty | category.isNotBlank() | Reject |

**Interface:**

```kotlin
interface PuzzleValidator {
    suspend fun validate(
        puzzle: PuzzleResponse,
        allExistingWords: Set<String>,
        expectedWordLength: IntRange
    ): ValidationResult
}

sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val reasons: List<String>) : ValidationResult()
}
```

## SamplerConfig by Model

| Parameter | Gemma 4 E2B | Gemma 3 1B | Chat (both) |
|---|---|---|---|
| temperature | 1.0 | 0.7 | 0.9 |
| topK | 64 | 40 | 40 |
| topP | 0.95 | 0.95 | 0.95 |

Gemma 4 uses temperature=1.0 as per the official model card recommendation.

## Function Calling (Gemma 4 only)

For Gemma 4, use native function calling to enforce the JSON schema:

```kotlin
@Tool("Gera um puzzle de palavra para o jogo")
fun generatePuzzle(
    @ToolParam("A palavra gerada, substantivo comum, sem acentos, minúscula")
    word: String,
    @ToolParam("Categoria da palavra")
    category: String,
    @ToolParam("Nível de dificuldade de 1 a 5")
    difficulty: Int,
    @ToolParam("Lista de 5 dicas progressivas, da mais vaga à mais específica")
    hints: List<String>
): String {
    // Parse structured output
}
```

## Acceptance Criteria

- [ ] `LlmEngineManager` initializes with Gemma 4 E2B on a device with ≥8GB RAM
- [ ] `LlmEngineManager` initializes with Gemma 3 1B on a device with 4-8GB RAM
- [ ] `PuzzleGenerator` generates a batch of 7 valid puzzles in <60s (Gemma 4) or <30s (Gemma 3)
- [ ] `LlmResponseParser` parses valid JSON correctly
- [ ] `LlmResponseParser` extracts JSON from responses with extra text via regex fallback
- [ ] `PuzzleValidator` rejects words outside the 5-8 character range
- [ ] `PuzzleValidator` rejects words with accents or special characters
- [ ] `PuzzleValidator` rejects puzzles with hints that contain the word
- [ ] `PuzzleValidator` rejects duplicate words
- [ ] `ChatEngine` streams tokens during response
- [ ] `ChatEngine` respects the 10-message limit per session
- [ ] Retry generates a valid puzzle after failure on the first attempt (tested with mock)
- [ ] Engine is destroyed and recreated correctly when switching models

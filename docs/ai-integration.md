# AI Integration — Technical Reference

## Overview

Palabrita uses LiteRT-LM to run LLM inference directly on Android devices. This document details how to integrate, configure, and use the runtime, including lifecycle management, prompt engineering, response parsing, and fallback strategies.

## LiteRT-LM SDK

### Gradle Dependency

```kotlin
// gradle/libs.versions.toml
[versions]
litertlm = "latest.release"  // Pin a specific version before publishing

[libraries]
litertlm-android = { module = "com.google.ai.edge.litertlm:litertlm-android", version.ref = "litertlm" }
```

```kotlin
// core/ai/build.gradle.kts
dependencies {
    implementation(libs.litertlm.android)
}
```

### Manifest (for GPU backend)

```xml
<!-- AndroidManifest.xml of the app module -->
<uses-native-library android:name="libOpenCL.so" android:required="false"/>
<uses-native-library android:name="libvndksupport.so" android:required="false"/>
```

`android:required="false"` ensures the app works on devices without OpenCL GPU (falls back to CPU).

## Engine Lifecycle

### Initialization

```kotlin
class LlmEngineManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LlmEngineManager {

    private var engine: Engine? = null
    private val _state = MutableStateFlow(EngineState.UNINITIALIZED)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    override suspend fun initialize(modelPath: String) = withContext(Dispatchers.IO) {
        _state.value = EngineState.INITIALIZING

        try {
            val config = EngineConfig(
                modelPath = modelPath,
                backend = selectBackend(),
                cacheDir = context.cacheDir.path
            )

            engine = Engine(config).also { it.initialize() }
            _state.value = EngineState.READY
        } catch (e: Exception) {
            _state.value = EngineState.ERROR(e.message ?: "Unknown error")
        }
    }

    private fun selectBackend(): Backend {
        // GPU preferred, CPU as fallback
        return try {
            Backend.GPU()
        } catch (e: Exception) {
            Backend.CPU()
        }
    }

    override suspend fun createConversation(config: ConversationConfig?): Conversation {
        val eng = engine ?: throw IllegalStateException("Engine not initialized")
        return eng.createConversation(config ?: ConversationConfig())
    }

    override fun destroy() {
        engine?.close()
        engine = null
        _state.value = EngineState.UNINITIALIZED
    }
}
```

### Critical Notes

1. **`initialize()` may take 5-15 seconds** --- NEVER call on the main thread
2. **Engine and Conversation are `AutoCloseable`** --- use `use {}` or close explicitly
3. **One Engine at a time** --- when switching models, destroy the previous one first
4. **GPU may not be available** --- always have CPU fallback
5. **Model must exist on the filesystem** --- verify before initializing

## Conversation Management

### For Puzzle Generation

Create a new Conversation per puzzle (prevents contamination between generations):

```kotlin
suspend fun generateSinglePuzzle(params: PuzzleParams): PuzzleResponse? {
    val config = buildConversationConfig(params)
    val conversation = engineManager.createConversation(config)

    return conversation.use { conv ->
        val prompt = buildPrompt(params)
        val response = conv.sendMessage(prompt)
        val rawText = response.content.filterIsInstance<Content.Text>()
            .joinToString("") { it.text }

        parser.parsePuzzle(rawText).let {
            when (it) {
                is ParseResult.Success -> it.data
                is ParseResult.Error -> null
            }
        }
    }
}
```

### For Post-Game Chat

The `ChatViewModel` uses `LlmSession` for multi-turn chat with streaming. The engine is auto-initialized if necessary.

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val engineManager: LlmEngineManager,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private var session: LlmSession? = null

    // Ensures engine is ready before creating session
    private suspend fun ensureSession(): Boolean {
        if (session != null) return true

        val currentState = engineManager.engineState.value
        if (currentState !is EngineState.Ready) {
            _state.update { it.copy(isEngineLoading = true) }

            // Auto-initialize if Uninitialized
            if (currentState is EngineState.Uninitialized) {
                val modelPath = modelRepository.getConfig().modelPath ?: return false
                engineManager.initialize(modelPath)
            }

            // Observe and wait for Ready (timeout 120s)
            val ready = withTimeoutOrNull(ENGINE_INIT_TIMEOUT_MS) {
                engineManager.engineState.first { it is EngineState.Ready || it is EngineState.Error }
            }
            _state.update { it.copy(isEngineLoading = false) }
            if (ready !is EngineState.Ready) return false
        }

        val systemPrompt = PromptTemplates.chatSystemPrompt(word, category, language)
        session = engineManager.createChatSession(systemPrompt)
        return true
    }
}
```

**Chat flow:**

```
ChatScreen opened
    |
    +-- Engine Ready? ---> Yes ---> Create LlmSession with chatSystemPrompt
    |                  `---> No ---> Show loading, auto-initialize engine
    |
    v
Send initial prompt: "Tell me a fun fact about the word '{word}'"
    |
    v
LlmSession.sendMessageStreaming() -> Flow<String>
    |
    +-- Tokens arrive -> progressively update UiChatMessage.content
    +-- isStreaming = true while tokens flow
    +-- Timeout 60s -> remove message, show error
    `-- Complete -> save to Room, isStreaming = false
```

**Response streaming:**

```kotlin
currentSession.sendMessageStreaming(userText).collect { token ->
    accumulated.append(token)
    _state.update {
        val updated = it.messages.toMutableList()
        updated[updated.lastIndex] = UiChatMessage(
            role = MessageRole.MODEL,
            content = accumulated.toString(),
            isStreaming = true
        )
        it.copy(messages = updated)
    }
}
```

**Chat state:**

```kotlin
data class ChatState(
    val isEngineLoading: Boolean = false,  // engine loading
    val isModelResponding: Boolean = false, // response streaming
    val messages: List<UiChatMessage>,
    val userMessageCount: Int,             // limit: 10 user messages
    val suggestionsVisible: Boolean,       // chips disappear after 1st message
    // ...
)
```

**History restoration:** when the chat reopens with existing history, USER messages are re-sent to `LlmSession` to reconstruct the model's context.

## Prompt Engineering

### Principles

1. **Be extremely specific** --- small models need clear instructions
2. **Request exact format** --- JSON, no extra text
3. **Give negative examples** --- "DO NOT include the word in the hints"
4. **Limit output** --- "maximum 3 paragraphs"
5. **Adapt to the model** --- Gemma 4 has system role + function calling; Qwen3 does not

### ConversationConfig by Model

**Gemma 4 E2B:**

```kotlin
fun buildConversationConfigGemma4(params: PuzzleParams): ConversationConfig {
    return ConversationConfig(
        systemInstruction = PromptTemplates.puzzleSystemPromptGemma4(),
        samplerConfig = SamplerConfig(
            temperature = 1.0f,
            topK = 64,
            topP = 0.95f
        )
        // Function calling via ToolSet (see below)
    )
}
```

**Qwen3 0.6B:**

```kotlin
fun buildConversationConfigQwen3(): ConversationConfig {
    return ConversationConfig(
        // No system instruction (not natively supported)
        samplerConfig = SamplerConfig(
            temperature = 0.7f,
            topK = 40,
            topP = 0.95f
        )
    )
}
```

### Function Calling (Gemma 4)

LiteRT-LM supports function calling via `@Tool` and `@ToolParam` annotations:

```kotlin
class PuzzleToolSet : ToolSet {

    var lastResult: PuzzleResponse? = null
        private set

    @Tool("Generates a word puzzle for the guessing game")
    fun generatePuzzle(
        @ToolParam("Generated word: common noun, no accents, lowercase, 5-8 letters")
        word: String,
        @ToolParam("Word category (e.g., animal, fruit, object)")
        category: String,
        @ToolParam("Difficulty level from 1 (easy) to 5 (hard)")
        difficulty: Int,
        @ToolParam("List of 5 progressive hints, from vaguest to most specific")
        hint1: String,
        @ToolParam("Second hint")
        hint2: String,
        @ToolParam("Third hint")
        hint3: String,
        @ToolParam("Fourth hint")
        hint4: String,
        @ToolParam("Fifth hint, most specific")
        hint5: String
    ): String {
        lastResult = PuzzleResponse(
            word = word.lowercase(),
            category = category,
            difficulty = difficulty.coerceIn(1, 5),
            hints = listOf(hint1, hint2, hint3, hint4, hint5)
        )
        return "OK"
    }
}
```

Use with `automaticToolCalling = true` in `ConversationConfig` so the model calls the function automatically.

### Thinking Mode (Gemma 4)

For puzzle generation, activate thinking mode for more elaborate hints:

```
System: <|think|>
You are a word generator for a guessing game.
...
```

The `<|think|>` token at the start of the system prompt activates the model's internal reasoning. The "thought" output (`<|channel>thought\n...<channel|>`) is automatically separated from the final response by the SDK.

For chat: **DO NOT** use thinking mode (prioritize response speed).

## Response Parsing

### Layered Strategy

```
Raw LLM Response
    |
    v
1. Try JSON.decodeFromString<PuzzleResponse>()
    |
    +-- Success -> ParseResult.Success
    |
    `-- Failure --->  2. Extract JSON via regex
                      |
                      +-- Found JSON -> try decode again
                      |   +-- Success -> ParseResult.Success
                      |   `-- Failure -> ParseResult.Error
                      |
                      `-- Not found -> ParseResult.Error
```

### Extraction Regex

```kotlin
private val JSON_REGEX = Regex("""\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}""", RegexOption.DOT_MATCHES_ALL)


fun extractJson(raw: String): String? {
    return JSON_REGEX.find(raw)?.value
}
```

### Serialization

```kotlin
@Serializable
data class PuzzleResponse(
    val word: String,
    val category: String,
    val difficulty: Int,
    val hints: List<String>
)

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}
```

`ignoreUnknownKeys` and `isLenient` are essential because LLMs frequently add extra fields or use inconsistent formatting.

## Validation

### Pipeline

```
PuzzleResponse (from parser)
    |
    v
PuzzleValidator.validate()
    |
    +-- word.length in 5..8
    +-- word.matches(Regex("[a-z]+"))
    +-- word !in existingWords
    +-- hints.size == 5
    +-- hints.none { it.contains(word, ignoreCase = true) }
    +-- category.isNotBlank()
    `-- difficulty in 1..5
    |
    +-- All ok -> ValidationResult.Valid
    `-- Any failure -> ValidationResult.Invalid(reasons)
```

### Blacklist

Maintain a list of forbidden words (offensive, ambiguous, problematic):

```kotlin
private val BLACKLIST = setOf(
    // offensive or problematic words
    // update as necessary
)

fun isBlacklisted(word: String): Boolean = word in BLACKLIST
```

## Retry Strategy

```
Attempt 1: default prompt
    |
    `-- Failure -> Attempt 2: prompt + "The previous response was invalid. Try again with a different word."
                    |
                    `-- Failure -> Attempt 3: simplified prompt (no exclusion list, relaxed constraints)
                                    |
                                    `-- Failure -> Skip this puzzle
```

Between retries: create new Conversation (clears the context).

## Expected Performance

| Operation | Gemma 4 E2B | Gemma 3 1B |
|---|---|---|
| Engine init | 5-15s | 3-10s |
| Generate 1 puzzle | 3-8s | 2-5s |
| Generate batch of 7 | 30-60s | 15-35s |
| Chat: first response (TTFT) | 1-3s | 0.5-2s |
| Chat: streaming speed | 47-52 tok/s | 47 tok/s |

These values vary significantly by device and backend (CPU vs GPU).

## Troubleshooting

| Problem | Likely Cause | Solution |
|---|---|---|
| `LiteRtLmJniException` on init | Corrupted model or wrong path | Check path, offer re-download |
| Empty response | Prompt too restrictive | Relax constraints on retry |
| Consistently invalid JSON | Model doesn't understand the format | Use function calling (Gemma 4) or simplify schema |
| OOM during inference | Model too large for the device | Check RAM before initializing, suggest smaller model |
| Response in wrong language | Model ignores language instruction | Add "[Respond in {language}]" at the end of the prompt |
| GPU backend crash | OpenCL not supported | Catch exception, fall back to CPU |

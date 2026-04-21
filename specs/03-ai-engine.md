# Spec 03 — AI Engine

## Resumo

O módulo `core/ai` encapsula toda interação com o LLM local via LiteRT-LM. Ele é responsável por: gerenciar o lifecycle do Engine, gerar puzzles em batch, conduzir conversas pós-acerto, parsear respostas JSON e validar puzzles.

## Componentes

### LlmEngineManager

Singleton (Hilt `@Singleton`) que gerencia o lifecycle do LiteRT-LM Engine.

**Responsabilidades:**
- Inicializar Engine com `EngineConfig` (modelPath, backend, cacheDir)
- Selecionar backend: GPU preferido, fallback para CPU
- Manter referência ao Engine ativo
- Destruir Engine quando não mais necessário (ex: troca de modelo)
- Expor estado via state machine formal

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
// Definição da state machine do Engine
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

**Regras:**
- `initialize()` DEVE rodar em Dispatchers.IO (pode levar ~10s)
- Transições inválidas (ex: `Initialize` quando já `Ready`) são ignoradas pela state machine
- Se o modelo não existir no path, transição `Failure` com mensagem clara
- Ao trocar de modelo: `Destroy` → `Initialize` com novo path
- `AutoCloseable`: Engine e Conversation devem ser fechados adequadamente

### PuzzleGenerator

Gera puzzles em batch usando o LLM.

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

**Estratégia de exclusão de palavras:**
- `recentWords`: últimas ~50 palavras jogadas, injetadas no prompt como hint de variedade para o LLM
- `allExistingWords`: todas as palavras já geradas (pode chegar a milhares ao longo dos anos), usadas apenas pelo `PuzzleValidator` para checagem determinística
- **Por que não mandar tudo no prompt?** Com ~3650 palavras (10 anos de uso), a exclusion list consumiria milhares de tokens do context window, degradando a qualidade e velocidade da inferência — especialmente no Gemma 3 1B. O LLM não é confiável para evitar listas grandes de qualquer forma.

**Fluxo de geração (por puzzle):**

1. Montar prompt baseado no modelo ativo (incluindo `recentWords` na exclusion list):
   - **Gemma 4 E2B**: system role + function calling + thinking mode
   - **Gemma 3 1B**: prompt-only com instrução de JSON
2. Criar Conversation (nova para cada puzzle para evitar contaminação)
3. Enviar prompt via `conversation.sendMessage()`
4. Parsear resposta via `LlmResponseParser`
5. Validar via `PuzzleValidator` (checando contra `allExistingWords`)
6. Se inválido: retry até 3x com prompts ligeiramente variados
7. Se ainda inválido: pular (não adicionar ao batch)
8. Palavras aceitas no batch são adicionadas a `allExistingWords` para evitar duplicatas intra-batch
9. Fechar Conversation

**Retry strategy:**
- Tentativa 1: prompt padrão
- Tentativa 2: adicionar instrução "A resposta anterior foi inválida. Tente novamente."
- Tentativa 3: simplificar prompt (remover `recentWords`, relaxar constraints)

### ChatEngine

Gerencia conversas pós-acerto.

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

**Regras:**
- System prompt contextualizado com a palavra e categoria
- Gemma 4: usa `system` role nativo no `ConversationConfig.systemInstruction`
- Gemma 3: prepend no primeiro user message
- Streaming via `conversation.sendMessageAsync(message).collect {}`
- Limite: 10 mensagens do usuário por sessão
- Não disponível em Light mode (caller deve checar antes)

### PromptTemplates

Constantes de prompt organizadas por modelo e use case.

**Regra de idioma dos prompts:**
- Instruções ao modelo: sempre em **inglês** (melhor instruction following em Gemma 3/4)
- Conteúdo gerado (word, hints, category, chat): no idioma do usuário, controlado pelo parâmetro `language`

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

Extrai JSON estruturado da resposta do LLM.

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

**Estratégia de parsing:**
1. Tentar `kotlinx.serialization.json.Json.decodeFromString<PuzzleResponse>()`
2. Se falhar: tentar extrair JSON via regex (`\{.*\}` com dotAll)
3. Se extraiu: tentar decode novamente no substring
4. Se tudo falhar: retornar `ParseResult.Error` com o motivo

### PuzzleValidator

Validação determinística (sem LLM).

**Regras de validação:**

| Regra | Critério | Ação se falhar |
|---|---|---|
| Tamanho da palavra | Dentro do range para a dificuldade (ver `difficultyToWordLength`) | Rejeitar |
| Caracteres válidos | Apenas `[a-z]` (sem acentos, sem espaços, sem hífens) | Rejeitar |
| Minúscula | Toda a palavra em minúscula | Normalizar (toLowerCase) |
| Não duplicada | Palavra não existe no banco | Rejeitar |
| Hints count | Exatamente 5 hints | Rejeitar |
| Hints não revelam | Nenhuma hint contém a palavra | Rejeitar |
| Difficulty range | 1-5 | Clampar |
| Category não vazia | category.isNotBlank() | Rejeitar |

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

## SamplerConfig por Modelo

| Parâmetro | Gemma 4 E2B | Gemma 3 1B | Chat (ambos) |
|---|---|---|---|
| temperature | 1.0 | 0.7 | 0.9 |
| topK | 64 | 40 | 40 |
| topP | 0.95 | 0.95 | 0.95 |

Gemma 4 usa temperature=1.0 conforme recomendação oficial do model card.

## Function Calling (Gemma 4 apenas)

Para Gemma 4, usar function calling nativo para forçar schema JSON:

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

## Critérios de Aceite

- [ ] `LlmEngineManager` inicializa com Gemma 4 E2B em dispositivo com ≥8GB RAM
- [ ] `LlmEngineManager` inicializa com Gemma 3 1B em dispositivo com 4-8GB RAM
- [ ] `PuzzleGenerator` gera batch de 7 puzzles válidos em <60s (Gemma 4) ou <30s (Gemma 3)
- [ ] `LlmResponseParser` parseia JSON válido corretamente
- [ ] `LlmResponseParser` extrai JSON de respostas com texto extra via regex fallback
- [ ] `PuzzleValidator` rejeita palavras fora do range 5-8 caracteres
- [ ] `PuzzleValidator` rejeita palavras com acentos ou caracteres especiais
- [ ] `PuzzleValidator` rejeita puzzles com hints que contêm a palavra
- [ ] `PuzzleValidator` rejeita palavras duplicadas
- [ ] `ChatEngine` faz streaming de tokens durante resposta
- [ ] `ChatEngine` respeita limite de 10 mensagens por sessão
- [ ] Retry gera puzzle válido após falha na primeira tentativa (testado com mock)
- [ ] Engine é destruído e recriado corretamente ao trocar de modelo

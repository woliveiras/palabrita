# AI Integration — Technical Reference

## Overview

Palabrita usa LiteRT-LM para executar inferência de LLM diretamente no dispositivo Android. Este documento detalha como integrar, configurar e usar o runtime, incluindo lifecycle management, prompt engineering, parsing de respostas e estratégias de fallback.

## LiteRT-LM SDK

### Dependência Gradle

```kotlin
// gradle/libs.versions.toml
[versions]
litertlm = "latest.release"  // Fixar versão específica antes de publicar

[libraries]
litertlm-android = { module = "com.google.ai.edge.litertlm:litertlm-android", version.ref = "litertlm" }
```

```kotlin
// core/ai/build.gradle.kts
dependencies {
    implementation(libs.litertlm.android)
}
```

### Manifest (para GPU backend)

```xml
<!-- AndroidManifest.xml do módulo app -->
<uses-native-library android:name="libOpenCL.so" android:required="false"/>
<uses-native-library android:name="libvndksupport.so" android:required="false"/>
```

`android:required="false"` garante que o app funciona em devices sem GPU OpenCL (fallback para CPU).

## Engine Lifecycle

### Inicialização

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
        // GPU preferido, CPU como fallback
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

### Pontos Críticos

1. **`initialize()` pode levar 5-15 segundos** — NUNCA chamar na main thread
2. **Engine e Conversation são `AutoCloseable`** — usar `use {}` ou fechar explicitamente
3. **Um Engine por vez** — ao trocar de modelo, destruir o anterior primeiro
4. **GPU pode não estar disponível** — sempre ter fallback para CPU
5. **Modelo deve existir no filesystem** — verificar antes de inicializar

## Conversation Management

### Para Geração de Puzzles

Criar uma nova Conversation por puzzle (evita contaminação entre gerações):

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

### Para Chat Pós-Jogo

O `ChatViewModel` usa `LlmSession` para chat multi-turn com streaming. O engine é auto-inicializado se necessário.

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val engineManager: LlmEngineManager,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private var session: LlmSession? = null

    // Garante engine pronto antes de criar sessão
    private suspend fun ensureSession(): Boolean {
        if (session != null) return true

        val currentState = engineManager.engineState.value
        if (currentState !is EngineState.Ready) {
            _state.update { it.copy(isEngineLoading = true) }

            // Auto-inicializa se Uninitialized
            if (currentState is EngineState.Uninitialized) {
                val modelPath = modelRepository.getConfig().modelPath ?: return false
                engineManager.initialize(modelPath)
            }

            // Observa e aguarda Ready (timeout 120s)
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

**Fluxo do chat:**

```
ChatScreen aberta
    │
    ├── Engine Ready? ──→ Sim ──→ Cria LlmSession com chatSystemPrompt
    │                  └──→ Não ──→ Mostra loading, auto-inicializa engine
    │
    ▼
Envia prompt inicial: "Conte uma curiosidade sobre a palavra '{word}'"
    │
    ▼
LlmSession.sendMessageStreaming() → Flow<String>
    │
    ├── Tokens chegam → atualiza UiChatMessage.content progressivamente
    ├── isStreaming = true enquanto tokens fluem
    ├── Timeout 60s → remove mensagem, mostra erro
    └── Completo → salva no Room, isStreaming = false
```

**Streaming de resposta:**

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

**Estado do chat:**

```kotlin
data class ChatState(
    val isEngineLoading: Boolean = false,  // loading do engine
    val isModelResponding: Boolean = false, // streaming de resposta
    val messages: List<UiChatMessage>,
    val userMessageCount: Int,             // limite: 10 mensagens do user
    val suggestionsVisible: Boolean,       // chips desaparecem após 1ª mensagem
    // ...
)
```

**Restauração de histórico:** quando o chat reabre com histórico existente, as mensagens USER são re-enviadas ao `LlmSession` para reconstruir o contexto do modelo.
```

## Prompt Engineering

### Princípios

1. **Ser extremamente específico** — modelos pequenos precisam de instruções claras
2. **Pedir formato exato** — JSON, sem texto extra
3. **Dar exemplos negativos** — "NÃO inclua a palavra nas dicas"
4. **Limitar output** — "máximo 3 parágrafos"
5. **Adaptar ao modelo** — Gemma 4 tem system role + function calling; Qwen3 não

### ConversationConfig por Modelo

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
        // Function calling via ToolSet (ver abaixo)
    )
}
```

**Qwen3 0.6B:**

```kotlin
fun buildConversationConfigQwen3(): ConversationConfig {
    return ConversationConfig(
        // Sem system instruction (não suportado nativamente)
        samplerConfig = SamplerConfig(
            temperature = 0.7f,
            topK = 40,
            topP = 0.95f
        )
    )
}
```

### Function Calling (Gemma 4)

LiteRT-LM suporta function calling via annotations `@Tool` e `@ToolParam`:

```kotlin
class PuzzleToolSet : ToolSet {

    var lastResult: PuzzleResponse? = null
        private set

    @Tool("Gera um puzzle de palavra para o jogo de adivinhação")
    fun generatePuzzle(
        @ToolParam("Palavra gerada: substantivo comum, sem acentos, minúscula, 5-8 letras")
        word: String,
        @ToolParam("Categoria da palavra (ex: animal, fruta, objeto)")
        category: String,
        @ToolParam("Nível de dificuldade de 1 (fácil) a 5 (difícil)")
        difficulty: Int,
        @ToolParam("Lista de 5 dicas progressivas, da mais vaga à mais específica")
        hint1: String,
        @ToolParam("Segunda dica")
        hint2: String,
        @ToolParam("Terceira dica")
        hint3: String,
        @ToolParam("Quarta dica")
        hint4: String,
        @ToolParam("Quinta dica, mais específica")
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

Usar com `automaticToolCalling = true` no `ConversationConfig` para que o modelo chame a função automaticamente.

### Thinking Mode (Gemma 4)

Para geração de puzzles, ativar thinking mode para dicas mais elaboradas:

```
System: <|think|>
Você é um gerador de palavras para um jogo de adivinhação.
...
```

O token `<|think|>` no início do system prompt ativa o raciocínio interno do modelo. O output de "pensamento" (`<|channel>thought\n...<channel|>`) é separado automaticamente da resposta final pelo SDK.

Para chat: **NÃO** usar thinking mode (priorizar velocidade de resposta).

## Parsing de Respostas

### Estratégia em Camadas

```
Resposta bruta do LLM
    │
    ▼
1. Tentar JSON.decodeFromString<PuzzleResponse>()
    │
    ├── Sucesso → ParseResult.Success
    │
    └── Falha ──→ 2. Extrair JSON via regex
                      │
                      ├── Encontrou JSON → tentar decode novamente
                      │   ├── Sucesso → ParseResult.Success
                      │   └── Falha → ParseResult.Error
                      │
                      └── Não encontrou → ParseResult.Error
```

### Regex de Extração

```kotlin
private val JSON_REGEX = Regex("""\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}""", RegexOption.DOT_MATCHES_ALL)

fun extractJson(raw: String): String? {
    return JSON_REGEX.find(raw)?.value
}
```

### Serialização

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

`ignoreUnknownKeys` e `isLenient` são essenciais porque LLMs frequentemente adicionam campos extras ou usam formatting inconsistente.

## Validação

### Pipeline

```
PuzzleResponse (do parser)
    │
    ▼
PuzzleValidator.validate()
    │
    ├── word.length in 5..8
    ├── word.matches(Regex("[a-z]+"))
    ├── word !in existingWords
    ├── hints.size == 5
    ├── hints.none { it.contains(word, ignoreCase = true) }
    ├── category.isNotBlank()
    └── difficulty in 1..5
    │
    ├── Tudo ok → ValidationResult.Valid
    └── Qualquer falha → ValidationResult.Invalid(reasons)
```

### Blacklist

Manter lista de palavras proibidas (ofensivas, ambíguas, problemáticas):

```kotlin
private val BLACKLIST = setOf(
    // palavras ofensivas ou problemáticas
    // atualizar conforme necessário
)

fun isBlacklisted(word: String): Boolean = word in BLACKLIST
```

## Retry Strategy

```
Tentativa 1: prompt padrão
    │
    └── Falha → Tentativa 2: prompt + "A resposta anterior foi inválida. Tente novamente com uma palavra diferente."
                    │
                    └── Falha → Tentativa 3: prompt simplificado (sem exclusion list, constraints relaxadas)
                                    │
                                    └── Falha → Pular este puzzle
```

Entre retries: criar nova Conversation (limpa o contexto).

## Performance Esperada

| Operação | Gemma 4 E2B | Gemma 3 1B |
|---|---|---|
| Engine init | 5-15s | 3-10s |
| Gerar 1 puzzle | 3-8s | 2-5s |
| Gerar batch de 7 | 30-60s | 15-35s |
| Chat: primeira resposta (TTFT) | 1-3s | 0.5-2s |
| Chat: velocidade de streaming | 47-52 tok/s | 47 tok/s |

Esses valores variam significativamente por device e backend (CPU vs GPU).

## Troubleshooting

| Problema | Causa provável | Solução |
|---|---|---|
| `LiteRtLmJniException` na init | Modelo corrompido ou path errado | Verificar path, oferecer re-download |
| Resposta vazia | Prompt muito restritivo | Relaxar constraints no retry |
| JSON inválido consistente | Modelo não entende o formato | Usar function calling (Gemma 4) ou simplificar schema |
| OOM durante inferência | Modelo grande demais para o device | Verificar RAM antes de inicializar, sugerir modelo menor |
| Resposta em idioma errado | Modelo ignora instrução de idioma | Adicionar "[Responda em {language}]" no fim do prompt |
| GPU backend crash | OpenCL não suportado | Catch exception, fallback para CPU |

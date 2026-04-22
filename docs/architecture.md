# Architecture — Technical Reference

## Overview

Palabrita segue uma arquitetura multi-módulo Android com separação clara entre camadas de UI, domínio, dados e IA. Este documento explica as decisões técnicas, o grafo de dependências e os padrões utilizados.

## Por que Multi-Módulo?

| Benefício | Impacto |
|---|---|
| Build incremental | Alterar `feature/chat` não recompila `feature/game` |
| Encapsulamento | Módulos expõem apenas interfaces públicas |
| Testabilidade | Cada módulo pode ser testado isoladamente |
| Escalabilidade | Novas features são módulos novos, sem afetar os existentes |
| Portfólio | Demonstra maturidade arquitetural |

## Diagrama de Módulos

```
                          ┌─────────┐
                          │   app   │
                          └────┬────┘
                ┌──────────┬───┴───┬──────────┐
                ▼          ▼       ▼          ▼
        ┌───────────┐ ┌────────┐ ┌──────┐ ┌──────────┐
        │ onboarding│ │  game  │ │ chat │ │ settings │
        └─────┬─────┘ └───┬────┘ └──┬───┘ └────┬─────┘
              │            │         │           │
              └──────┬─────┴────┬────┴───────────┘
                     ▼          ▼
              ┌──────────┐ ┌─────────┐
              │ core/ai  │ │core/data│
              └────┬─────┘ └────┬────┘
                   │            │
                   └─────┬──────┘
                         ▼
                  ┌─────────────┐
                  │ core/model  │
                  └─────────────┘
                         ▲
                  ┌──────┴──────┐
                  │ core/common │
                  └─────────────┘
```

## Padrões Arquiteturais

### Camada UI → ViewModel → Repository

```
Compose Screen
    │ observa StateFlow
    ▼
ViewModel (Hilt @HiltViewModel)
    │ chama suspend functions
    ▼
Repository Interface (em core/model)
    │ implementado em core/data
    ▼
Room DAO / LiteRT-LM Engine
```

### Unidirectional Data Flow (UDF)

Cada feature segue o padrão:

```kotlin
// State imutável
data class GameState(
    val puzzle: Puzzle? = null,
    val chosenDifficulty: Int = 1,    // auto-selecionado via PlayerStats.currentDifficulty
    val attempts: List<Attempt> = emptyList(),
    val currentInput: String = "",
    val revealedHints: List<String> = emptyList(),
    val keyboardState: Map<Char, LetterState> = emptyMap(),
    val gameStatus: GameStatus = GameStatus.LOADING,  // LOADING → PLAYING → WON/LOST
    // ...
)

// Actions discretas — sem SelectDifficulty/StartGame (removidos)
sealed class GameAction {
    data class TypeLetter(val letter: Char) : GameAction()
    data object DeleteLetter : GameAction()
    data object SubmitAttempt : GameAction()
    data object RevealHint : GameAction()
    data object LoadNextPuzzle : GameAction()
    // ...
}

// ViewModel carrega jogo automaticamente na dificuldade do jogador
@HiltViewModel
class GameViewModel @Inject constructor(
    private val puzzleRepository: PuzzleRepository,
    private val statsRepository: StatsRepository,
    // ...
) : ViewModel() {
    init { loadNextGame() }  // auto-start sem picker

    private fun loadNextGame() {
        viewModelScope.launch {
            val stats = statsRepository.getStats()
            val difficulty = stats.currentDifficulty  // 1-5, gerenciado automaticamente
            // busca puzzle e inicia jogo
        }
    }
}
```

### State Machines (core/common)

Para fluxos complexos com muitos estados e transições condicionais, usamos uma mini state machine genérica (~30 linhas). Sem dependência externa.

**Implementação:**

```kotlin
/**
 * State machine genérica e tipada.
 * Transições inválidas são ignoradas (não lançam exceção).
 * Thread-safe via StateFlow.
 */
class StateMachine<S : Any, E : Any>(
    initialState: S,
    private val transitions: Map<Pair<KClass<out S>, KClass<out E>>, (S, E) -> S>
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    val currentState: S get() = _state.value

    fun transition(event: E): Boolean {
        val key = _state.value::class to event::class
        val handler = transitions[key] ?: return false
        _state.value = handler(_state.value, event)
        return true
    }

    companion object {
        /** Builder DSL para definir transições de forma declarativa */
        fun <S : Any, E : Any> create(
            initialState: S,
            block: TransitionBuilder<S, E>.() -> Unit
        ): StateMachine<S, E> {
            val builder = TransitionBuilder<S, E>()
            builder.block()
            return StateMachine(initialState, builder.build())
        }
    }
}

class TransitionBuilder<S : Any, E : Any> {
    private val transitions = mutableMapOf<Pair<KClass<out S>, KClass<out E>>, (S, E) -> S>()

    inline fun <reified FROM : S, reified ON : E> on(noinline handler: (FROM, ON) -> S) {
        @Suppress("UNCHECKED_CAST")
        transitions[FROM::class to ON::class] = handler as (S, E) -> S
    }

    fun build() = transitions.toMap()
}
```

**Uso:**

```kotlin
// Exemplo: Engine lifecycle
val engineSM = StateMachine.create<EngineState, EngineEvent>(
    initialState = EngineState.Uninitialized
) {
    on<EngineState.Uninitialized, EngineEvent.Initialize> { _, _ ->
        EngineState.Initializing
    }
    on<EngineState.Initializing, EngineEvent.Success> { _, _ ->
        EngineState.Ready
    }
    on<EngineState.Initializing, EngineEvent.Failure> { _, event ->
        EngineState.Error(event.message)
    }
    on<EngineState.Ready, EngineEvent.Destroy> { _, _ ->
        EngineState.Uninitialized
    }
    on<EngineState.Error, EngineEvent.Retry> { _, _ ->
        EngineState.Initializing
    }
    on<EngineState.Error, EngineEvent.Destroy> { _, _ ->
        EngineState.Uninitialized
    }
}

// Uso no ViewModel/Manager
engineSM.transition(EngineEvent.Initialize)   // Uninitialized → Initializing
engineSM.transition(EngineEvent.Success)      // Initializing → Ready
engineSM.transition(EngineEvent.Initialize)   // Ready + Initialize → ignorado (retorna false)
```

**Onde é usado no Palabrita:**

| Fluxo | States | Events | Módulo |
|---|---|---|---|
| Engine lifecycle | Uninitialized, Initializing, Ready, Error | Initialize, Success, Failure, Destroy, Retry | `core/ai` |
| Download de modelo | Idle, Checking, WaitingForWifi, Downloading, Completed, Failed | StartDownload, SpaceOk, WifiRequired, Done, Fail, Retry, Cancel | `feature/onboarding` |
| Fluxo de onboarding | Welcome, Language, ModelSelection, Download, Generation, Complete | Next, Back, DownloadComplete, GenerationComplete | `feature/onboarding` |

**Onde NÃO usamos (sealed class + when é suficiente):**

| Fluxo | Motivo |
|---|---|
| Game status (Loading, Playing, Won, Lost) | Poucos estados, transições lineares. Auto-dificuldade baseada no nível do jogador (sem picker). |
| Chat status (Idle, EngineLoading, Responding, AtLimit) | Linear, engine auto-inicializa se necessário |

### Injeção de Dependência (Hilt)

```
@HiltAndroidApp
class PalabritaApp : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity()

// Módulos Hilt por camada:
@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): PalabritaDatabase
}

@Module @InstallIn(SingletonComponent::class)
object AiModule {
    @Provides @Singleton
    fun provideLlmEngineManager(...): LlmEngineManager
}

@Module @InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindPuzzleRepo(impl: PuzzleRepositoryImpl): PuzzleRepository
}
```

### Navegação

Navigation Compose com type-safe routes (Kotlin Serialization):

```kotlin
@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data class GenerationRoute(val modelId: String, val isRegeneration: Boolean)
@Serializable data object GameRoute
@Serializable data class ChatRoute(val puzzleId: Long)
@Serializable data object SettingsRoute
@Serializable data object AiInfoRoute
```

Fluxo de navegação:

```
Start ──→ Onboarding completado? ──→ Sim ──→ Home
                                  └──→ Não ──→ Onboarding ──→ Generation ──→ Home

Home ──→ Jogar ──→ Game (auto-dificuldade via PlayerStats.currentDifficulty)
Home ──→ Gerar Mais ──→ Generation (re-gen)
Game ──→ Win/Lose ──→ Result ──→ Chat (post-game, real LLM com streaming)
Game ──→ No Puzzles Left ──→ Generation (re-gen)

Após cada partida (win/lose), o sistema chama checkAndPromoteDifficulty():
  - Promove: ≥5 wins + ≥70% winRate na dificuldade atual
  - Demove: 3 derrotas consecutivas

Bottom Nav: Home | IA (AiInfoScreen) | Mais (Settings)
```

## Gestão de Estado de Longo Prazo

| Estado | Armazenamento | Escopo |
|---|---|---|
| Puzzles gerados | Room (`PuzzleEntity`) | Permanente |
| Sessão de jogo em andamento | Room (`GameSessionEntity`) | Até completar |
| Estatísticas do jogador | Room (`PlayerStatsEntity`) | Permanente |
| Histórico de chat | Room (`ChatMessageEntity`) | Permanente |
| Configuração do modelo | Room (`ModelConfigEntity`) | Permanente |
| Onboarding completado | DataStore Preferences | Permanente |
| Estado da UI (input, etc.) | ViewModel StateFlow | Lifecycle do ViewModel |

## Concorrência

- **UI**: `Dispatchers.Main` (Compose, ViewModel)
- **Room queries**: `Dispatchers.IO` (Room já faz por default em suspend functions)
- **LLM inference**: `Dispatchers.IO` (via `withContext`)
- **Engine init**: `Dispatchers.IO` (pode levar ~10s)
- **WorkManager**: gerenciado pelo sistema (background thread)
- **Download**: gerenciado pelo Play Asset Delivery / OkHttp

## Tratamento de Erros

Estratégia por camada:

| Camada | Estratégia |
|---|---|
| Repository | Retorna `Result<T>` ou throws (caught pelo ViewModel) |
| ViewModel | Catch, atualiza `state.error`, exibe na UI |
| AI Engine | `ParseResult<T>` sealed class para parsing, retry para geração |
| Download | Estados em `DownloadState` enum, retry explícito |

## Testes

| Tipo | Framework | Foco |
|---|---|---|
| Unit | JUnit 5 + MockK | Validador, parser, algoritmo de dificuldade |
| Integration | Room in-memory + Turbine | DAOs, repositórios, flows |
| UI | Compose Testing | Telas, navegação, interações |
| E2E (manual) | Device físico | Fluxo completo com modelo real |

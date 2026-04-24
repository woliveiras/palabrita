# Architecture — Technical Reference

## Overview

Palabrita follows a multi-module Android architecture with clear separation between UI, domain, data, and AI layers. This document explains the technical decisions, dependency graph, and patterns used.

## Why Multi-Module?

| Benefit | Impact |
|---|---|
| Incremental builds | Changing `feature/chat` does not recompile `feature/game` |
| Encapsulation | Modules expose only public interfaces |
| Testability | Each module can be tested in isolation |
| Scalability | New features are new modules, without affecting existing ones |
| Long-term maintainability | Clear boundaries make large-scale changes safer |

## Module Diagram

```
                          +---------+
                          |   app   |
                          +----+----+
                +---------+---+---+---------+
                v          v       v          v
        +----------+ +------+ +----+ +--------+
        | onboarding| |  game  | | chat | | settings |
        +-----+-----+ +---+----+ +--+---+ +----+-----+
              |            |         |           |
              +------+-----+----+----+-----------+
                     v          v
              +----------+ +---------+
              | core/ai  | |core/data|
              +----+-----+ +----+----+
                   |            |
                   +-----+------+
                         v
                  +-------------+
                  | core/model  |
                  +-------------+
                         ^
                  +------+------+
                  | core/common |
                  +-------------+

  (test-only)
  +---------------+
  | core/testing  | ──→ core/model, core/ai
  +---------------+
```

## Architectural Patterns

### UI Layer -> ViewModel -> Repository

```
Compose Screen
    | observes StateFlow
    v
ViewModel (Hilt @HiltViewModel)
    | calls suspend functions
    v
Repository Interface (in core/model)
    | implemented in core/data
    v
Room DAO / LiteRT-LM Engine
```

### Unidirectional Data Flow (UDF)

Each feature follows the pattern:

```kotlin
// Immutable state
data class GameState(
    val puzzle: Puzzle? = null,
    val attempts: List<Attempt> = emptyList(),
    val currentInput: String = "",
    val revealedHints: List<String> = emptyList(),
    val keyboardState: Map<Char, LetterState> = emptyMap(),
    val gameStatus: GameStatus = GameStatus.LOADING,
    val showShake: Boolean = false,    // triggers shake animation on invalid attempt
    val isLoading: Boolean = false,
    val errorRes: Int? = null,
    val showAbandonDialog: Boolean = false,
)

// Discrete actions -- no SelectDifficulty/StartGame (removed)
sealed class GameAction {
    data class TypeLetter(val letter: Char) : GameAction()
    data object DeleteLetter : GameAction()
    data object SubmitAttempt : GameAction()
    data object RevealHint : GameAction()
    data object LoadNextPuzzle : GameAction()
    data object ClearShake : GameAction()
    // ...
}

// ViewModel automatically loads the next puzzle by word length
@HiltViewModel
class GameViewModel @Inject constructor(
    private val puzzleRepository: PuzzleRepository,
    // ...
) : ViewModel() {
    init { restoreOrLoadNext() }  // restores active session or loads next puzzle

    private fun loadNextGame() {
        viewModelScope.launch {
            // fetches next unplayed puzzle (ordered by word length within batch)
        }
    }
}
```

### State Machines (core/common)

For complex flows with many states and conditional transitions, we use a generic mini state machine (~30 lines). No external dependency.

**Implementation:**

```kotlin
/**
 * Generic, typed state machine.
 * Invalid transitions are ignored (no exception thrown).
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
        /** Builder DSL for declaring transitions in a declarative style */
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

**Usage:**

```kotlin
// Example: Engine lifecycle
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

// Usage in ViewModel/Manager
engineSM.transition(EngineEvent.Initialize)   // Uninitialized -> Initializing
engineSM.transition(EngineEvent.Success)      // Initializing -> Ready
engineSM.transition(EngineEvent.Initialize)   // Ready + Initialize -> ignored (returns false)
```

**Where it is used in Palabrita:**

| Flow | States | Events | Module |
|---|---|---|---|
| Engine lifecycle | Uninitialized, Initializing, Ready, Error | Initialize, Success, Failure, Destroy, Retry | `core/ai` |
| Model download | Idle, Checking, WaitingForWifi, Downloading, Completed, Failed | StartDownload, SpaceOk, WifiRequired, Done, Fail, Retry, Cancel | `feature/onboarding` |
| Onboarding flow | Welcome, Language, ModelSelection, Download, Generation, Complete | Next, Back, DownloadComplete, GenerationComplete | `feature/onboarding` |

**Where we DON'T use it (sealed class + when is sufficient):**

| Flow | Reason |
|---|---|
| Game status (Loading, Playing, Won, Lost) | Few states, linear transitions. No picker — puzzle loaded automatically. |
| Chat status (Idle, EngineLoading, Responding, AtLimit) | Linear, engine auto-initializes if needed |

### Dependency Injection (Hilt)

```
@HiltAndroidApp
class PalabritaApp : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity()

// Hilt modules by layer:
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

### Navigation

Navigation Compose with type-safe routes (Kotlin Serialization):

```kotlin
@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data class GenerationRoute(val modelId: String, val isRegeneration: Boolean)
@Serializable data object GameRoute
@Serializable data class ChatRoute(val puzzleId: Long)
@Serializable data object SettingsRoute
@Serializable data object AiInfoRoute
```

Navigation flow:

```
Start ---> Onboarding completed? ---> Yes ---> Home
                                  `---> No ---> Onboarding ---> Generation ---> Home

Home ---> Play ---> Game (next puzzle by word length from 3-level system)
Home ---> Generate More ---> Generation (re-gen)
Game ---> Win/Lose ---> Result ---> Chat (post-game, real LLM with streaming)
Game ---> No Puzzles Left ---> Generation (re-gen)

Difficulty is implicit via word length (4-6 letters). 3-level system:
Level 1 = 5×4-letter, Level 2 = 10×5-letter, Level 3+ = 10×6-letter (see Spec 15).

Bottom Nav: Home | AI (AiInfoScreen) | More (Settings)
```

## Long-Term State Management

| State | Storage | Scope |
|---|---|---|
| Generated puzzles | Room (`PuzzleEntity`) | Permanent |
| Active game session | Room (`GameSessionEntity`) | Until completed |
| Player statistics | Room (`PlayerStatsEntity`) | Permanent |
| Chat history | Room (`ChatMessageEntity`) | Permanent |
| Model configuration | Room (`ModelConfigEntity`) | Permanent |
| Onboarding completed | DataStore Preferences | Permanent |
| UI state (input, etc.) | ViewModel StateFlow | ViewModel lifecycle |

## Concurrency

- **UI**: `Dispatchers.Main` (Compose, ViewModel)
- **Room queries**: `Dispatchers.IO` (Room already does this by default in suspend functions)
- **LLM inference**: `Dispatchers.IO` (via `withContext`)
- **Engine init**: `Dispatchers.IO` (may take ~10s)
- **WorkManager**: managed by the system (background thread)
- **Download**: managed by Play Asset Delivery / OkHttp

## Error Handling

Strategy by layer:

| Layer | Strategy |
|---|---|
| Repository | Returns `Result<T>` or throws (caught by ViewModel) |
| ViewModel | Catch, updates `state.error`, displays in UI |
| AI Engine | `ParseResult<T>` sealed class for parsing, retry for generation |
| Download | States in `DownloadState` enum, explicit retry |

## Testing

| Type | Framework | Focus |
|---|---|---|
| Unit | JUnit + Truth + Turbine | Validator, parser, game rules, use cases |
| Integration | Room in-memory + Turbine | DAOs, repositories, flows |
| UI | Compose Testing | Screens, navigation, interactions |
| E2E (manual) | Physical device | Full flow with real model |

### Shared Fakes (core/testing)

The `core/testing` module provides fake implementations of all repository interfaces for use in unit tests. Prefer real objects (Fakes) over mocks; mock only at system boundaries.

```kotlin
// Available fakes:
FakePuzzleRepository, FakeChatRepository, FakeGameSessionRepository,
FakeStatsRepository, FakeModelRepository, FakePreferencesRepository,
FakeLlmEngineManager, FakePuzzleGenerator
```

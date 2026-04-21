---
applyTo: "**/*.kt"
---

# Kotlin / Android Conventions

## Architecture: MVVM + UDF
- **ViewModel** exposes `StateFlow<UiState>` — never LiveData
- **UiState** is an immutable `data class` with sensible defaults
- **Actions** are `sealed class` processed by ViewModel (Unidirectional Data Flow)
- **Repository** is an interface in `core/model`; implementation in `core/data`, injected via Hilt
- **Screen** collects state with `collectAsStateWithLifecycle()`

## State Management
- Complex flows (multi-step, async transitions): `StateMachine<S, E>` from `core/common`
- Simple flows (UI state, one-shot): `sealed class` + `when`
- Never use mutable state outside ViewModel

## Hilt DI
- ViewModels: `@HiltViewModel` with `@Inject constructor`
- Repositories: `@Singleton` scope, bound via `@Binds` in `@Module`
- Use constructor injection — never field injection

## Room
- DAOs return `Flow<List<Entity>>` for observable queries
- Use `suspend` for write operations
- Entities live in `core/data`; domain models in `core/model`

## Compose
- Screen composables take `viewModel` as parameter (default `hiltViewModel()`)
- Extract private helper composables for complex UI sections
- Use `Modifier` as first optional parameter in reusable composables
- Follow Material 3 design patterns
- Use `CompositionLocal` for theme tokens (e.g., `LocalGameColors`)

## Multi-module
- Modules depend downward: `feature/*` → `core/*` → no feature dependencies
- `core/model` has no Android dependencies (pure Kotlin)
- `core/common` holds shared utilities (StateMachine, DeviceCapabilities)

## Testing
- Backtick-delimited test names: `` fun `returns error when input is blank`() ``
- Use test builders: `createTestPuzzle()`, `createTestPlayerStats()`
- Fake repositories for ViewModel tests
- `Truth` for assertions, `Turbine` for Flow testing

## Formatting
- Spotless with ktfmt (Google style)
- Run `./gradlew spotlessCheck` before commit

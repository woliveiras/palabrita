# ADR 003 — Custom StateMachine Over External Libraries

## Context

Several flows in Palabrita involve multiple states and conditional transitions: the LLM engine lifecycle, the model download pipeline, and the onboarding sequence. Managing these with ad-hoc `when` expressions leads to scattered transition logic and untestable state mutations.

We need a structured approach to state management for these complex flows.

### Options Evaluated

| Option | Lines of Code | Dependencies Added | Typesafe | Testable |
|---|---|---|---|---|
| **Custom `StateMachine<S, E>`** | ~50 | 0 | ✅ | ✅ |
| [Tinder StateMachine](https://github.com/Tinder/StateMachine) | 0 (library) | 1 | ✅ | ✅ |
| [Kstatemachine](https://github.com/nsk90/kstatemachine) | 0 (library) | 1 | ✅ | ✅ |
| Ad-hoc `when` + `MutableStateFlow` | varies | 0 | Partial | Partial |
| MVI (Orbit, MVI Kotlin) | 0 (library) | 1–2 | ✅ | ✅ |

## Decision

Implement a minimal custom `StateMachine<S, E>` (~50 lines) in `core/common`. Do not add an external state machine library.

## Rationale

1. **Zero additional dependency**: adding a library for ~50 lines of logic increases dependency surface, build time, and future update burden without meaningful benefit.

2. **The use case is narrow**: only three flows use `StateMachine` (engine lifecycle, download, onboarding). Simpler flows use `sealed class + when`, which is sufficient and idiomatic Kotlin.

3. **Full control over the contract**: the custom implementation exposes exactly the API needed — `transition(event): Boolean`, `state: StateFlow<S>`, and a builder DSL. External libraries expose API surface we don't need.

4. **Typesafe by construction**: transitions are declared as `inline reified` functions, giving compile-time safety on state and event types.

5. **Testable**: the machine is a plain Kotlin class with no Android dependencies, fully testable with JUnit and Turbine (Flow assertions).

6. **No hidden behaviours**: external state machine libraries often include features (hierarchical states, history states, side-effect hooks) that introduce complexity and potential bugs in flows where those features are unneeded.

## Implementation

```kotlin
class StateMachine<S : Any, E : Any>(
    initialState: S,
    private val transitions: Map<Pair<KClass<out S>, KClass<out E>>, (S, E) -> S>
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    fun transition(event: E): Boolean {
        val key = _state.value::class to event::class
        val handler = transitions[key] ?: return false
        _state.value = handler(_state.value, event)
        return true
    }
}
```

Invalid transitions return `false` and leave state unchanged — no exceptions thrown, safe for concurrent environments.

## Trade-offs

- **No hierarchical states**: acceptable, as none of the current flows require nested states.
- **Maintenance burden**: the implementation lives in `core/common` and must be maintained internally. At ~50 lines, this is minimal.
- **Discovery**: contributors unfamiliar with the project must learn the custom API. Mitigation: the builder DSL is documented in `docs/architecture.md` with full examples.

## Consequences

- `StateMachine<S, E>` is the canonical solution for multi-step async flows. Do not introduce external state machine libraries without revisiting this ADR.
- Flows that do not require formal state transitions (game status, chat status) continue to use `sealed class + when` inside `ViewModel`.

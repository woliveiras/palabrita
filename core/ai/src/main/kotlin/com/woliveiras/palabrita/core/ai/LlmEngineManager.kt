package com.woliveiras.palabrita.core.ai

import com.woliveiras.palabrita.core.common.StateMachine

/**
 * Manages the LiteRT-LM engine lifecycle. Wraps Engine init/destroy behind a state machine.
 *
 * Implementation will be added when the LiteRT-LM SDK dependency is integrated.
 */
class LlmEngineManager {

    sealed class State {
        data object Uninitialized : State()

        data object Initializing : State()

        data object Ready : State()

        data class Error(val message: String) : State()
    }

    sealed class Event {
        data object Initialize : Event()

        data object Success : Event()

        data class Failure(val message: String) : Event()

        data object Destroy : Event()

        data object Retry : Event()
    }

    val stateMachine =
        StateMachine.create<State, Event>(initialState = State.Uninitialized) {
            on<State.Uninitialized, Event.Initialize> { _, _ -> State.Initializing }
            on<State.Initializing, Event.Success> { _, _ -> State.Ready }
            on<State.Initializing, Event.Failure> { _, event -> State.Error(event.message) }
            on<State.Ready, Event.Destroy> { _, _ -> State.Uninitialized }
            on<State.Error, Event.Retry> { _, _ -> State.Initializing }
            on<State.Error, Event.Destroy> { _, _ -> State.Uninitialized }
        }
}

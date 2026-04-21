package com.woliveiras.palabrita.core.common

import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Generic typed state machine. Invalid transitions are ignored (return false). Thread-safe via
 * StateFlow.
 */
class StateMachine<S : Any, E : Any>(
  initialState: S,
  private val transitions: Map<Pair<KClass<out S>, KClass<out E>>, (S, E) -> S>,
) {
  private val _state = MutableStateFlow(initialState)
  val state: StateFlow<S> = _state.asStateFlow()

  val currentState: S
    get() = _state.value

  fun transition(event: E): Boolean {
    val key = _state.value::class to event::class
    val handler = transitions[key] ?: return false
    _state.value = handler(_state.value, event)
    return true
  }

  companion object {
    fun <S : Any, E : Any> create(
      initialState: S,
      block: TransitionBuilder<S, E>.() -> Unit,
    ): StateMachine<S, E> {
      val builder = TransitionBuilder<S, E>()
      builder.block()
      return StateMachine(initialState, builder.build())
    }
  }
}

class TransitionBuilder<S : Any, E : Any> {
  @PublishedApi
  internal val transitions = mutableMapOf<Pair<KClass<out S>, KClass<out E>>, (S, E) -> S>()

  inline fun <reified FROM : S, reified ON : E> on(noinline handler: (FROM, ON) -> S) {
    @Suppress("UNCHECKED_CAST")
    transitions[FROM::class to ON::class] = handler as (S, E) -> S
  }

  fun build() = transitions.toMap()
}

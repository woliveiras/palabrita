package com.woliveiras.palabrita.core.ai

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.ai.LlmEngineManager.Event
import com.woliveiras.palabrita.core.ai.LlmEngineManager.State
import org.junit.Test

class LlmEngineManagerTest {

  @Test
  fun `initial state is Uninitialized`() {
    val manager = LlmEngineManager()
    assertThat(manager.stateMachine.currentState).isEqualTo(State.Uninitialized)
  }

  @Test
  fun `Initialize transitions from Uninitialized to Initializing`() {
    val manager = LlmEngineManager()
    val accepted = manager.stateMachine.transition(Event.Initialize)
    assertThat(accepted).isTrue()
    assertThat(manager.stateMachine.currentState).isEqualTo(State.Initializing)
  }

  @Test
  fun `Success transitions from Initializing to Ready`() {
    val manager = LlmEngineManager()
    manager.stateMachine.transition(Event.Initialize)
    val accepted = manager.stateMachine.transition(Event.Success)
    assertThat(accepted).isTrue()
    assertThat(manager.stateMachine.currentState).isEqualTo(State.Ready)
  }

  @Test
  fun `Failure transitions from Initializing to Error`() {
    val manager = LlmEngineManager()
    manager.stateMachine.transition(Event.Initialize)
    val accepted = manager.stateMachine.transition(Event.Failure("model not found"))
    assertThat(accepted).isTrue()
    assertThat(manager.stateMachine.currentState).isInstanceOf(State.Error::class.java)
    assertThat((manager.stateMachine.currentState as State.Error).message).isEqualTo("model not found")
  }

  @Test
  fun `Destroy transitions from Ready to Uninitialized`() {
    val manager = LlmEngineManager()
    manager.stateMachine.transition(Event.Initialize)
    manager.stateMachine.transition(Event.Success)
    val accepted = manager.stateMachine.transition(Event.Destroy)
    assertThat(accepted).isTrue()
    assertThat(manager.stateMachine.currentState).isEqualTo(State.Uninitialized)
  }

  @Test
  fun `Retry transitions from Error to Initializing`() {
    val manager = LlmEngineManager()
    manager.stateMachine.transition(Event.Initialize)
    manager.stateMachine.transition(Event.Failure("timeout"))
    val accepted = manager.stateMachine.transition(Event.Retry)
    assertThat(accepted).isTrue()
    assertThat(manager.stateMachine.currentState).isEqualTo(State.Initializing)
  }

  @Test
  fun `Destroy transitions from Error to Uninitialized`() {
    val manager = LlmEngineManager()
    manager.stateMachine.transition(Event.Initialize)
    manager.stateMachine.transition(Event.Failure("crash"))
    val accepted = manager.stateMachine.transition(Event.Destroy)
    assertThat(accepted).isTrue()
    assertThat(manager.stateMachine.currentState).isEqualTo(State.Uninitialized)
  }

  @Test
  fun `invalid transition is ignored`() {
    val manager = LlmEngineManager()
    val accepted = manager.stateMachine.transition(Event.Success) // invalid from Uninitialized
    assertThat(accepted).isFalse()
    assertThat(manager.stateMachine.currentState).isEqualTo(State.Uninitialized)
  }

  @Test
  fun `Initialize when already Ready is ignored`() {
    val manager = LlmEngineManager()
    manager.stateMachine.transition(Event.Initialize)
    manager.stateMachine.transition(Event.Success)
    val accepted = manager.stateMachine.transition(Event.Initialize)
    assertThat(accepted).isFalse()
    assertThat(manager.stateMachine.currentState).isEqualTo(State.Ready)
  }

  @Test
  fun `full lifecycle Initialize to Ready to Destroy to Initialize again`() {
    val manager = LlmEngineManager()
    manager.stateMachine.transition(Event.Initialize)
    manager.stateMachine.transition(Event.Success)
    assertThat(manager.stateMachine.currentState).isEqualTo(State.Ready)

    manager.stateMachine.transition(Event.Destroy)
    assertThat(manager.stateMachine.currentState).isEqualTo(State.Uninitialized)

    manager.stateMachine.transition(Event.Initialize)
    assertThat(manager.stateMachine.currentState).isEqualTo(State.Initializing)
  }
}

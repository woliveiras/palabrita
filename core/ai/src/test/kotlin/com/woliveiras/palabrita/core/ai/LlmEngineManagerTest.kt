package com.woliveiras.palabrita.core.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EngineStateTest {

  @Test
  fun `EngineState Uninitialized is data object`() {
    assertThat(EngineState.Uninitialized).isInstanceOf(EngineState::class.java)
  }

  @Test
  fun `EngineState Initializing is data object`() {
    assertThat(EngineState.Initializing).isInstanceOf(EngineState::class.java)
  }

  @Test
  fun `EngineState Ready is data object`() {
    assertThat(EngineState.Ready).isInstanceOf(EngineState::class.java)
  }

  @Test
  fun `EngineState Error carries message`() {
    val error = EngineState.Error("model not found")
    assertThat(error.message).isEqualTo("model not found")
  }

  @Test
  fun `all states are distinct`() {
    val states: Set<EngineState> =
      setOf(
        EngineState.Uninitialized,
        EngineState.Initializing,
        EngineState.Ready,
        EngineState.Error("err"),
      )
    assertThat(states).hasSize(4)
  }
}

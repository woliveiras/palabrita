package com.woliveiras.palabrita.feature.chat

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.PromptTemplates
import com.woliveiras.palabrita.core.model.ChatMessage
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.MessageRole
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.PuzzleSource
import com.woliveiras.palabrita.core.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // --- Initial state ---

  @Test
  fun `initial state has empty messages`() = runTest {
    val vm = createViewModel()
    assertThat(vm.state.value.messages).isEmpty()
  }

  @Test
  fun `initial input is empty`() = runTest {
    val vm = createViewModel()
    assertThat(vm.state.value.currentInput).isEmpty()
  }

  // --- Loading chat ---

  @Test
  fun `loads puzzle word on init`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.word).isEqualTo("gatos")
  }

  @Test
  fun `sends initial model message when no history exists`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.messages).isNotEmpty()
    assertThat(vm.state.value.messages[0].role).isEqualTo(MessageRole.MODEL)
  }

  @Test
  fun `restores existing messages from repository`() = runTest {
    val repo =
      FakeChatRepository(
        existingMessages =
          listOf(
            ChatMessage(1, 1L, MessageRole.MODEL, "Olá!", 1000),
            ChatMessage(2, 1L, MessageRole.USER, "Oi", 2000),
          )
      )
    val vm = createViewModel(chatRepo = repo)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.messages).hasSize(2)
    assertThat(vm.state.value.messages[0].role).isEqualTo(MessageRole.MODEL)
    assertThat(vm.state.value.messages[1].role).isEqualTo(MessageRole.USER)
  }

  @Test
  fun `does not send initial message when history exists`() = runTest {
    val repo =
      FakeChatRepository(
        existingMessages = listOf(ChatMessage(1, 1L, MessageRole.MODEL, "Already here", 1000))
      )
    val vm = createViewModel(chatRepo = repo)
    testDispatcher.scheduler.advanceUntilIdle()
    // Should have exactly 1 message (the existing one), not 2
    assertThat(vm.state.value.messages).hasSize(1)
    assertThat(vm.state.value.messages[0].content).isEqualTo("Already here")
  }

  @Test
  fun `suggestions visible when no history exists`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    // After initial model message, suggestions should still be visible
    // (they hide only after first USER message)
    assertThat(vm.state.value.suggestionsVisible).isTrue()
  }

  @Test
  fun `suggestions hidden when history exists`() = runTest {
    val repo =
      FakeChatRepository(
        existingMessages =
          listOf(
            ChatMessage(1, 1L, MessageRole.MODEL, "Hi", 1000),
            ChatMessage(2, 1L, MessageRole.USER, "Hello", 2000),
          )
      )
    val vm = createViewModel(chatRepo = repo)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.suggestionsVisible).isFalse()
  }

  // --- Input ---

  @Test
  fun `updating input updates state`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ChatAction.UpdateInput("test"))
    assertThat(vm.state.value.currentInput).isEqualTo("test")
  }

  // --- Sending messages ---

  @Test
  fun `sending message adds user message to list`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ChatAction.UpdateInput("De onde vem?"))
    vm.onAction(ChatAction.SendMessage)
    testDispatcher.scheduler.advanceUntilIdle()
    val userMessages = vm.state.value.messages.filter { it.role == MessageRole.USER }
    assertThat(userMessages).hasSize(1)
    assertThat(userMessages[0].content).isEqualTo("De onde vem?")
  }

  @Test
  fun `sending message clears input`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ChatAction.UpdateInput("pergunta"))
    vm.onAction(ChatAction.SendMessage)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentInput).isEmpty()
  }

  @Test
  fun `sending message triggers model response`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ChatAction.UpdateInput("pergunta"))
    vm.onAction(ChatAction.SendMessage)
    testDispatcher.scheduler.advanceUntilIdle()
    // Should have: initial model + user + model response = 3 messages
    assertThat(vm.state.value.messages).hasSize(3)
    assertThat(vm.state.value.messages.last().role).isEqualTo(MessageRole.MODEL)
  }

  @Test
  fun `sending message increments user message count`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ChatAction.UpdateInput("pergunta"))
    vm.onAction(ChatAction.SendMessage)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.userMessageCount).isEqualTo(1)
  }

  @Test
  fun `sending empty message is ignored`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ChatAction.UpdateInput("   "))
    vm.onAction(ChatAction.SendMessage)
    testDispatcher.scheduler.advanceUntilIdle()
    val userMessages = vm.state.value.messages.filter { it.role == MessageRole.USER }
    assertThat(userMessages).isEmpty()
  }

  @Test
  fun `sending message hides suggestions`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ChatAction.UpdateInput("pergunta"))
    vm.onAction(ChatAction.SendMessage)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.suggestionsVisible).isFalse()
  }

  @Test
  fun `messages are persisted to repository`() = runTest {
    val repo = FakeChatRepository()
    val vm = createViewModel(chatRepo = repo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ChatAction.UpdateInput("pergunta"))
    vm.onAction(ChatAction.SendMessage)
    testDispatcher.scheduler.advanceUntilIdle()
    // Initial model message + user message + model response = 3 saved
    assertThat(repo.savedMessages).hasSize(3)
  }

  // --- Message limit ---

  @Test
  fun `reaching message limit sets isAtLimit`() = runTest {
    val repo =
      FakeChatRepository(
        existingMessages =
          (1..9).map { i -> ChatMessage(i.toLong(), 1L, MessageRole.USER, "msg$i", 1000L * i) }
      )
    val vm = createViewModel(chatRepo = repo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ChatAction.UpdateInput("last question"))
    vm.onAction(ChatAction.SendMessage)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.isAtLimit).isTrue()
  }

  @Test
  fun `cannot send message when at limit`() = runTest {
    val repo =
      FakeChatRepository(
        existingMessages =
          (1..10).map { i -> ChatMessage(i.toLong(), 1L, MessageRole.USER, "msg$i", 1000L * i) }
      )
    val vm = createViewModel(chatRepo = repo)
    testDispatcher.scheduler.advanceUntilIdle()
    val msgCountBefore = vm.state.value.messages.size
    vm.onAction(ChatAction.UpdateInput("blocked"))
    vm.onAction(ChatAction.SendMessage)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.messages.size).isEqualTo(msgCountBefore)
  }

  // --- Suggestions ---

  @Test
  fun `selecting suggestion sends it as message`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ChatAction.SelectSuggestion("De onde vem essa palavra?"))
    testDispatcher.scheduler.advanceUntilIdle()
    val userMessages = vm.state.value.messages.filter { it.role == MessageRole.USER }
    assertThat(userMessages).hasSize(1)
    assertThat(userMessages[0].content).isEqualTo("De onde vem essa palavra?")
  }

  @Test
  fun `selecting suggestion hides suggestions`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ChatAction.SelectSuggestion("Me dá sinônimos"))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.suggestionsVisible).isFalse()
  }

  // --- Engine state ---

  @Test
  fun `shows error when engine not ready and no model path`() = runTest {
    val engine = FakeLlmEngineManager(initialState = EngineState.Uninitialized)
    val modelRepo = FakeModelRepository(ModelConfig())
    val vm = createViewModel(engineManager = engine, modelRepo = modelRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.error).isNotNull()
    assertThat(vm.state.value.isEngineLoading).isFalse()
  }

  @Test
  fun `initializes engine and sends initial message when uninitialized`() = runTest {
    val engine = FakeLlmEngineManager(initialState = EngineState.Uninitialized)
    val vm = createViewModel(engineManager = engine)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.messages).isNotEmpty()
    assertThat(vm.state.value.messages[0].role).isEqualTo(MessageRole.MODEL)
    assertThat(vm.state.value.isEngineLoading).isFalse()
  }

  // --- Helpers ---

  private fun createTestPuzzle() =
    Puzzle(
      id = 1,
      word = "gatos",
      wordDisplay = "GATOS",
      language = "pt",
      difficulty = 1,
      category = "",
      hints = listOf("Dica 1", "Dica 2", "Dica 3"),
      source = PuzzleSource.AI,
      generatedAt = 1000,
    )

  private fun createViewModel(
    chatRepo: FakeChatRepository = FakeChatRepository(),
    engineManager: FakeLlmEngineManager = FakeLlmEngineManager(),
    modelRepo: FakeModelRepository =
      FakeModelRepository(
        ModelConfig(
          modelId = ModelId.GEMMA4_E2B,
          downloadState = DownloadState.DOWNLOADED,
          modelPath = "/fake/model/path",
        )
      ),
    puzzleRepo: FakePuzzleRepository = FakePuzzleRepository(createTestPuzzle()),
    gameSessionRepo: FakeGameSessionRepository = FakeGameSessionRepository(),
    puzzleId: Long = 1L,
  ): ChatViewModel {
    val savedState = SavedStateHandle(mapOf("puzzleId" to puzzleId))
    return ChatViewModel(
      savedStateHandle = savedState,
      chatRepository = chatRepo,
      puzzleRepository = puzzleRepo,
      gameSessionRepository = gameSessionRepo,
      engineManager = engineManager,
      modelRepository = modelRepo,
      promptProvider = PromptTemplates,
    )
  }
}

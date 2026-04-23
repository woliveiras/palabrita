package com.woliveiras.palabrita.feature.chat

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import com.woliveiras.palabrita.core.ai.LlmSession
import com.woliveiras.palabrita.core.model.ChatMessage
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.MessageRole
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.PuzzleSource
import com.woliveiras.palabrita.core.model.repository.ChatRepository
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
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
  fun `loads puzzle word and category on init`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.word).isEqualTo("gatos")
    assertThat(vm.state.value.category).isEqualTo("Animal")
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
          ),
        userMessageCount = 1,
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
    val repo = FakeChatRepository(userMessageCount = 9)
    val vm = createViewModel(chatRepo = repo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ChatAction.UpdateInput("last question"))
    vm.onAction(ChatAction.SendMessage)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.isAtLimit).isTrue()
  }

  @Test
  fun `cannot send message when at limit`() = runTest {
    val repo = FakeChatRepository(userMessageCount = 10)
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
    val modelRepo = FakeModelRepository(modelPath = null)
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
      category = "Animal",
      hints = listOf("Dica 1", "Dica 2", "Dica 3", "Dica 4", "Dica 5"),
      source = PuzzleSource.AI,
      generatedAt = 1000,
    )

  private fun createViewModel(
    chatRepo: FakeChatRepository = FakeChatRepository(),
    engineManager: FakeLlmEngineManager = FakeLlmEngineManager(),
    modelRepo: FakeModelRepository = FakeModelRepository(),
    puzzleId: Long = 1L,
  ): ChatViewModel {
    val savedState = SavedStateHandle(mapOf("puzzleId" to puzzleId))
    return ChatViewModel(
      savedStateHandle = savedState,
      chatRepository = chatRepo,
      engineManager = engineManager,
      modelRepository = modelRepo,
    )
  }
}

private class FakeLlmSession(private val response: String = "Fake LLM response") : LlmSession {
  override suspend fun sendMessage(message: String): String = response

  override fun sendMessageStreaming(message: String): Flow<String> = flowOf(response)

  override fun close() {}
}

private class FakeLlmEngineManager(
  initialState: EngineState = EngineState.Ready,
  private val sessionResponse: String = "Fake LLM response",
) : LlmEngineManager {
  private val _engineState = MutableStateFlow(initialState)
  override val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

  override suspend fun initialize(modelPath: String) {
    _engineState.value = EngineState.Ready
  }

  override suspend fun generateSingleTurn(systemPrompt: String?, userPrompt: String): String =
    sessionResponse

  override suspend fun createChatSession(systemPrompt: String): LlmSession =
    FakeLlmSession(sessionResponse)

  override fun destroy() {
    _engineState.value = EngineState.Uninitialized
  }

  override fun isReady(): Boolean = _engineState.value is EngineState.Ready
}

private class FakeChatRepository(
  private val existingMessages: List<ChatMessage> = emptyList(),
  private var userMessageCount: Int = existingMessages.count { it.role == MessageRole.USER },
  private val puzzle: Puzzle =
    Puzzle(
      id = 1,
      word = "gatos",
      wordDisplay = "GATOS",
      language = "pt",
      difficulty = 1,
      category = "Animal",
      hints = listOf("Dica 1", "Dica 2", "Dica 3", "Dica 4", "Dica 5"),
      source = PuzzleSource.AI,
      generatedAt = 1000,
    ),
) : ChatRepository {
  val savedMessages = mutableListOf<ChatMessage>()

  override suspend fun getMessages(puzzleId: Long): List<ChatMessage> = existingMessages

  override suspend fun saveMessage(message: ChatMessage) {
    savedMessages.add(message)
    if (message.role == MessageRole.USER) userMessageCount++
  }

  override suspend fun countUserMessages(puzzleId: Long): Int = userMessageCount

  override suspend fun getPuzzle(puzzleId: Long): Puzzle? = puzzle

  override suspend fun deleteAll() {}
}

private class FakeModelRepository(private val modelPath: String? = "/fake/model/path") :
  ModelRepository {
  private var config =
    ModelConfig(
      modelId = ModelId.GEMMA4_E2B,
      downloadState = DownloadState.DOWNLOADED,
      modelPath = modelPath,
    )

  override suspend fun getConfig(): ModelConfig = config

  override suspend fun updateConfig(config: ModelConfig) {
    this.config = config
  }

  override fun observeConfig(): kotlinx.coroutines.flow.Flow<ModelConfig> =
    kotlinx.coroutines.flow.flowOf(config)
}

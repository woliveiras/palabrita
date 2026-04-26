package com.woliveiras.palabrita.core.model.usecase

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.model.ChatMessage
import com.woliveiras.palabrita.core.model.GameSession
import com.woliveiras.palabrita.core.model.MessageRole
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.repository.ChatRepository
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResetProgressUseCaseTest {

  @Test
  fun `resets all repositories`() = runTest {
    val stats = InlineStatsRepository(PlayerStats(totalPlayed = 10, totalWon = 5))
    val sessions = InlineGameSessionRepository()
    sessions.sessions.add(GameSession(id = 1, puzzleId = 1, startedAt = 0L))
    val chat = InlineChatRepository()
    chat.messages.add(
      ChatMessage(puzzleId = 1, role = MessageRole.USER, content = "hi", timestamp = 0)
    )
    val puzzles = InlinePuzzleRepository()
    val prefs = InlineAppPreferences(cycle = 3)

    val useCase = ResetProgressUseCase(stats, sessions, chat, puzzles, prefs)
    useCase()

    assertThat(stats.stats.totalPlayed).isEqualTo(0)
    assertThat(stats.stats.totalWon).isEqualTo(0)
    assertThat(sessions.sessions).isEmpty()
    assertThat(chat.messages).isEmpty()
    assertThat(puzzles.allDeleted).isTrue()
    assertThat(prefs.cycleValue).isEqualTo(0)
  }

  // Minimal inline fakes scoped to this test
  private class InlineStatsRepository(var stats: PlayerStats) : StatsRepository {
    private val _flow = MutableStateFlow(stats)

    override suspend fun getStats() = stats

    override suspend fun updateAfterGame(won: Boolean, attempts: Int, hintsUsed: Int) {}

    override suspend fun updateLanguage(language: String) {}

    override suspend fun resetProgress() {
      stats = PlayerStats(preferredLanguage = stats.preferredLanguage)
      _flow.value = stats
    }

    override fun observeStats(): Flow<PlayerStats> = _flow
  }

  private class InlineGameSessionRepository : GameSessionRepository {
    val sessions = mutableListOf<GameSession>()

    override suspend fun create(session: GameSession): Long = 0

    override suspend fun update(session: GameSession) {}

    override suspend fun getByPuzzleId(puzzleId: Long) = null

    override suspend fun getActiveSession() = null

    override suspend fun hasActiveGame() = false

    override suspend fun completeSession(
      puzzleId: Long,
      attempts: List<String>,
      completedAt: Long,
      hintsUsed: Int,
      won: Boolean,
    ) {}

    override suspend fun markChatExplored(puzzleId: Long) {}

    override suspend fun getCurrentStreak(): Int = 0

    override suspend fun deleteAll() {
      sessions.clear()
    }
  }

  private class InlineChatRepository : ChatRepository {
    val messages = mutableListOf<ChatMessage>()

    override suspend fun getMessages(puzzleId: Long) = messages.toList()

    override suspend fun saveMessage(message: ChatMessage) {}

    override suspend fun countUserMessages(puzzleId: Long) = 0

    override suspend fun deleteAll() {
      messages.clear()
    }
  }

  private class InlinePuzzleRepository : PuzzleRepository {
    var allDeleted = false

    override suspend fun getNextUnplayed(language: String) = null

    override suspend fun countAllUnplayed(language: String) = 0

    override fun observeUnplayedCount(language: String): kotlinx.coroutines.flow.Flow<Int> =
      kotlinx.coroutines.flow.flowOf(0)

    override suspend fun getAllGeneratedWords(): Set<String> = emptySet()

    override suspend fun getRecentWords(limit: Int) = emptyList<String>()

    override suspend fun savePuzzle(puzzle: Puzzle) = 0L

    override suspend fun savePuzzles(puzzles: List<Puzzle>) {}

    override suspend fun markAsPlayed(puzzleId: Long) {}

    override suspend fun deleteUnplayedAiPuzzles() {}

    override suspend fun deleteAllAiPuzzles() {}

    override suspend fun deleteUnplayedByLanguage(language: String) {}

    override suspend fun markAllUnplayed() {}

    override suspend fun deleteAll() {
      allDeleted = true
    }

    override suspend fun getById(id: Long) = null
  }

  private class InlineAppPreferences(cycle: Int = 0) : AppPreferences {
    private val _onboarding = MutableStateFlow(false)
    override val isOnboardingComplete: Flow<Boolean> = _onboarding

    private val _cycle = MutableStateFlow(cycle)
    override val generationCycle: Flow<Int> = _cycle
    val cycleValue: Int
      get() = _cycle.value

    private val _appLanguage = MutableStateFlow("en")
    override val appLanguage: Flow<String> = _appLanguage

    private val _themeMode = MutableStateFlow(com.woliveiras.palabrita.core.model.ThemeMode.SYSTEM)
    override val themeMode: Flow<com.woliveiras.palabrita.core.model.ThemeMode> = _themeMode

    override suspend fun setOnboardingComplete() {
      _onboarding.value = true
    }

    override suspend fun incrementGenerationCycle() {
      _cycle.value += 1
    }

    override suspend fun resetGenerationCycle() {
      _cycle.value = 0
    }

    override suspend fun setAppLanguage(language: String) {
      _appLanguage.value = language
    }

    override suspend fun setThemeMode(mode: com.woliveiras.palabrita.core.model.ThemeMode) {
      _themeMode.value = mode
    }
  }
}

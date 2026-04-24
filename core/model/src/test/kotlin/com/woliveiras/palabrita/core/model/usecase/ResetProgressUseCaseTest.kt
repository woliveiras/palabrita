package com.woliveiras.palabrita.core.model.usecase

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.model.ChatMessage
import com.woliveiras.palabrita.core.model.GameSession
import com.woliveiras.palabrita.core.model.MessageRole
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.Puzzle
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

    val useCase = ResetProgressUseCase(stats, sessions, chat, puzzles)
    useCase()

    assertThat(stats.stats.totalPlayed).isEqualTo(0)
    assertThat(stats.stats.totalWon).isEqualTo(0)
    assertThat(sessions.sessions).isEmpty()
    assertThat(chat.messages).isEmpty()
    assertThat(puzzles.allMarkedUnplayed).isTrue()
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
    var allMarkedUnplayed = false

    override suspend fun getNextUnplayed(language: String) = null

    override suspend fun countAllUnplayed(language: String) = 0

    override suspend fun getAllGeneratedWords(): Set<String> = emptySet()

    override suspend fun getRecentWords(limit: Int) = emptyList<String>()

    override suspend fun savePuzzle(puzzle: Puzzle) = 0L

    override suspend fun savePuzzles(puzzles: List<Puzzle>) {}

    override suspend fun markAsPlayed(puzzleId: Long) {}

    override suspend fun deleteUnplayedAiPuzzles() {}

    override suspend fun markAllUnplayed() {
      allMarkedUnplayed = true
    }

    override suspend fun getById(id: Long) = null
  }
}

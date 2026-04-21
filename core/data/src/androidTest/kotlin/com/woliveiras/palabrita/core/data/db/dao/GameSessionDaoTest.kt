package com.woliveiras.palabrita.core.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.data.db.PalabritaDatabase
import com.woliveiras.palabrita.core.data.db.createTestGameSessionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameSessionDaoTest {

  private lateinit var database: PalabritaDatabase
  private lateinit var dao: GameSessionDao

  @Before
  fun setup() {
    database =
      Room.inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext(),
          PalabritaDatabase::class.java,
        )
        .allowMainThreadQueries()
        .build()
    dao = database.gameSessionDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun insert_returnsGeneratedId() = runTest {
    val session = createTestGameSessionEntity(puzzleId = 42)

    val id = dao.insert(session)

    assertThat(id).isGreaterThan(0)
  }

  @Test
  fun getByPuzzleId_returnsCorrectSession() = runTest {
    val session = createTestGameSessionEntity(puzzleId = 42, hintsUsed = 3, won = true)
    dao.insert(session)

    val result = dao.getByPuzzleId(42)

    assertThat(result).isNotNull()
    assertThat(result!!.puzzleId).isEqualTo(42)
    assertThat(result.hintsUsed).isEqualTo(3)
    assertThat(result.won).isTrue()
  }

  @Test
  fun getByPuzzleId_returnsNullForNonExistent() = runTest {
    val result = dao.getByPuzzleId(999)

    assertThat(result).isNull()
  }

  @Test
  fun update_modifiesSession() = runTest {
    val session = createTestGameSessionEntity(puzzleId = 42)
    val id = dao.insert(session)

    val updated =
      session.copy(
        id = id,
        attempts = """["gatos","campo"]""",
        completedAt = System.currentTimeMillis(),
        won = true,
      )
    dao.update(updated)

    val result = dao.getByPuzzleId(42)
    assertThat(result!!.attempts).isEqualTo("""["gatos","campo"]""")
    assertThat(result.completedAt).isNotNull()
    assertThat(result.won).isTrue()
  }

  @Test
  fun attemptsJson_persistsCorrectly() = runTest {
    val attempts = """["carro","campo","gatos","mundo","plano"]"""
    val session = createTestGameSessionEntity(puzzleId = 42, attempts = attempts)
    dao.insert(session)

    val result = dao.getByPuzzleId(42)

    assertThat(result!!.attempts).isEqualTo(attempts)
  }
}

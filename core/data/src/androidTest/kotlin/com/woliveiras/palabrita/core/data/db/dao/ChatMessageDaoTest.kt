package com.woliveiras.palabrita.core.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.data.db.PalabritaDatabase
import com.woliveiras.palabrita.core.data.db.createTestChatMessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatMessageDaoTest {

  private lateinit var database: PalabritaDatabase
  private lateinit var dao: ChatMessageDao

  @Before
  fun setup() {
    database =
      Room.inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext(),
          PalabritaDatabase::class.java,
        )
        .allowMainThreadQueries()
        .build()
    dao = database.chatMessageDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun insert_storesMessage() = runTest {
    val message = createTestChatMessageEntity(puzzleId = 1, role = "user", content = "Olá!")

    dao.insert(message)

    val messages = dao.getByPuzzleId(1)
    assertThat(messages).hasSize(1)
    assertThat(messages[0].content).isEqualTo("Olá!")
  }

  @Test
  fun getByPuzzleId_returnsMessagesOrderedByTimestamp() = runTest {
    dao.insert(createTestChatMessageEntity(puzzleId = 1, role = "user", content = "Primeiro", timestamp = 100))
    dao.insert(createTestChatMessageEntity(puzzleId = 1, role = "model", content = "Segundo", timestamp = 200))
    dao.insert(createTestChatMessageEntity(puzzleId = 1, role = "user", content = "Terceiro", timestamp = 300))

    val messages = dao.getByPuzzleId(1)

    assertThat(messages).hasSize(3)
    assertThat(messages[0].content).isEqualTo("Primeiro")
    assertThat(messages[1].content).isEqualTo("Segundo")
    assertThat(messages[2].content).isEqualTo("Terceiro")
  }

  @Test
  fun getByPuzzleId_returnsEmptyForNonExistentPuzzle() = runTest {
    val messages = dao.getByPuzzleId(999)

    assertThat(messages).isEmpty()
  }

  @Test
  fun getByPuzzleId_doesNotReturnMessagesFromOtherPuzzles() = runTest {
    dao.insert(createTestChatMessageEntity(puzzleId = 1, content = "Puzzle 1"))
    dao.insert(createTestChatMessageEntity(puzzleId = 2, content = "Puzzle 2"))

    val messages = dao.getByPuzzleId(1)

    assertThat(messages).hasSize(1)
    assertThat(messages[0].content).isEqualTo("Puzzle 1")
  }

  @Test
  fun countUserMessages_countsOnlyUserRole() = runTest {
    dao.insert(createTestChatMessageEntity(puzzleId = 1, role = "user", timestamp = 100))
    dao.insert(createTestChatMessageEntity(puzzleId = 1, role = "model", timestamp = 200))
    dao.insert(createTestChatMessageEntity(puzzleId = 1, role = "user", timestamp = 300))

    assertThat(dao.countUserMessages(1)).isEqualTo(2)
  }

  @Test
  fun countUserMessages_returnsZeroForNonExistentPuzzle() = runTest {
    assertThat(dao.countUserMessages(999)).isEqualTo(0)
  }
}

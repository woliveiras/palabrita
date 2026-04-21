package com.woliveiras.palabrita.core.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.data.db.PalabritaDatabase
import com.woliveiras.palabrita.core.data.db.createTestPuzzleEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PuzzleDaoTest {

  private lateinit var database: PalabritaDatabase
  private lateinit var dao: PuzzleDao

  @Before
  fun setup() {
    database =
      Room.inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext(),
          PalabritaDatabase::class.java,
        )
        .allowMainThreadQueries()
        .build()
    dao = database.puzzleDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun getNextUnplayed_returnsPuzzleMatchingLanguageAndDifficulty() = runTest {
    dao.insert(createTestPuzzleEntity(word = "gatos", language = "pt", difficulty = 2))

    val result = dao.getNextUnplayed("pt", 2)

    assertThat(result).isNotNull()
    assertThat(result!!.word).isEqualTo("gatos")
  }

  @Test
  fun getNextUnplayed_returnsNullWhenAllPlayed() = runTest {
    dao.insert(createTestPuzzleEntity(word = "gatos", isPlayed = true))

    val result = dao.getNextUnplayed("pt", 2)

    assertThat(result).isNull()
  }

  @Test
  fun getNextUnplayed_filtersByLanguage() = runTest {
    dao.insert(createTestPuzzleEntity(word = "gatos", language = "pt", generatedAt = 100))
    dao.insert(createTestPuzzleEntity(word = "cats", language = "en", generatedAt = 200))

    val result = dao.getNextUnplayed("en", 2)

    assertThat(result!!.word).isEqualTo("cats")
  }

  @Test
  fun getNextUnplayed_filtersByDifficulty() = runTest {
    dao.insert(createTestPuzzleEntity(word = "gatos", difficulty = 2, generatedAt = 100))
    dao.insert(createTestPuzzleEntity(word = "hipopotamo", difficulty = 5, generatedAt = 200))

    val result = dao.getNextUnplayed("pt", 5)

    assertThat(result!!.word).isEqualTo("hipopotamo")
  }

  @Test
  fun getNextUnplayed_returnsOldestFirst() = runTest {
    dao.insert(createTestPuzzleEntity(word = "campo", generatedAt = 200))
    dao.insert(createTestPuzzleEntity(word = "gatos", generatedAt = 100))

    val result = dao.getNextUnplayed("pt", 2)

    assertThat(result!!.word).isEqualTo("gatos")
  }

  @Test
  fun countUnplayed_returnsCorrectCount() = runTest {
    dao.insert(createTestPuzzleEntity(word = "gatos", language = "pt", difficulty = 2))
    dao.insert(createTestPuzzleEntity(word = "campo", language = "pt", difficulty = 2))
    dao.insert(
      createTestPuzzleEntity(word = "carro", language = "pt", difficulty = 2, isPlayed = true)
    )

    assertThat(dao.countUnplayed("pt", 2)).isEqualTo(2)
  }

  @Test
  fun countUnplayed_filtersByLanguageAndDifficulty() = runTest {
    dao.insert(createTestPuzzleEntity(word = "gatos", language = "pt", difficulty = 2))
    dao.insert(createTestPuzzleEntity(word = "cats", language = "en", difficulty = 2))
    dao.insert(createTestPuzzleEntity(word = "mundo", language = "pt", difficulty = 3))

    assertThat(dao.countUnplayed("pt", 2)).isEqualTo(1)
  }

  @Test
  fun getAllWords_returnsAllWordsRegardlessOfPlayState() = runTest {
    dao.insert(createTestPuzzleEntity(word = "gatos"))
    dao.insert(createTestPuzzleEntity(word = "campo", isPlayed = true))

    val words = dao.getAllWords()

    assertThat(words).containsExactly("gatos", "campo")
  }

  @Test
  fun getRecentWords_returnsMostRecentInOrder() = runTest {
    dao.insert(createTestPuzzleEntity(word = "alfa", generatedAt = 100))
    dao.insert(createTestPuzzleEntity(word = "beta", generatedAt = 200))
    dao.insert(createTestPuzzleEntity(word = "gama", generatedAt = 300))

    val recent = dao.getRecentWords(2)

    assertThat(recent).containsExactly("gama", "beta").inOrder()
  }

  @Test
  fun insert_withDuplicateWord_isIgnored() = runTest {
    val firstId = dao.insert(createTestPuzzleEntity(word = "gatos", category = "animal"))
    val duplicateId = dao.insert(createTestPuzzleEntity(word = "gatos", category = "bicho"))

    assertThat(firstId).isGreaterThan(0)
    assertThat(duplicateId).isEqualTo(-1)
    assertThat(dao.getAllWords()).hasSize(1)
  }

  @Test
  fun markAsPlayed_updatesPuzzleCorrectly() = runTest {
    val id = dao.insert(createTestPuzzleEntity(word = "gatos"))
    val playedAt = System.currentTimeMillis()

    dao.markAsPlayed(id, playedAt)

    val result = dao.getNextUnplayed("pt", 2)
    assertThat(result).isNull()
  }
}

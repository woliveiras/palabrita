package com.woliveiras.palabrita.core.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.data.db.PalabritaDatabase
import com.woliveiras.palabrita.core.data.db.createTestPlayerStatsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerStatsDaoTest {

  private lateinit var database: PalabritaDatabase
  private lateinit var dao: PlayerStatsDao

  @Before
  fun setup() {
    database =
      Room.inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext(),
          PalabritaDatabase::class.java,
        )
        .allowMainThreadQueries()
        .build()
    dao = database.playerStatsDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun get_returnsNullWhenEmpty() = runTest {
    assertThat(dao.get()).isNull()
  }

  @Test
  fun upsert_createsNewStats() = runTest {
    val stats = createTestPlayerStatsEntity(totalPlayed = 10, totalWon = 7)

    dao.upsert(stats)

    val result = dao.get()
    assertThat(result).isNotNull()
    assertThat(result!!.totalPlayed).isEqualTo(10)
    assertThat(result.totalWon).isEqualTo(7)
  }

  @Test
  fun upsert_updatesExistingStats() = runTest {
    dao.upsert(createTestPlayerStatsEntity(totalPlayed = 5))

    dao.upsert(createTestPlayerStatsEntity(totalPlayed = 10, currentStreak = 3))

    val result = dao.get()
    assertThat(result!!.totalPlayed).isEqualTo(10)
    assertThat(result.currentStreak).isEqualTo(3)
  }

  @Test
  fun observe_emitsStatsAfterUpsert() = runTest {
    val stats = createTestPlayerStatsEntity(totalXp = 150, playerTier = "ASTUTO")

    dao.upsert(stats)

    val result = dao.observe().first()
    assertThat(result).isNotNull()
    assertThat(result!!.totalXp).isEqualTo(150)
    assertThat(result.playerTier).isEqualTo("ASTUTO")
  }

  @Test
  fun upsert_preservesJsonFields() = runTest {
    val distribution = """{"1":5,"2":12,"3":18,"4":8,"5":3,"6":1}"""
    val winsPerDiff = """{"1":45,"2":30,"3":12}"""

    dao.upsert(
      createTestPlayerStatsEntity(guessDistribution = distribution, gamesWonByDifficulty = winsPerDiff)
    )

    val result = dao.get()
    assertThat(result!!.guessDistribution).isEqualTo(distribution)
    assertThat(result.gamesWonByDifficulty).isEqualTo(winsPerDiff)
  }
}

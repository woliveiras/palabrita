package com.woliveiras.palabrita.core.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.data.db.PalabritaDatabase
import com.woliveiras.palabrita.core.data.db.createTestModelConfigEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModelConfigDaoTest {

  private lateinit var database: PalabritaDatabase
  private lateinit var dao: ModelConfigDao

  @Before
  fun setup() {
    database =
      Room.inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext(),
          PalabritaDatabase::class.java,
        )
        .allowMainThreadQueries()
        .build()
    dao = database.modelConfigDao()
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
  fun upsert_createsNewConfig() = runTest {
    val config =
      createTestModelConfigEntity(
        modelId = "gemma4_e2b",
        downloadState = "DOWNLOADED",
        modelPath = "/data/models/gemma4",
        sizeBytes = 2_600_000_000,
      )

    dao.upsert(config)

    val result = dao.get()
    assertThat(result).isNotNull()
    assertThat(result!!.modelId).isEqualTo("gemma4_e2b")
    assertThat(result.downloadState).isEqualTo("DOWNLOADED")
    assertThat(result.modelPath).isEqualTo("/data/models/gemma4")
    assertThat(result.sizeBytes).isEqualTo(2_600_000_000)
  }

  @Test
  fun upsert_updatesDownloadState() = runTest {
    dao.upsert(
      createTestModelConfigEntity(modelId = "qwen3_0_6b", downloadState = "NOT_DOWNLOADED")
    )

    dao.upsert(
      createTestModelConfigEntity(
        modelId = "qwen3_0_6b",
        downloadState = "DOWNLOADED",
        modelPath = "/data/models/qwen3",
        sizeBytes = 529_000_000,
      )
    )

    val result = dao.get()
    assertThat(result!!.downloadState).isEqualTo("DOWNLOADED")
    assertThat(result.modelPath).isEqualTo("/data/models/qwen3")
  }

  @Test
  fun observe_emitsConfigAfterUpsert() = runTest {
    dao.upsert(
      createTestModelConfigEntity(modelId = "gemma4_e2b", downloadState = "DOWNLOADING")
    )

    val result = dao.observe().first()

    assertThat(result).isNotNull()
    assertThat(result!!.modelId).isEqualTo("gemma4_e2b")
    assertThat(result.downloadState).isEqualTo("DOWNLOADING")
  }

  @Test
  fun upsert_persistsDownloadStateBetweenReads() = runTest {
    dao.upsert(
      createTestModelConfigEntity(modelId = "qwen3_0_6b", downloadState = "DOWNLOADING")
    )

    val first = dao.get()
    assertThat(first!!.downloadState).isEqualTo("DOWNLOADING")

    dao.upsert(first.copy(downloadState = "DOWNLOADED"))

    val second = dao.get()
    assertThat(second!!.downloadState).isEqualTo("DOWNLOADED")
  }
}

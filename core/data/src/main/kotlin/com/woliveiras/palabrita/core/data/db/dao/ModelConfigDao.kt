package com.woliveiras.palabrita.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.woliveiras.palabrita.core.data.db.entity.ModelConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelConfigDao {

  @Query("SELECT * FROM model_config WHERE id = 1") suspend fun get(): ModelConfigEntity?

  @Query("SELECT * FROM model_config WHERE id = 1") fun observe(): Flow<ModelConfigEntity?>

  @Upsert suspend fun upsert(config: ModelConfigEntity)
}

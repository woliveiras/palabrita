package com.woliveiras.palabrita.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.woliveiras.palabrita.core.data.db.entity.PlayerStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStatsDao {

    @Query("SELECT * FROM player_stats WHERE id = 1")
    suspend fun get(): PlayerStatsEntity?

    @Query("SELECT * FROM player_stats WHERE id = 1")
    fun observe(): Flow<PlayerStatsEntity?>

    @Upsert suspend fun upsert(stats: PlayerStatsEntity)
}

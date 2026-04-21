package com.woliveiras.palabrita.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.woliveiras.palabrita.core.data.db.dao.ChatMessageDao
import com.woliveiras.palabrita.core.data.db.dao.GameSessionDao
import com.woliveiras.palabrita.core.data.db.dao.ModelConfigDao
import com.woliveiras.palabrita.core.data.db.dao.PlayerStatsDao
import com.woliveiras.palabrita.core.data.db.dao.PuzzleDao
import com.woliveiras.palabrita.core.data.db.entity.ChatMessageEntity
import com.woliveiras.palabrita.core.data.db.entity.GameSessionEntity
import com.woliveiras.palabrita.core.data.db.entity.ModelConfigEntity
import com.woliveiras.palabrita.core.data.db.entity.PlayerStatsEntity
import com.woliveiras.palabrita.core.data.db.entity.PuzzleEntity

@Database(
  entities =
    [
      PuzzleEntity::class,
      PlayerStatsEntity::class,
      GameSessionEntity::class,
      ChatMessageEntity::class,
      ModelConfigEntity::class,
    ],
  version = 2,
  exportSchema = true,
)
abstract class PalabritaDatabase : RoomDatabase() {
  abstract fun puzzleDao(): PuzzleDao

  abstract fun playerStatsDao(): PlayerStatsDao

  abstract fun gameSessionDao(): GameSessionDao

  abstract fun chatMessageDao(): ChatMessageDao

  abstract fun modelConfigDao(): ModelConfigDao

  companion object {
    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE game_sessions ADD COLUMN dailyChallengeIndex INTEGER")
        db.execSQL("ALTER TABLE game_sessions ADD COLUMN dailyChallengeDate TEXT")
        db.execSQL("ALTER TABLE game_sessions ADD COLUMN chatExplored INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE player_stats ADD COLUMN lastDailyDate TEXT")
      }
    }
  }
}

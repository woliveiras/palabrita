package com.woliveiras.palabrita.core.data.di

import android.content.Context
import androidx.room.Room
import com.woliveiras.palabrita.core.data.db.PalabritaDatabase
import com.woliveiras.palabrita.core.data.db.dao.GameSessionDao
import com.woliveiras.palabrita.core.data.db.dao.ModelConfigDao
import com.woliveiras.palabrita.core.data.db.dao.PlayerStatsDao
import com.woliveiras.palabrita.core.data.db.dao.PuzzleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): PalabritaDatabase =
    Room.databaseBuilder(context, PalabritaDatabase::class.java, "palabrita.db")
      .fallbackToDestructiveMigration(true)
      .build()

  @Provides fun providePuzzleDao(db: PalabritaDatabase): PuzzleDao = db.puzzleDao()

  @Provides fun providePlayerStatsDao(db: PalabritaDatabase): PlayerStatsDao = db.playerStatsDao()

  @Provides fun provideGameSessionDao(db: PalabritaDatabase): GameSessionDao = db.gameSessionDao()

  @Provides fun provideModelConfigDao(db: PalabritaDatabase): ModelConfigDao = db.modelConfigDao()
}

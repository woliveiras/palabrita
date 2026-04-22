package com.woliveiras.palabrita.core.data.di

import com.woliveiras.palabrita.core.data.preferences.AppPreferences
import com.woliveiras.palabrita.core.data.preferences.AppPreferencesImpl
import com.woliveiras.palabrita.core.data.repository.ChatRepositoryImpl
import com.woliveiras.palabrita.core.data.repository.GameSessionRepositoryImpl
import com.woliveiras.palabrita.core.data.repository.ModelRepositoryImpl
import com.woliveiras.palabrita.core.data.repository.PuzzleRepositoryImpl
import com.woliveiras.palabrita.core.data.repository.StatsRepositoryImpl
import com.woliveiras.palabrita.core.model.repository.ChatRepository
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

  @Binds @Singleton abstract fun bindPuzzleRepository(impl: PuzzleRepositoryImpl): PuzzleRepository

  @Binds @Singleton abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

  @Binds @Singleton abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository

  @Binds @Singleton abstract fun bindGameSessionRepository(impl: GameSessionRepositoryImpl): GameSessionRepository

  @Binds @Singleton abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

  @Binds @Singleton abstract fun bindAppPreferences(impl: AppPreferencesImpl): AppPreferences
}

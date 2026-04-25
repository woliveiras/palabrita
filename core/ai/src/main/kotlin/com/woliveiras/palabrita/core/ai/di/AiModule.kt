package com.woliveiras.palabrita.core.ai.di

import com.woliveiras.palabrita.core.ai.AiModelRegistry
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import com.woliveiras.palabrita.core.ai.LlmEngineManagerImpl
import com.woliveiras.palabrita.core.ai.LlmResponseParser
import com.woliveiras.palabrita.core.ai.LlmResponseParserImpl
import com.woliveiras.palabrita.core.ai.ModelDownloadManager
import com.woliveiras.palabrita.core.ai.ModelDownloadManagerImpl
import com.woliveiras.palabrita.core.ai.ModelRegistry
import com.woliveiras.palabrita.core.ai.PuzzleGenerator
import com.woliveiras.palabrita.core.ai.PuzzleGeneratorImpl
import com.woliveiras.palabrita.core.ai.PuzzleValidator
import com.woliveiras.palabrita.core.ai.PuzzleValidatorImpl
import com.woliveiras.palabrita.core.ai.worker.PuzzleGenerationScheduler
import com.woliveiras.palabrita.core.ai.worker.PuzzleGenerationSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

  @Binds @Singleton abstract fun bindLlmEngineManager(impl: LlmEngineManagerImpl): LlmEngineManager

  @Binds
  @Singleton
  abstract fun bindModelDownloadManager(impl: ModelDownloadManagerImpl): ModelDownloadManager

  @Binds @Singleton abstract fun bindPuzzleGenerator(impl: PuzzleGeneratorImpl): PuzzleGenerator

  @Binds abstract fun bindLlmResponseParser(impl: LlmResponseParserImpl): LlmResponseParser

  @Binds abstract fun bindPuzzleValidator(impl: PuzzleValidatorImpl): PuzzleValidator

  @Binds
  @Singleton
  abstract fun bindPuzzleGenerationScheduler(
    impl: PuzzleGenerationSchedulerImpl
  ): PuzzleGenerationScheduler

  companion object {
    @Provides @Singleton fun provideModelRegistry(): ModelRegistry = AiModelRegistry
  }
}

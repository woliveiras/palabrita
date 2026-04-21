package com.woliveiras.palabrita.core.model.repository

import com.woliveiras.palabrita.core.model.ModelConfig
import kotlinx.coroutines.flow.Flow

interface ModelRepository {
  suspend fun getConfig(): ModelConfig

  suspend fun updateConfig(config: ModelConfig)

  fun observeConfig(): Flow<ModelConfig>
}

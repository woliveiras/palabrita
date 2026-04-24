package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeModelRepository(initialConfig: ModelConfig = ModelConfig()) : ModelRepository {
  private var config = initialConfig
  private val _flow = MutableStateFlow(initialConfig)

  override suspend fun getConfig(): ModelConfig = config

  override suspend fun updateConfig(config: ModelConfig) {
    this.config = config
    _flow.value = config
  }

  override fun observeConfig(): Flow<ModelConfig> = _flow
}

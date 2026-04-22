package com.woliveiras.palabrita.core.data.repository

import com.woliveiras.palabrita.core.data.db.dao.ModelConfigDao
import com.woliveiras.palabrita.core.data.mapper.toDomain
import com.woliveiras.palabrita.core.data.mapper.toEntity
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ModelRepositoryImpl @Inject constructor(private val configDao: ModelConfigDao) :
  ModelRepository {

  override suspend fun getConfig(): ModelConfig = configDao.get()?.toDomain() ?: ModelConfig()

  override suspend fun updateConfig(config: ModelConfig) {
    configDao.upsert(config.toEntity())
  }

  override fun observeConfig(): Flow<ModelConfig> =
    configDao.observe().map { it?.toDomain() ?: ModelConfig() }
}

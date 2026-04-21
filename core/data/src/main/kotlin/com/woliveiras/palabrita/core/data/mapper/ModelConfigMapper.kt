package com.woliveiras.palabrita.core.data.mapper

import com.woliveiras.palabrita.core.data.db.entity.ModelConfigEntity
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId

fun ModelConfigEntity.toDomain(): ModelConfig =
  ModelConfig(
    id = id,
    modelId = ModelId.valueOf(modelId.uppercase()),
    downloadState = DownloadState.valueOf(downloadState),
    modelPath = modelPath,
    sizeBytes = sizeBytes,
    selectedAt = selectedAt,
  )

fun ModelConfig.toEntity(): ModelConfigEntity =
  ModelConfigEntity(
    id = id,
    modelId = modelId.name.lowercase(),
    downloadState = downloadState.name,
    modelPath = modelPath,
    sizeBytes = sizeBytes,
    selectedAt = selectedAt,
  )

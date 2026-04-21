package com.woliveiras.palabrita.core.model

data class ModelConfig(
  val id: Int = 1,
  val modelId: ModelId = ModelId.NONE,
  val downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
  val modelPath: String? = null,
  val sizeBytes: Long = 0,
  val selectedAt: Long = 0,
)

enum class ModelId {
  GEMMA4_E2B,
  GEMMA3_1B,
  NONE,
}

enum class DownloadState {
  NOT_DOWNLOADED,
  DOWNLOADING,
  DOWNLOADED,
  FAILED,
}

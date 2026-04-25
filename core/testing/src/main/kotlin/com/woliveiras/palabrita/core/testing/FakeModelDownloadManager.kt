package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.ai.ModelDownloadManager
import com.woliveiras.palabrita.core.ai.ModelDownloadProgress
import com.woliveiras.palabrita.core.model.ModelId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeModelDownloadManager : ModelDownloadManager {
  private val _progress = MutableStateFlow<ModelDownloadProgress>(ModelDownloadProgress.Idle)
  override val progress: StateFlow<ModelDownloadProgress> = _progress

  val modelPaths: MutableMap<ModelId, String> = mutableMapOf()
  var startDownloadCallCount: Int = 0
    private set

  override suspend fun startDownload(modelId: ModelId) {
    startDownloadCallCount++
    _progress.value = ModelDownloadProgress.Completed("/fake/model.litertlm")
  }

  override fun cancelDownload() {
    _progress.value = ModelDownloadProgress.Idle
  }

  override fun getModelPath(modelId: ModelId): String? = modelPaths[modelId]
}

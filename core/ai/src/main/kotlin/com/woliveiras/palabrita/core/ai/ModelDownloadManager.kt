package com.woliveiras.palabrita.core.ai

import android.content.Context
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ModelDownloadProgress {
  data object Idle : ModelDownloadProgress()

  data object Checking : ModelDownloadProgress()

  data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) :
    ModelDownloadProgress()

  data class Completed(val modelPath: String) : ModelDownloadProgress()

  data class Failed(val message: String) : ModelDownloadProgress()
}

interface ModelDownloadManager {
  val progress: StateFlow<ModelDownloadProgress>

  suspend fun startDownload(modelId: ModelId)

  fun cancelDownload()

  fun getModelPath(modelId: ModelId): String?
}

@Singleton
class ModelDownloadManagerImpl
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val modelRepository: ModelRepository,
) : ModelDownloadManager {

  private val _progress = MutableStateFlow<ModelDownloadProgress>(ModelDownloadProgress.Idle)
  override val progress: StateFlow<ModelDownloadProgress> = _progress.asStateFlow()

  private var downloadJob: Job? = null

  private val modelsDir: File
    get() = File(context.filesDir, "models").also { it.mkdirs() }

  override fun getModelPath(modelId: ModelId): String? {
    val info = AiModelRegistry.getInfo(modelId) ?: return null
    val file = File(modelsDir, info.fileName)
    return if (file.exists()) file.absolutePath else null
  }

  override suspend fun startDownload(modelId: ModelId) {
    val info =
      AiModelRegistry.getInfo(modelId)
        ?: run {
          _progress.value = ModelDownloadProgress.Failed("Unknown model: $modelId")
          return
        }

    _progress.value = ModelDownloadProgress.Checking

    val targetFile = File(modelsDir, info.fileName)
    if (targetFile.exists() && targetFile.length() == info.sizeBytes) {
      completeDownload(modelId, targetFile)
      return
    }

    val availableBytes =
      android.os.StatFs(context.filesDir.absolutePath).availableBytes
    if (availableBytes < info.sizeBytes + 100_000_000L) {
      _progress.value =
        ModelDownloadProgress.Failed(
          context.getString(
            com.woliveiras.palabrita.core.common.R.string.error_insufficient_space,
            formatBytes(info.sizeBytes),
            formatBytes(availableBytes),
          )
        )
      return
    }

    coroutineScope {
      downloadJob =
        launch {
          try {
            downloadFile(info.downloadUrl, targetFile)
            completeDownload(modelId, targetFile)
          } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            _progress.value = ModelDownloadProgress.Idle
            throw e
          } catch (e: Exception) {
            _progress.value =
              ModelDownloadProgress.Failed(e.message ?: context.getString(com.woliveiras.palabrita.core.common.R.string.error_download_unknown))
          }
        }
      downloadJob?.join()
    }
  }

  override fun cancelDownload() {
    downloadJob?.cancel()
    downloadJob = null
    _progress.value = ModelDownloadProgress.Idle
  }

  private suspend fun downloadFile(url: String, targetFile: File) {
    withContext(Dispatchers.IO) {
      val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
      val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

      val connection = openConnection(url, existingBytes)
      try {
        val responseCode = connection.responseCode
        val totalBytes: Long
        val startOffset: Long

        if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
          totalBytes = existingBytes + connection.contentLengthLong()
          startOffset = existingBytes
        } else if (responseCode == HttpURLConnection.HTTP_OK) {
          totalBytes = connection.contentLengthLong()
          startOffset = 0L
          if (tempFile.exists()) tempFile.delete()
        } else {
          throw java.io.IOException("HTTP $responseCode: ${connection.responseMessage}")
        }

        val inputStream = connection.inputStream
        val outputStream = FileOutputStream(tempFile, startOffset > 0)
        val buffer = ByteArray(8192)
        var downloadedBytes = startOffset

        inputStream.use { input ->
          outputStream.use { output ->
            while (true) {
              ensureActive()
              val bytesRead = input.read(buffer)
              if (bytesRead == -1) break
              output.write(buffer, 0, bytesRead)
              downloadedBytes += bytesRead
              val progress =
                if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
              _progress.value =
                ModelDownloadProgress.Downloading(progress, downloadedBytes, totalBytes)
            }
          }
        }

        tempFile.renameTo(targetFile)
      } finally {
        connection.disconnect()
      }
    }
  }

  private fun openConnection(url: String, existingBytes: Long): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 30_000
    connection.readTimeout = 30_000
    connection.instanceFollowRedirects = true
    if (existingBytes > 0) {
      connection.setRequestProperty("Range", "bytes=$existingBytes-")
    }
    return connection
  }

  private fun HttpURLConnection.contentLengthLong(): Long {
    val header = getHeaderField("Content-Length") ?: return -1L
    return header.toLongOrNull() ?: -1L
  }

  private suspend fun completeDownload(modelId: ModelId, file: File) {
    val config =
      ModelConfig(
        modelId = modelId,
        downloadState = DownloadState.DOWNLOADED,
        modelPath = file.absolutePath,
        sizeBytes = file.length(),
        selectedAt = System.currentTimeMillis(),
      )
    modelRepository.updateConfig(config)
    _progress.value = ModelDownloadProgress.Completed(file.absolutePath)
  }

  private fun formatBytes(bytes: Long): String {
    val gb = bytes / 1_000_000_000.0
    return if (gb >= 1.0) "%.1f GB".format(gb) else "%.0f MB".format(bytes / 1_000_000.0)
  }
}

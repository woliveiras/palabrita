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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

private class ModelGatedException : Exception()

private const val MAX_REDIRECTS = 10
private const val CONNECT_TIMEOUT_MS = 30_000
private const val READ_TIMEOUT_MS = 30_000
private const val BUFFER_SIZE = 32_768
private const val PROGRESS_EMIT_THRESHOLD = 0.005f

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
  private val attemptId = AtomicInteger(0)
  private val activeConnection = AtomicReference<HttpURLConnection?>(null)

  private val modelsDir: File
    get() = File(context.filesDir, "models").also { it.mkdirs() }

  override fun getModelPath(modelId: ModelId): String? {
    val info = AiModelRegistry.getInfo(modelId) ?: return null
    val file = File(modelsDir, info.fileName)
    return if (file.exists()) file.absolutePath else null
  }

  override suspend fun startDownload(modelId: ModelId) {
    // Cancel any in-flight download before starting a new one
    downloadJob?.let {
      it.cancel()
      it.join()
    }
    activeConnection.getAndSet(null)?.disconnect()

    val currentAttempt = attemptId.incrementAndGet()

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

    val availableBytes = android.os.StatFs(context.filesDir.absolutePath).availableBytes
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

    try {
      downloadFile(info.downloadUrl, targetFile, currentAttempt)
      if (attemptId.get() == currentAttempt) {
        completeDownload(modelId, targetFile)
      }
    } catch (e: kotlin.coroutines.cancellation.CancellationException) {
      if (attemptId.get() == currentAttempt) {
        _progress.value = ModelDownloadProgress.Idle
      }
      throw e
    } catch (_: ModelGatedException) {
      if (attemptId.get() == currentAttempt) {
        _progress.value =
          ModelDownloadProgress.Failed(
            context.getString(com.woliveiras.palabrita.core.common.R.string.error_model_gated)
          )
      }
    } catch (e: Exception) {
      if (attemptId.get() == currentAttempt) {
        _progress.value =
          ModelDownloadProgress.Failed(
            e.message
              ?: context.getString(
                com.woliveiras.palabrita.core.common.R.string.error_download_unknown
              )
          )
      }
    }
  }

  override fun cancelDownload() {
    attemptId.incrementAndGet()
    activeConnection.getAndSet(null)?.disconnect()
    downloadJob?.cancel()
    downloadJob = null
    _progress.value = ModelDownloadProgress.Idle
  }

  private suspend fun downloadFile(url: String, targetFile: File, attempt: Int) {
    withContext(Dispatchers.IO) {
      val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
      val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

      val connection = openConnectionWithRedirects(url, existingBytes)
      activeConnection.set(connection)
      try {
        val responseCode = connection.responseCode
        val totalBytes: Long
        val startOffset: Long

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == 403) {
          throw ModelGatedException()
        } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
          startOffset = parseContentRangeOffset(connection) ?: existingBytes
          totalBytes =
            parseContentRangeTotal(connection) ?: (startOffset + connection.contentLengthLong())
        } else if (responseCode == HttpURLConnection.HTTP_OK) {
          totalBytes = connection.contentLengthLong()
          startOffset = 0L
          if (tempFile.exists()) tempFile.delete()
        } else {
          throw java.io.IOException("HTTP $responseCode: ${connection.responseMessage}")
        }

        val inputStream = connection.inputStream
        val outputStream = FileOutputStream(tempFile, startOffset > 0)
        val buffer = ByteArray(BUFFER_SIZE)
        var downloadedBytes = startOffset
        var lastEmittedProgress = 0f

        inputStream.use { input ->
          outputStream.use { output ->
            while (true) {
              ensureActive()
              if (attemptId.get() != attempt) return@withContext
              val bytesRead = input.read(buffer)
              if (bytesRead == -1) break
              output.write(buffer, 0, bytesRead)
              downloadedBytes += bytesRead

              val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
              if (progress - lastEmittedProgress >= PROGRESS_EMIT_THRESHOLD || progress >= 1f) {
                if (attemptId.get() == attempt) {
                  _progress.value =
                    ModelDownloadProgress.Downloading(progress, downloadedBytes, totalBytes)
                }
                lastEmittedProgress = progress
              }
            }
          }
        }

        val renamed = tempFile.renameTo(targetFile)
        if (!renamed) {
          throw java.io.IOException("Failed to move downloaded file to final location")
        }
      } finally {
        activeConnection.compareAndSet(connection, null)
        connection.disconnect()
      }
    }
  }

  /**
   * Opens a connection following redirects manually so that the Range header is preserved across
   * CDN redirect chains (HttpURLConnection can strip it on auto-redirect).
   */
  private fun openConnectionWithRedirects(url: String, existingBytes: Long): HttpURLConnection {
    var currentUrl = url
    var redirectCount = 0

    while (true) {
      val connection = URL(currentUrl).openConnection() as HttpURLConnection
      connection.connectTimeout = CONNECT_TIMEOUT_MS
      connection.readTimeout = READ_TIMEOUT_MS
      connection.instanceFollowRedirects = false
      if (existingBytes > 0) {
        connection.setRequestProperty("Range", "bytes=$existingBytes-")
      }

      val responseCode = connection.responseCode
      if (responseCode in 301..308 && responseCode != 304) {
        val location = connection.getHeaderField("Location")
        connection.disconnect()

        if (location == null) {
          throw java.io.IOException("Redirect $responseCode without Location header")
        }

        currentUrl = URL(URL(currentUrl), location).toExternalForm()
        redirectCount++
        if (redirectCount > MAX_REDIRECTS) {
          throw java.io.IOException("Too many redirects ($redirectCount)")
        }
      } else {
        return connection
      }
    }
  }

  private fun parseContentRangeOffset(connection: HttpURLConnection): Long? {
    val header = connection.getHeaderField("Content-Range") ?: return null
    val match = Regex("""bytes (\d+)-""").find(header)
    return match?.groupValues?.get(1)?.toLongOrNull()
  }

  private fun parseContentRangeTotal(connection: HttpURLConnection): Long? {
    val header = connection.getHeaderField("Content-Range") ?: return null
    val match = Regex("""/(\d+)""").find(header)
    return match?.groupValues?.get(1)?.toLongOrNull()
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

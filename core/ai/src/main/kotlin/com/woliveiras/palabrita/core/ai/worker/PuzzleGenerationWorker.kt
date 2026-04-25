package com.woliveiras.palabrita.core.ai.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import com.woliveiras.palabrita.core.ai.PuzzleGenerator
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.model.GameRules
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class PuzzleGenerationWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val puzzleRepository: PuzzleRepository,
  private val puzzleGenerator: PuzzleGenerator,
  private val statsRepository: StatsRepository,
  private val engineManager: LlmEngineManager,
  private val appPreferences: AppPreferences,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    setForeground(createForegroundInfo())

    val stats = statsRepository.getStats()
    val language = stats.preferredLanguage

    val unplayedCount = puzzleRepository.countAllUnplayed(language)
    if (unplayedCount >= GameRules.REPLENISHMENT_THRESHOLD) {
      return Result.success(workDataOf(KEY_GENERATED_COUNT to 0))
    }

    if (!engineManager.isReady()) {
      return Result.retry()
    }

    val modelId =
      inputData.getString(KEY_MODEL_ID)?.let {
        try {
          ModelId.valueOf(it)
        } catch (e: Exception) {
          android.util.Log.e("PuzzleGenerationWorker", "Invalid ModelId: $it", e)
          null
        }
      } ?: return Result.failure()

    val existingWords = puzzleRepository.getAllGeneratedWords()
    val recentWords = puzzleRepository.getRecentWords(50)

    val cycle = appPreferences.generationCycle.first()
    val (wordLength, batchSize) = GameRules.levelForCycle(cycle)

    setProgress(workDataOf(KEY_GENERATED_COUNT to 0, KEY_TOTAL_EXPECTED to batchSize))

    var generatedCount = 0
    try {
      val puzzles =
        puzzleGenerator.generateBatch(
          count = batchSize,
          language = language,
          wordLength = wordLength,
          recentWords = recentWords,
          allExistingWords = existingWords,
          modelId = modelId,
        ) { successCount ->
          setProgress(
            workDataOf(KEY_GENERATED_COUNT to successCount, KEY_TOTAL_EXPECTED to batchSize)
          )
        }
      puzzleRepository.savePuzzles(puzzles)
      generatedCount = puzzles.size
      if (puzzles.isNotEmpty()) {
        appPreferences.incrementGenerationCycle()
        showCompletionNotification(puzzles.size)
      }
    } catch (e: Exception) {
      android.util.Log.e("PuzzleGenerationWorker", "Batch generation failed", e)
      // Generation failed entirely — return with zero count
    }

    return Result.success(
      workDataOf(KEY_GENERATED_COUNT to generatedCount, KEY_TOTAL_EXPECTED to batchSize)
    )
  }

  private fun createForegroundInfo(): ForegroundInfo {
    createNotificationChannel()
    val notification =
      NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        .setSmallIcon(CommonR.drawable.ic_notification)
        .setContentTitle(
          applicationContext.getString(CommonR.string.notification_generation_progress_title)
        )
        .setContentText(
          applicationContext.getString(CommonR.string.notification_generation_progress_body)
        )
        .setOngoing(true)
        .setProgress(0, 0, true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
    return ForegroundInfo(
      PROGRESS_NOTIFICATION_ID,
      notification,
      ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
    )
  }

  private fun showCompletionNotification(count: Int) {
    createNotificationChannel()

    if (
      ContextCompat.checkSelfPermission(
        applicationContext,
        Manifest.permission.POST_NOTIFICATIONS,
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    val notification =
      NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        .setSmallIcon(CommonR.drawable.ic_notification)
        .setContentTitle(applicationContext.getString(CommonR.string.notification_generation_title))
        .setContentText(
          applicationContext.getString(CommonR.string.notification_generation_body, count)
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    // Permission is checked above via ContextCompat.checkSelfPermission before reaching this line
    @SuppressLint("MissingPermission")
    NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
  }

  private fun createNotificationChannel() {
    val channel =
      NotificationChannel(
          CHANNEL_ID,
          applicationContext.getString(CommonR.string.notification_channel_name),
          NotificationManager.IMPORTANCE_LOW,
        )
        .apply {
          description =
            applicationContext.getString(CommonR.string.notification_channel_description)
        }
    val manager = applicationContext.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)
  }

  companion object {
    const val WORK_NAME = "puzzle_generation"
    const val KEY_MODEL_ID = "model_id"
    const val KEY_GENERATED_COUNT = "generated_count"
    const val KEY_TOTAL_EXPECTED = "total_expected"
    private const val CHANNEL_ID = "puzzle_generation"
    private const val NOTIFICATION_ID = 1001
    private const val PROGRESS_NOTIFICATION_ID = 1002
  }
}

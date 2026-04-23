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
    if (unplayedCount >= REPLENISHMENT_THRESHOLD) {
      return Result.success()
    }

    if (!engineManager.isReady()) {
      return Result.retry()
    }

    val modelId =
      inputData.getString(KEY_MODEL_ID)?.let {
        try {
          com.woliveiras.palabrita.core.model.ModelId.valueOf(it)
        } catch (_: Exception) {
          null
        }
      } ?: return Result.failure()

    val existingWords = puzzleRepository.getAllGeneratedWords()
    val recentWords = puzzleRepository.getRecentWords(50)

    val cycle = appPreferences.generationCycle.first()
    val (wordLength, batchSize) = levelForCycle(cycle)

    setProgress(workDataOf(KEY_GENERATED_COUNT to 0, KEY_TOTAL_EXPECTED to batchSize))

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
      if (puzzles.isNotEmpty()) {
        appPreferences.incrementGenerationCycle()
        showCompletionNotification(puzzles.size)
      }
    } catch (_: Exception) {
      // Generation failed entirely
    }

    return Result.success()
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
    const val REPLENISHMENT_THRESHOLD = 5
    const val MAX_WORD_LENGTH = 6
    private const val CHANNEL_ID = "puzzle_generation"
    private const val NOTIFICATION_ID = 1001
    private const val PROGRESS_NOTIFICATION_ID = 1002

    // Each level is (wordLength, batchSize). Last entry repeats forever.
    val LEVELS = listOf(4 to 5, 5 to 10, 6 to 10)

    fun levelForCycle(cycle: Int): Pair<Int, Int> = LEVELS[cycle.coerceIn(0, LEVELS.lastIndex)]
  }
}

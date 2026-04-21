package com.woliveiras.palabrita.core.ai.worker

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.woliveiras.palabrita.core.model.ModelId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PuzzleGenerationSchedulerImpl @Inject constructor(
  private val workManager: WorkManager,
) : PuzzleGenerationScheduler {

  override fun scheduleGeneration(modelId: ModelId) {
    if (modelId == ModelId.NONE) return

    val constraints = Constraints.Builder()
      .setRequiresBatteryNotLow(true)
      .build()

    val workRequest = OneTimeWorkRequestBuilder<PuzzleGenerationWorker>()
      .setConstraints(constraints)
      .setInputData(workDataOf(PuzzleGenerationWorker.KEY_MODEL_ID to modelId.name))
      .build()

    workManager.enqueueUniqueWork(
      PuzzleGenerationWorker.WORK_NAME,
      ExistingWorkPolicy.KEEP,
      workRequest,
    )
  }
}

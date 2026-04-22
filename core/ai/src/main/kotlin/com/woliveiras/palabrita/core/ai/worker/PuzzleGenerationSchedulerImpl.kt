package com.woliveiras.palabrita.core.ai.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.woliveiras.palabrita.core.model.ModelId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PuzzleGenerationSchedulerImpl @Inject constructor(private val workManager: WorkManager) :
  PuzzleGenerationScheduler {

  override fun scheduleGeneration(modelId: ModelId) {
    if (modelId == ModelId.NONE) return

    val workRequest =
      OneTimeWorkRequestBuilder<PuzzleGenerationWorker>()
        .setInputData(workDataOf(PuzzleGenerationWorker.KEY_MODEL_ID to modelId.name))
        .build()

    workManager.enqueueUniqueWork(
      PuzzleGenerationWorker.WORK_NAME,
      ExistingWorkPolicy.KEEP,
      workRequest,
    )
  }

  override fun observeGenerationState(): Flow<GenerationWorkState> =
    workManager.getWorkInfosForUniqueWorkFlow(PuzzleGenerationWorker.WORK_NAME).map { infos ->
      val info = infos.firstOrNull()
      when (info?.state) {
        WorkInfo.State.SUCCEEDED -> GenerationWorkState.SUCCEEDED
        WorkInfo.State.FAILED -> GenerationWorkState.FAILED
        WorkInfo.State.RUNNING,
        WorkInfo.State.ENQUEUED -> GenerationWorkState.RUNNING
        else -> GenerationWorkState.IDLE
      }
    }
}

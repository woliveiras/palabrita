package com.woliveiras.palabrita.core.ai.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
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
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .build()

    workManager.enqueueUniqueWork(
      PuzzleGenerationWorker.WORK_NAME,
      ExistingWorkPolicy.KEEP,
      workRequest,
    )
  }

  override fun cancelGeneration() {
    workManager.cancelUniqueWork(PuzzleGenerationWorker.WORK_NAME)
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

  override fun observeGenerationInfo(): Flow<GenerationInfo> =
    workManager.getWorkInfosForUniqueWorkFlow(PuzzleGenerationWorker.WORK_NAME).map { infos ->
      val info = infos.firstOrNull()
      val state =
        when (info?.state) {
          WorkInfo.State.SUCCEEDED -> GenerationWorkState.SUCCEEDED
          WorkInfo.State.FAILED -> GenerationWorkState.FAILED
          WorkInfo.State.RUNNING,
          WorkInfo.State.ENQUEUED -> GenerationWorkState.RUNNING
          else -> GenerationWorkState.IDLE
        }
      val progress =
        if (info?.state == WorkInfo.State.SUCCEEDED) {
          val outputData = info.outputData
          GenerationProgress(
            generatedCount = outputData.getInt(PuzzleGenerationWorker.KEY_GENERATED_COUNT, 0),
            totalExpected = outputData.getInt(PuzzleGenerationWorker.KEY_TOTAL_EXPECTED, 0),
          )
        } else {
          val progressData = info?.progress
          GenerationProgress(
            generatedCount =
              progressData?.getInt(PuzzleGenerationWorker.KEY_GENERATED_COUNT, 0) ?: 0,
            totalExpected = progressData?.getInt(PuzzleGenerationWorker.KEY_TOTAL_EXPECTED, 0) ?: 0,
          )
        }
      GenerationInfo(state = state, progress = progress)
    }
}

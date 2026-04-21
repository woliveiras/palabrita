package com.woliveiras.palabrita.core.ai

import com.woliveiras.palabrita.core.model.ModelId

data class AiModelInfo(
  val modelId: ModelId,
  val displayName: String,
  val fileName: String,
  val downloadUrl: String,
  val sizeBytes: Long,
  val requiredRamMb: Long,
)

object AiModelRegistry {

  val GEMMA4_E2B =
    AiModelInfo(
      modelId = ModelId.GEMMA4_E2B,
      displayName = "Gemma 4 E2B",
      fileName = "gemma-4-E2B-it.litertlm",
      downloadUrl =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
      sizeBytes = 2_583_000_000L,
      requiredRamMb = 8192,
    )

  val GEMMA3_1B =
    AiModelInfo(
      modelId = ModelId.GEMMA3_1B,
      displayName = "Gemma 3 1B",
      fileName = "Gemma3-1B-IT.litertlm",
      downloadUrl =
        "https://huggingface.co/litert-community/Gemma3-1B-IT-litert-lm/resolve/main/Gemma3-1B-IT.litertlm",
      sizeBytes = 1_005_000_000L,
      requiredRamMb = 4096,
    )

  fun getInfo(modelId: ModelId): AiModelInfo? =
    when (modelId) {
      ModelId.GEMMA4_E2B -> GEMMA4_E2B
      ModelId.GEMMA3_1B -> GEMMA3_1B
      ModelId.NONE -> null
    }
}

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

  val QWEN3_0_6B =
    AiModelInfo(
      modelId = ModelId.QWEN3_0_6B,
      displayName = "Qwen3 0.6B",
      fileName = "Qwen3-0.6B.litertlm",
      downloadUrl =
        "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm",
      sizeBytes = 614_000_000L,
      requiredRamMb = 2048,
    )

  fun getInfo(modelId: ModelId): AiModelInfo? =
    when (modelId) {
      ModelId.GEMMA4_E2B -> GEMMA4_E2B
      ModelId.QWEN3_0_6B -> QWEN3_0_6B
      ModelId.NONE -> null
    }
}

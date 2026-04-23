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

  val GEMMA4_E4B =
    AiModelInfo(
      modelId = ModelId.GEMMA4_E4B,
      displayName = "Gemma 4 4B",
      fileName = "gemma-4-E4B-it.litertlm",
      downloadUrl =
        "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
      sizeBytes = 3_650_000_000L,
      requiredRamMb = 12288,
    )

  val GEMMA4_E2B =
    AiModelInfo(
      modelId = ModelId.GEMMA4_E2B,
      displayName = "Gemma 4 2B",
      fileName = "gemma-4-E2B-it.litertlm",
      downloadUrl =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
      sizeBytes = 2_583_000_000L,
      requiredRamMb = 8192,
    )

  val PHI4_MINI =
    AiModelInfo(
      modelId = ModelId.PHI4_MINI,
      displayName = "Phi-4 Mini Instruct",
      fileName = "Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm",
      downloadUrl =
        "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm",
      sizeBytes = 3_910_000_000L,
      requiredRamMb = 8192,
    )

  val DEEPSEEK_R1_1_5B =
    AiModelInfo(
      modelId = ModelId.DEEPSEEK_R1_1_5B,
      displayName = "DeepSeek R1 1.5B",
      fileName = "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
      downloadUrl =
        "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
      sizeBytes = 1_830_000_000L,
      requiredRamMb = 4096,
    )

  val QWEN2_5_1_5B =
    AiModelInfo(
      modelId = ModelId.QWEN2_5_1_5B,
      displayName = "Qwen2.5 1.5B Instruct",
      fileName = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
      downloadUrl =
        "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
      sizeBytes = 1_600_000_000L,
      requiredRamMb = 4096,
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
      ModelId.GEMMA4_E4B -> GEMMA4_E4B
      ModelId.GEMMA4_E2B -> GEMMA4_E2B
      ModelId.PHI4_MINI -> PHI4_MINI
      ModelId.DEEPSEEK_R1_1_5B -> DEEPSEEK_R1_1_5B
      ModelId.QWEN2_5_1_5B -> QWEN2_5_1_5B
      ModelId.QWEN3_0_6B -> QWEN3_0_6B
      ModelId.NONE -> null
    }
}

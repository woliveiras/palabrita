# Device Compatibility — Technical Reference

## Overview

Palabrita runs LLM on-device, which requires capable hardware. This document details the compatibility tiers, RAM requirements, benchmarks per device, and the fallback strategy for less capable devices.

## Device Tiers

| Tier | RAM | Model | Mode | Features |
|---|---|---|---|---|
| **HIGH** | >= 8 GB | Gemma 4 E2B (~2.6GB) | AI Premium | Generation, chat, system prompt, function calling, thinking |
| **MEDIUM** | 4-7 GB | Qwen3 0.6B (~614MB) | AI Compact | Generation, chat (prompt-only) |
| **LOW** | < 4 GB | Qwen3 0.6B (~614MB) | AI Compact | Generation, chat (prompt-only) |

## RAM Detection

```kotlin
class DeviceCapabilities @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getTotalRamBytes(): Long {
        val activityManager = context.getSystemService<ActivityManager>()
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        return memInfo.totalMem
    }

    fun getTotalRamGb(): Float {
        return getTotalRamBytes() / (1024f * 1024f * 1024f)
    }

    fun getDeviceTier(): DeviceTier {
        val ramGb = getTotalRamGb()
        return when {
            ramGb >= 8.0f -> DeviceTier.HIGH
            ramGb >= 4.0f -> DeviceTier.MEDIUM
            else -> DeviceTier.LOW
        }
    }

    fun getRecommendedModel(): ModelId {
        return when (getDeviceTier()) {
            DeviceTier.HIGH -> ModelId.GEMMA4_E2B
            DeviceTier.MEDIUM -> ModelId.QWEN3_0_6B
            DeviceTier.LOW -> ModelId.QWEN3_0_6B
        }
    }

    fun canRunModel(modelId: ModelId): Boolean {
        val tier = getDeviceTier()
        return when (modelId) {
            ModelId.GEMMA4_E2B -> tier == DeviceTier.HIGH
            ModelId.QWEN3_0_6B -> true
            ModelId.NONE -> true
        }
    }
}

enum class DeviceTier { LOW, MEDIUM, HIGH }

enum class ModelId {
    GEMMA4_E2B,
    QWEN3_0_6B,
    NONE
}
```

## Memory Requirements per Model

### Gemma 4 E2B

| Resource | Value |
|---|---|
| File on disk | ~2.6 GB |
| RAM (model loaded, INT4) | ~3.2 GB |
| Total recommended RAM (device) | >= 8 GB |
| Actual available RAM for app (with OS + other apps) | ~4-5 GB |

### Qwen3 0.6B

| Resource | Value |
|---|---|
| File on disk | ~614 MB |
| RAM (model loaded, INT4) | ~1 GB |
| Total recommended RAM (device) | >= 2 GB |
| Actual available RAM for app (with OS + other apps) | ~2-3 GB |

> **Note**: Android OS + system services consume 2-3 GB of RAM. The app requires that the model + app + OS fit within total RAM. This is why requirements are higher than the model size.

## Device Benchmarks

### Gemma 4 E2B

| Device | Year | RAM | Backend | Prefill (tok/s) | Decode (tok/s) | Init Time |
|---|---|---|---|---|---|---|
| Samsung S26 Ultra | 2026 | 12 GB | GPU | 3808 | 52 | ~3s |
| Samsung S26 Ultra | 2026 | 12 GB | CPU | 557 | 47 | ~5s |
| Samsung S24 Ultra | 2024 | 12 GB | GPU | ~2500 | ~45 | ~5s |
| Pixel 8 Pro | 2023 | 12 GB | GPU | ~1500 | ~35 | ~8s |
| Pixel 7 Pro | 2022 | 12 GB | CPU | ~400 | ~30 | ~10s |

### Qwen3 0.6B

| Device | Year | RAM | Backend | Prefill (tok/s) | Decode (tok/s) | Init Time |
|---|---|---|---|---|---|---|
| Samsung S24 Ultra | 2024 | 12 GB | GPU | 2585 | 56 | ~3s |
| Samsung S24 Ultra | 2024 | 12 GB | CPU | 322 | 47 | ~3s |
| Samsung S25 Ultra | 2025 | 12 GB | NPU | 5836 | 85 | ~2s |
| Pixel 7 | 2022 | 8 GB | CPU | ~200 | ~35 | ~5s |
| Mid-range device | --- | 6 GB | CPU | ~150 | ~25 | ~8s |
| Entry-level device | --- | 4 GB | CPU | ~100 | ~15 | ~12s |

### UX Implications

| Operation | Gemma 4 (high-end) | Qwen3 (mid-range) | Qwen3 (low-end) |
|---|---|---|---|
| Generate 1 puzzle (~50 tokens) | ~1-2s | ~1-2s | ~3-5s |
| Generate 7 puzzles | ~10-15s | ~10-15s | ~25-35s |
| Chat: first word | ~1s | ~0.5s | ~2s |
| Chat: full response (~100 tokens) | ~2s | ~2-3s | ~5-7s |
| Engine init (cold start) | ~3-8s | ~3-5s | ~8-15s |

## GPU vs CPU

### When GPU is Available

- Most flagships and mid-range devices since 2021
- Requires OpenCL (Qualcomm Adreno, ARM Mali)
- LiteRT-LM automatically tries GPU if configured with `Backend.GPU()`

### When GPU is NOT Available

- Emulators (unreliable for inference)
- Some budget devices without OpenCL
- When `libOpenCL.so` is not present

### Palabrita's Strategy

```kotlin
fun selectBackend(): Backend {
    return try {
        Backend.GPU()
    } catch (e: Exception) {
        Log.w(TAG, "GPU not available, falling back to CPU", e)
        Backend.CPU()
    }
}
```

Always try GPU first, silent fallback to CPU.

## NPU (Neural Processing Unit)

Some recent devices (Samsung S25+, Pixel 9+) have a dedicated NPU that can significantly accelerate inference (e.g., Gemma 3 1B at 85 tok/s on S25 Ultra with NPU vs 47 tok/s on CPU).

LiteRT-LM supports NPU via `Backend.NPU(nativeLibraryDir)`, but requires vendor native libraries. For V1, do **not** prioritize NPU --- GPU and CPU are sufficient.

## Known Limitations

### Emulators

- **NOT reliable** for testing LLM inference
- Engine may initialize but performance will be poor
- GPU backend does not work on emulators
- Recommendation: ALWAYS test on a physical device

### Memory Under Pressure

If the Android system needs memory, it may kill the app process. Mitigations:

1. Register `ComponentCallbacks2` to listen for `onTrimMemory()`
2. On `TRIM_MEMORY_RUNNING_LOW`: consider releasing the Engine if not actively in use
3. Always save game state in Room (survives process death)

```kotlin
class PalabritaApp : Application(), ComponentCallbacks2 {
    @Inject lateinit var engineManager: LlmEngineManager

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            // Release Engine if not actively generating/chatting
            if (!engineManager.isActivelyInferring()) {
                engineManager.destroy()
            }
        }
    }
}
```

### Thermal Throttling

Continuous inference (batch of 7 puzzles) may heat the device and cause throttling:
- Performance degrades progressively
- Mitigation: pause 1-2s between generations in the batch
- WorkManager with `requiresCharging()` constraint can help (device charging = cooling usually active)

## Reference Devices for Testing

| Tier | Recommended Device | RAM | Year | Notes |
|---|---|---|---|---|
| HIGH | Pixel 8 Pro | 12 GB | 2023 | Tensor G3, good for GPU |
| HIGH | Samsung S24 | 8 GB | 2024 | Snapdragon 8 Gen 3 |
| MEDIUM | Pixel 6a | 6 GB | 2022 | Tensor G1, min spec test |
| MEDIUM | Samsung A54 | 6 GB | 2023 | Exynos, real mid-range |
| LOW | Pixel 4a | 6 GB | 2020 | Snapdragon 730G, borderline |
| LOW | Samsung A14 | 3-4 GB | 2023 | Budget, Light mode |

## Compatibility Matrix

```
Device RAM:
  < 4 GB ---> Light mode (static dataset)
  4-5 GB ---> Gemma 3 1B (works, acceptable performance)
  6-7 GB ---> Gemma 3 1B (works well) or Gemma 4 E2B (risky)
  8+ GB  ---> Gemma 4 E2B (recommended) or Gemma 3 1B (works great)
  12+ GB ---> Gemma 4 E2B (excellent)
```

> The user may FORCE a model above the recommended tier (with a warning). The app does not block, only warns.

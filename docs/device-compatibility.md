# Device Compatibility — Technical Reference

## Overview

Palabrita roda LLM on-device, o que exige hardware capaz. Este documento detalha os tiers de compatibilidade, requisitos de RAM, benchmarks por device, e a estratégia de fallback para dispositivos menos capazes.

## Tiers de Dispositivo

| Tier | RAM | Modelo | Modo | Features |
|---|---|---|---|---|
| **HIGH** | ≥ 8 GB | Gemma 4 E2B (~2.6GB) | AI Premium | Geração, chat, system prompt, function calling, thinking |
| **MEDIUM** | 4-7 GB | Gemma 3 1B (~529MB) | AI Compacto | Geração, chat (prompt-only) |
| **LOW** | < 4 GB | Nenhum | Light | Dataset estático, sem chat (card de curiosidade) |

## Detecção de RAM

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
            DeviceTier.MEDIUM -> ModelId.GEMMA3_1B
            DeviceTier.LOW -> ModelId.NONE
        }
    }

    fun canRunModel(modelId: ModelId): Boolean {
        val tier = getDeviceTier()
        return when (modelId) {
            ModelId.GEMMA4_E2B -> tier == DeviceTier.HIGH
            ModelId.GEMMA3_1B -> tier >= DeviceTier.MEDIUM
            ModelId.NONE -> true
        }
    }
}

enum class DeviceTier { LOW, MEDIUM, HIGH }

enum class ModelId {
    GEMMA4_E2B,
    GEMMA3_1B,
    NONE
}
```

## Requisitos de Memória por Modelo

### Gemma 4 E2B

| Recurso | Valor |
|---|---|
| Arquivo em disco | ~2.6 GB |
| RAM (modelo carregado, INT4) | ~3.2 GB |
| RAM total recomendada (device) | ≥ 8 GB |
| RAM real disponível para app (com OS + outros apps) | ~4-5 GB |

### Gemma 3 1B

| Recurso | Valor |
|---|---|
| Arquivo em disco | ~529 MB |
| RAM (modelo carregado, INT4) | ~1 GB |
| RAM total recomendada (device) | ≥ 4 GB |
| RAM real disponível para app (com OS + outros apps) | ~2-3 GB |

> **Nota**: o Android OS + system services consomem 2-3 GB de RAM. O app precisa que o modelo + app + OS caibam na RAM total. Por isso os requisitos são maiores que o tamanho do modelo.

## Benchmarks por Dispositivo

### Gemma 4 E2B

| Device | Ano | RAM | Backend | Prefill (tok/s) | Decode (tok/s) | Init Time |
|---|---|---|---|---|---|---|
| Samsung S26 Ultra | 2026 | 12 GB | GPU | 3808 | 52 | ~3s |
| Samsung S26 Ultra | 2026 | 12 GB | CPU | 557 | 47 | ~5s |
| Samsung S24 Ultra | 2024 | 12 GB | GPU | ~2500 | ~45 | ~5s |
| Pixel 8 Pro | 2023 | 12 GB | GPU | ~1500 | ~35 | ~8s |
| Pixel 7 Pro | 2022 | 12 GB | CPU | ~400 | ~30 | ~10s |

### Gemma 3 1B

| Device | Ano | RAM | Backend | Prefill (tok/s) | Decode (tok/s) | Init Time |
|---|---|---|---|---|---|---|
| Samsung S24 Ultra | 2024 | 12 GB | GPU | 2585 | 56 | ~3s |
| Samsung S24 Ultra | 2024 | 12 GB | CPU | 322 | 47 | ~3s |
| Samsung S25 Ultra | 2025 | 12 GB | NPU | 5836 | 85 | ~2s |
| Pixel 7 | 2022 | 8 GB | CPU | ~200 | ~35 | ~5s |
| Device médio | — | 6 GB | CPU | ~150 | ~25 | ~8s |
| Device básico | — | 4 GB | CPU | ~100 | ~15 | ~12s |

### Implicações para UX

| Operação | Gemma 4 (high-end) | Gemma 3 (mid-range) | Gemma 3 (low-end 4GB) |
|---|---|---|---|
| Gerar 1 puzzle (~50 tokens) | ~1-2s | ~1-2s | ~3-5s |
| Gerar 7 puzzles | ~10-15s | ~10-15s | ~25-35s |
| Chat: primeira palavra | ~1s | ~0.5s | ~2s |
| Chat: resposta completa (~100 tokens) | ~2s | ~2-3s | ~5-7s |
| Engine init (cold start) | ~3-8s | ~3-5s | ~8-15s |

## GPU vs CPU

### Quando GPU está Disponível

- Maioria dos flagships e mid-range desde 2021
- Requer OpenCL (Qualcomm Adreno, ARM Mali)
- LiteRT-LM tenta GPU automaticamente se configurado com `Backend.GPU()`

### Quando GPU NÃO está Disponível

- Emuladores (não confiáveis para inferência)
- Alguns devices budget sem OpenCL
- Quando `libOpenCL.so` não está presente

### Estratégia do Palabrita

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

Sempre tentar GPU primeiro, fallback silencioso para CPU.

## NPU (Neural Processing Unit)

Alguns devices recentes (Samsung S25+, Pixel 9+) têm NPU dedicada que pode acelerar inferência significativamente (ex: Gemma 3 1B a 85 tok/s no S25 Ultra com NPU vs 47 tok/s na CPU).

LiteRT-LM suporta NPU via `Backend.NPU(nativeLibraryDir)`, mas requer native libraries do vendor. Para V1, **não** priorizar NPU — GPU e CPU são suficientes.

## Limitações Conhecidas

### Emuladores

- **NÃO são confiáveis** para testar inferência LLM
- Engine pode inicializar mas performance será péssima
- GPU backend não funciona em emuladores
- Recomendação: testar SEMPRE em device físico

### Memória Sob Pressão

Se o sistema Android precisar de memória, pode matar o processo do app. Mitigações:

1. Registrar `ComponentCallbacks2` para ouvir `onTrimMemory()`
2. Em `TRIM_MEMORY_RUNNING_LOW`: considerar liberar o Engine se não está em uso ativo
3. Sempre salvar estado do jogo em Room (sobrevive a process death)

```kotlin
class PalabritaApp : Application(), ComponentCallbacks2 {
    @Inject lateinit var engineManager: LlmEngineManager

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            // Liberar Engine se não está gerando/chatando ativamente
            if (!engineManager.isActivelyInferring()) {
                engineManager.destroy()
            }
        }
    }
}
```

### Thermal Throttling

Inferência contínua (batch de 7 puzzles) pode aquecer o device e causar throttling:
- Performance degrada progressivamente
- Mitigação: pausar 1-2s entre gerações no batch
- WorkManager com constraint `requiresCharging()` pode ajudar (device charging = cooling geralmente ativo)

## Devices de Referência para Teste

| Tier | Device Recomendado | RAM | Ano | Notas |
|---|---|---|---|---|
| HIGH | Pixel 8 Pro | 12 GB | 2023 | Tensor G3, bom para GPU |
| HIGH | Samsung S24 | 8 GB | 2024 | Snapdragon 8 Gen 3 |
| MEDIUM | Pixel 6a | 6 GB | 2022 | Tensor G1, teste de min spec |
| MEDIUM | Samsung A54 | 6 GB | 2023 | Exynos, mid-range real |
| LOW | Pixel 4a | 6 GB | 2020 | Snapdragon 730G, borderline |
| LOW | Samsung A14 | 3-4 GB | 2023 | Budget, Light mode |

## Matriz de Compatibilidade

```
RAM do device:
  < 4 GB ──→ Light mode (dataset estático)
  4-5 GB ──→ Gemma 3 1B (funciona, performance aceitável)
  6-7 GB ──→ Gemma 3 1B (funciona bem) ou Gemma 4 E2B (arriscado)
  8+ GB  ──→ Gemma 4 E2B (recomendado) ou Gemma 3 1B (funciona ótimo)
  12+ GB ──→ Gemma 4 E2B (ótimo)
```

> O usuário pode FORÇAR um modelo acima do recomendado (com warning). O app não bloqueia, apenas avisa.

# Model Distribution — Technical Reference

## Overview

Os modelos LLM do Palabrita (~529MB a ~2.6GB) são grandes demais para o APK base (limite 200MB). Este documento detalha as estratégias de distribuição via Google Play e download direto, incluindo configuração de AI packs, device targeting e gestão do ciclo de vida do modelo no dispositivo.

## Estratégias de Distribuição

| Estratégia | Uso | Status |
|---|---|---|
| **Play Asset Delivery (PAD)** | Play Store — produção | GA (estável) |
| **Play for On-device AI (AI packs)** | Play Store — com device targeting | Beta |
| **Download direto (HuggingFace)** | Desenvolvimento / sideload | — |

### Decisão para V1

Usar **Play Asset Delivery (PAD)** com asset packs on-demand como estratégia primária. É GA, battle-tested, e suporta download + resume. Se Play for On-device AI sair de beta antes da publicação, migrar para AI packs pelo device targeting superior.

## Play Asset Delivery — Configuração

### Estrutura do Projeto

```
palabrita/
├── app/
├── gemma4-e2b-pack/          ← Asset pack para Gemma 4 E2B
│   ├── build.gradle.kts
│   └── src/main/assets/
│       └── (modelo baixado durante build ou CI)
├── gemma3-1b-pack/           ← Asset pack para Gemma 3 1B
│   ├── build.gradle.kts
│   └── src/main/assets/
│       └── (modelo baixado durante build ou CI)
└── ...
```

### Asset Pack build.gradle.kts

```kotlin
// gemma4-e2b-pack/build.gradle.kts
plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("gemma4_e2b")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
```

```kotlin
// gemma3-1b-pack/build.gradle.kts
plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("gemma3_1b")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
```

### Referência no app/build.gradle.kts

```kotlin
android {
    assetPacks += listOf(":gemma4-e2b-pack", ":gemma3-1b-pack")
}
```

### settings.gradle.kts

```kotlin
include(":gemma4-e2b-pack")
include(":gemma3-1b-pack")
```

## Download Flow no App

### Usando AssetPackManager

```kotlin
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val assetPackManager = AssetPackManagerFactory.getInstance(context)

    fun downloadModel(packName: String): Flow<DownloadProgress> = callbackFlow {
        val listener = AssetPackStateUpdateListener { state ->
            val packState = state.packStates()[packName] ?: return@AssetPackStateUpdateListener

            when (packState.status()) {
                AssetPackStatus.PENDING,
                AssetPackStatus.DOWNLOADING -> {
                    val progress = packState.bytesDownloaded().toFloat() /
                        packState.totalBytesToDownload().toFloat()
                    trySend(DownloadProgress.Downloading(progress))
                }
                AssetPackStatus.TRANSFERRING -> {
                    trySend(DownloadProgress.Transferring)
                }
                AssetPackStatus.COMPLETED -> {
                    trySend(DownloadProgress.Completed(getModelPath(packName)))
                    close()
                }
                AssetPackStatus.FAILED -> {
                    trySend(DownloadProgress.Failed(packState.errorCode()))
                    close()
                }
                AssetPackStatus.CANCELED -> {
                    trySend(DownloadProgress.Canceled)
                    close()
                }
                AssetPackStatus.WAITING_FOR_WIFI -> {
                    trySend(DownloadProgress.WaitingForWifi)
                }
                AssetPackStatus.REQUIRES_USER_CONFIRMATION -> {
                    // Downloads grandes podem exigir confirmação
                    trySend(DownloadProgress.RequiresConfirmation(
                        packState.totalBytesToDownload()
                    ))
                }
                else -> {}
            }
        }

        assetPackManager.registerListener(listener)
        assetPackManager.fetch(listOf(packName))

        awaitClose { assetPackManager.unregisterListener(listener) }
    }

    fun getModelPath(packName: String): String? {
        val location = assetPackManager.getPackLocation(packName) ?: return null
        return location.assetsPath() + "/model.litertlm"
    }

    fun cancelDownload(packName: String) {
        assetPackManager.cancel(listOf(packName))
    }

    fun deleteModel(packName: String) {
        assetPackManager.removePack(packName)
    }
}

sealed class DownloadProgress {
    data class Downloading(val progress: Float) : DownloadProgress()
    data object Transferring : DownloadProgress()
    data class Completed(val modelPath: String?) : DownloadProgress()
    data class Failed(val errorCode: Int) : DownloadProgress()
    data object Canceled : DownloadProgress()
    data object WaitingForWifi : DownloadProgress()
    data class RequiresConfirmation(val totalBytes: Long) : DownloadProgress()
}
```

### Verificação de Download Prévio

Antes de iniciar download, verificar se já está baixado:

```kotlin
fun isModelDownloaded(packName: String): Boolean {
    return assetPackManager.getPackLocation(packName) != null
}
```

## Download Direto (Dev / Sideload)

Para desenvolvimento sem Play Store, download via OkHttp:

```kotlin
class DirectModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    suspend fun download(url: String, modelId: String): Flow<DownloadProgress> = flow {
        val destDir = File(context.filesDir, "models")
        destDir.mkdirs()
        val destFile = File(destDir, "$modelId.litertlm")

        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()

        val body = response.body ?: throw IOException("Empty response")
        val totalBytes = body.contentLength()

        body.byteStream().use { input ->
            destFile.outputStream().buffered().use { output ->
                var downloaded = 0L
                val buffer = ByteArray(8192)
                var read: Int

                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    emit(DownloadProgress.Downloading(
                        downloaded.toFloat() / totalBytes.toFloat()
                    ))
                }
            }
        }

        emit(DownloadProgress.Completed(destFile.absolutePath))
    }.flowOn(Dispatchers.IO)
}
```

### URLs dos Modelos (HuggingFace)

```
Gemma 4 E2B: https://huggingface.co/litert-community/Gemma4-E2B-it/resolve/main/Gemma4-E2B-it.litertlm
Gemma 3 1B:  https://huggingface.co/litert-community/Gemma3-1B-it/resolve/main/Gemma3-1B-it.litertlm
```

> **Nota**: verificar nomes exatos dos arquivos antes de implementar. Os nomes podem variar.

## Play for On-device AI (Futuro)

Quando sair de beta, migrar para AI packs com device targeting:

### Device Groups (XML)

```xml
<!-- app/src/main/device-targeting/device_groups.xml -->
<config:device-targeting-config
    xmlns:config="http://schemas.android.com/apk/config">

    <config:device-group name="high_end">
        <config:device-selector>
            <config:ram min-bytes="8589934592"/>  <!-- 8 GB -->
        </config:device-selector>
    </config:device-group>

    <config:device-group name="mid_range">
        <config:device-selector>
            <config:ram min-bytes="4294967296"/>  <!-- 4 GB -->
        </config:device-selector>
    </config:device-group>

</config:device-targeting-config>
```

### AI Pack com Device Targeting

```
gemma4-e2b-aipack/
├── build.gradle.kts
└── src/main/ai/
    ├── high_end/          ← Entregue só para devices com ≥8GB RAM
    │   └── model.litertlm
    └── default/           ← Vazio (devices que não se qualificam)
```

Benefício: o Play Store só entrega o modelo para devices compatíveis, economizando bandwidth e evitando frustração do usuário.

## Limites do Google Play

| Componente | Limite |
|---|---|
| Base APK (comprimido) | 200 MB |
| Asset pack individual | 1.5 GB |
| Total install-time (base + asset packs) | 4 GB |
| Total on-demand + fast-follow | 4 GB (standard) / 30 GB (Level Up) |

Gemma 4 E2B (~2.6GB) cabe em um único asset pack on-demand (limite 1.5GB comprimido — modelo provavelmente comprime para menos que isso).

> **Se o modelo comprimido exceder 1.5GB**: dividir em múltiplos asset packs ou usar Play for On-device AI (AI packs têm os mesmos limites).

## Armazenamento no Device

### Localização dos Modelos

| Distribuição | Path |
|---|---|
| Play Asset Delivery | `assetPackManager.getPackLocation(packName).assetsPath()` |
| Download direto | `context.filesDir/models/{modelId}.litertlm` |

### Gestão de Espaço

```kotlin
class StorageChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getAvailableSpace(): Long {
        val stat = StatFs(context.filesDir.path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    fun hasSpaceForModel(modelSizeBytes: Long): Boolean {
        // Margem de 500MB além do tamanho do modelo
        return getAvailableSpace() > modelSizeBytes + 500_000_000
    }

    fun getModelSize(modelPath: String): Long {
        return File(modelPath).length()
    }
}
```

## Ciclo de Vida do Modelo

```
                    ┌──────────────┐
                    │ NOT_DOWNLOADED│
                    └──────┬───────┘
                           │ Usuário seleciona modelo
                           ▼
                    ┌──────────────┐
              ┌─────│ DOWNLOADING  │──────┐
              │     └──────┬───────┘      │
              │            │              │
           Cancel        Done          Failed
              │            │              │
              ▼            ▼              ▼
     ┌────────────┐ ┌────────────┐ ┌──────────┐
     │NOT_DOWNLOADED│ │ DOWNLOADED │ │  FAILED  │
     └────────────┘ └─────┬──────┘ └────┬─────┘
                          │              │
                       Engine init    Retry
                          │              │
                          ▼              ▼
                    ┌──────────┐   DOWNLOADING
                    │  READY   │
                    └──────────┘
```

## Testes

### Teste de Download (PAD)

PAD suporta testes locais via `bundletool`:

```bash
# Build AAB
./gradlew bundleRelease

# Gerar APKs com asset packs
bundletool build-apks --bundle=app.aab --output=app.apks --local-testing

# Instalar
bundletool install-apks --apks=app.apks
```

Com `--local-testing`, asset packs on-demand ficam disponíveis localmente e não precisam de Play Store.

### Internal App Sharing

Para testar download real via Play Store sem publicar:
1. Upload AAB para Internal App Sharing no Play Console
2. Gerar link de teste
3. Instalar via link no device de teste
4. Asset packs on-demand baixam via Play Store real

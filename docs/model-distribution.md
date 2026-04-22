# Model Distribution — Technical Reference

## Overview

Palabrita's LLM models (~529 MB to ~2.6 GB) are too large for the base APK (200 MB limit). This document details distribution strategies via Google Play and direct download, including AI pack configuration, device targeting, and model lifecycle management on the device.

## Distribution Strategies

| Strategy | Use | Status |
|---|---|---|
| **Play Asset Delivery (PAD)** | Play Store --- production | GA (stable) |
| **Play for On-device AI (AI packs)** | Play Store --- with device targeting | Beta |
| **Direct download (HuggingFace)** | Development / sideload | --- |

### V1 Decision

Use **Play Asset Delivery (PAD)** with on-demand asset packs as the primary strategy. It is GA, battle-tested, and supports download + resume. If Play for On-device AI exits beta before release, migrate to AI packs for superior device targeting.

## Play Asset Delivery --- Configuration

### Project Structure

```
palabrita/
+-- app/
+-- gemma4-e2b-pack/          <- Asset pack for Gemma 4 E2B
|   +-- build.gradle.kts
|   `-- src/main/assets/
|       `-- (model downloaded during build or CI)
+-- qwen3-06b-pack/             <- Asset pack for Qwen3 0.6B
|   +-- build.gradle.kts
|   `-- src/main/assets/
|       `-- (model downloaded during build or CI)
`-- ...
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
// qwen3-06b-pack/build.gradle.kts
plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("qwen3_0_6b")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
```

### Reference in app/build.gradle.kts

```kotlin
android {
    assetPacks += listOf(":gemma4-e2b-pack", ":qwen3-06b-pack")
}
```

### settings.gradle.kts

```kotlin
include(":gemma4-e2b-pack")
include(":qwen3-06b-pack")
```

## Download Flow in the App

### Using AssetPackManager

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
                    // Large downloads may require confirmation
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

### Previous Download Check

Before starting a download, check if already downloaded:

```kotlin
fun isModelDownloaded(packName: String): Boolean {
    return assetPackManager.getPackLocation(packName) != null
}
```

## Direct Download (Dev / Sideload)

For development without Play Store, download via OkHttp:

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

### Model URLs (HuggingFace)

```
Gemma 4 E2B: https://huggingface.co/litert-community/Gemma4-E2B-it/resolve/main/Gemma4-E2B-it.litertlm
Gemma 3 1B:  https://huggingface.co/litert-community/Gemma3-1B-it/resolve/main/Gemma3-1B-it.litertlm
```

> **Note**: verify exact file names before implementing. Names may vary.

## Play for On-device AI (Future)

When it exits beta, migrate to AI packs with device targeting:

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

### AI Pack with Device Targeting

```
gemma4-e2b-aipack/
+-- build.gradle.kts
`-- src/main/ai/
    +-- high_end/          <- Delivered only to devices with >=8 GB RAM
    |   `-- model.litertlm
    `-- default/           <- Empty (devices that do not qualify)
```

Benefit: Play Store only delivers the model to compatible devices, saving bandwidth and avoiding user frustration.

## Google Play Limits

| Component | Limit |
|---|---|
| Base APK (compressed) | 200 MB |
| Individual asset pack | 1.5 GB |
| Total install-time (base + asset packs) | 4 GB |
| Total on-demand + fast-follow | 4 GB (standard) / 30 GB (Level Up) |

Gemma 4 E2B (~2.6 GB) fits in a single on-demand asset pack (1.5 GB compressed limit --- the model likely compresses to less than that).

> **If the compressed model exceeds 1.5 GB**: split into multiple asset packs or use Play for On-device AI (AI packs have the same limits).

## On-Device Storage

### Model Location

| Distribution | Path |
|---|---|
| Play Asset Delivery | `assetPackManager.getPackLocation(packName).assetsPath()` |
| Direct download | `context.filesDir/models/{modelId}.litertlm` |

### Space Management

```kotlin
class StorageChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getAvailableSpace(): Long {
        val stat = StatFs(context.filesDir.path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    fun hasSpaceForModel(modelSizeBytes: Long): Boolean {
        // 500 MB margin beyond the model size
        return getAvailableSpace() > modelSizeBytes + 500_000_000
    }

    fun getModelSize(modelPath: String): Long {
        return File(modelPath).length()
    }
}
```

## Model Lifecycle

```
                    +--------------+
                    | NOT_DOWNLOADED|
                    +------+-------+
                           |  User selects model
                           v
                    +--------------+
              +-----| DOWNLOADING  |------+
              |     +------+-------+      |
              |            |              |
           Cancel        Done          Failed
              |            |              |
              v            v              v
     +------------+ +------------+ +----------+
     |NOT_DOWNLOADED| | DOWNLOADED | |  FAILED  |
     +------------+ +-----+------+ +----+-----+
                          |              |
                       Engine init    Retry
                          |              |
                          v              v
                    +----------+   DOWNLOADING
                    |  READY   |
                    +----------+
```

## Testing

### Download Test (PAD)

PAD supports local testing via `bundletool`:

```bash
# Build AAB
./gradlew bundleRelease

# Generate APKs with asset packs
bundletool build-apks --bundle=app.aab --output=app.apks --local-testing

# Install
bundletool install-apks --apks=app.apks
```

With `--local-testing`, on-demand asset packs become available locally and do not require Play Store.

### Internal App Sharing

To test real downloads via Play Store without publishing:
1. Upload AAB to Internal App Sharing in Play Console
2. Generate a test link
3. Install via link on the test device
4. On-demand asset packs download via the real Play Store

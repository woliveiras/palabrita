package com.woliveiras.palabrita.core.ai

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class DatasetRegistry @Inject constructor() {

  @Serializable
  data class DatasetInfo(
    val code: String,
    val displayName: String,
    val flag: String,
    val promptName: String,
  )

  private val entries: List<DatasetInfo> by lazy { loadManifest() }

  fun availableLanguages(): List<DatasetInfo> = entries

  fun findByCode(code: String): DatasetInfo? = entries.firstOrNull { it.code == code }

  fun promptName(code: String): String = findByCode(code)?.promptName ?: code

  private fun loadManifest(): List<DatasetInfo> =
    try {
      val stream =
        DatasetRegistry::class.java.getResourceAsStream("/wordlists/manifest.json")
          ?: return emptyList()
      val json = stream.bufferedReader().use { it.readText() }
      val parsed = Json.decodeFromString<List<DatasetInfo>>(json)
      parsed.filter { entry -> wordlistExists(entry.code) }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to parse manifest.json", e)
      emptyList()
    }

  private fun wordlistExists(code: String): Boolean =
    DatasetRegistry::class.java.getResourceAsStream("/wordlists/$code.json") != null

  private companion object {
    const val TAG = "DatasetRegistry"
  }
}

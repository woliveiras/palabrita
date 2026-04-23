package com.woliveiras.palabrita.core.data.mapper

import com.woliveiras.palabrita.core.data.db.entity.PlayerStatsEntity
import com.woliveiras.palabrita.core.model.PlayerStats
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }

fun PlayerStatsEntity.toDomain(): PlayerStats =
  PlayerStats(
    id = id,
    totalPlayed = totalPlayed,
    totalWon = totalWon,
    avgAttempts = avgAttempts,
    preferredLanguage = preferredLanguage,
    guessDistribution = parseIntMap(guessDistribution),
    lastPlayedAt = lastPlayedAt,
  )

fun PlayerStats.toEntity(): PlayerStatsEntity =
  PlayerStatsEntity(
    id = id,
    totalPlayed = totalPlayed,
    totalWon = totalWon,
    avgAttempts = avgAttempts,
    preferredLanguage = preferredLanguage,
    guessDistribution = encodeIntMap(guessDistribution),
    lastPlayedAt = lastPlayedAt,
  )

internal fun parseIntMap(jsonStr: String): Map<Int, Int> {
  if (jsonStr.isBlank() || jsonStr == "{}") return emptyMap()
  return json
    .parseToJsonElement(jsonStr)
    .jsonObject
    .mapKeys { it.key.toInt() }
    .mapValues { it.value.jsonPrimitive.int }
}

internal fun encodeIntMap(map: Map<Int, Int>): String {
  if (map.isEmpty()) return "{}"
  val obj = map.mapKeys { it.key.toString() }.mapValues { JsonPrimitive(it.value) }
  return json.encodeToString(
    kotlinx.serialization.json.JsonObject.serializer(),
    kotlinx.serialization.json.JsonObject(obj),
  )
}

package com.woliveiras.palabrita.core.data.mapper

import com.woliveiras.palabrita.core.data.db.entity.GameSessionEntity
import com.woliveiras.palabrita.core.model.GameSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun GameSessionEntity.toDomain(): GameSession =
  GameSession(
    id = id,
    puzzleId = puzzleId,
    attempts = json.decodeFromString<List<String>>(attempts),
    startedAt = startedAt,
    completedAt = completedAt,
    hintsUsed = hintsUsed,
    won = won,
    dailyChallengeIndex = dailyChallengeIndex,
    dailyChallengeDate = dailyChallengeDate,
    chatExplored = chatExplored,
  )

fun GameSession.toEntity(): GameSessionEntity =
  GameSessionEntity(
    id = id,
    puzzleId = puzzleId,
    attempts = json.encodeToString(attempts),
    startedAt = startedAt,
    completedAt = completedAt,
    hintsUsed = hintsUsed,
    won = won,
    dailyChallengeIndex = dailyChallengeIndex,
    dailyChallengeDate = dailyChallengeDate,
    chatExplored = chatExplored,
  )

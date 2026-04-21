package com.woliveiras.palabrita.core.data.mapper

import com.woliveiras.palabrita.core.data.db.entity.PuzzleEntity
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.PuzzleSource
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun PuzzleEntity.toDomain(): Puzzle =
  Puzzle(
    id = id,
    word = word,
    wordDisplay = wordDisplay,
    language = language,
    difficulty = difficulty,
    category = category,
    hints = json.decodeFromString<List<String>>(hints),
    source = PuzzleSource.valueOf(source),
    generatedAt = generatedAt,
    playedAt = playedAt,
    isPlayed = isPlayed,
    isValid = isValid,
  )

fun Puzzle.toEntity(): PuzzleEntity =
  PuzzleEntity(
    id = id,
    word = word,
    wordDisplay = wordDisplay,
    language = language,
    difficulty = difficulty,
    category = category,
    hints = json.encodeToString(hints),
    source = source.name,
    generatedAt = generatedAt,
    playedAt = playedAt,
    isPlayed = isPlayed,
    isValid = isValid,
  )

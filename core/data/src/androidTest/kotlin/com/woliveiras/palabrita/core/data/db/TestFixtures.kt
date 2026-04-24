package com.woliveiras.palabrita.core.data.db

import com.woliveiras.palabrita.core.data.db.entity.ChatMessageEntity
import com.woliveiras.palabrita.core.data.db.entity.GameSessionEntity
import com.woliveiras.palabrita.core.data.db.entity.ModelConfigEntity
import com.woliveiras.palabrita.core.data.db.entity.PlayerStatsEntity
import com.woliveiras.palabrita.core.data.db.entity.PuzzleEntity

fun createTestPuzzleEntity(
  id: Long = 0,
  word: String = "gatos",
  wordDisplay: String = word,
  language: String = "pt",
  difficulty: Int = 5,
  category: String = "",
  hints: String = """["Dica 1","Dica 2","Dica 3"]""",
  source: String = "AI",
  generatedAt: Long = System.currentTimeMillis(),
  playedAt: Long? = null,
  isPlayed: Boolean = false,
  isValid: Boolean = true,
): PuzzleEntity =
  PuzzleEntity(
    id = id,
    word = word,
    wordDisplay = wordDisplay,
    language = language,
    difficulty = difficulty,
    category = category,
    hints = hints,
    source = source,
    generatedAt = generatedAt,
    playedAt = playedAt,
    isPlayed = isPlayed,
    isValid = isValid,
  )

fun createTestPlayerStatsEntity(
  id: Int = 1,
  totalPlayed: Int = 0,
  totalWon: Int = 0,
  avgAttempts: Float = 0f,
  preferredLanguage: String = "pt",
  guessDistribution: String = "{}",
  lastPlayedAt: Long = 0,
): PlayerStatsEntity =
  PlayerStatsEntity(
    id = id,
    totalPlayed = totalPlayed,
    totalWon = totalWon,
    avgAttempts = avgAttempts,
    preferredLanguage = preferredLanguage,
    guessDistribution = guessDistribution,
    lastPlayedAt = lastPlayedAt,
  )

fun createTestGameSessionEntity(
  id: Long = 0,
  puzzleId: Long = 1,
  attempts: String = "[]",
  startedAt: Long = System.currentTimeMillis(),
  completedAt: Long? = null,
  hintsUsed: Int = 0,
  won: Boolean = false,
): GameSessionEntity =
  GameSessionEntity(
    id = id,
    puzzleId = puzzleId,
    attempts = attempts,
    startedAt = startedAt,
    completedAt = completedAt,
    hintsUsed = hintsUsed,
    won = won,
  )

fun createTestChatMessageEntity(
  id: Long = 0,
  puzzleId: Long = 1,
  role: String = "user",
  content: String = "O que significa essa palavra?",
  timestamp: Long = System.currentTimeMillis(),
): ChatMessageEntity =
  ChatMessageEntity(
    id = id,
    puzzleId = puzzleId,
    role = role,
    content = content,
    timestamp = timestamp,
  )

fun createTestModelConfigEntity(
  id: Int = 1,
  modelId: String = "none",
  downloadState: String = "NOT_DOWNLOADED",
  modelPath: String? = null,
  sizeBytes: Long = 0,
  selectedAt: Long = System.currentTimeMillis(),
): ModelConfigEntity =
  ModelConfigEntity(
    id = id,
    modelId = modelId,
    downloadState = downloadState,
    modelPath = modelPath,
    sizeBytes = sizeBytes,
    selectedAt = selectedAt,
  )

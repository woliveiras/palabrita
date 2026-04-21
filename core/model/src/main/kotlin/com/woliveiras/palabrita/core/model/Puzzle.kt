package com.woliveiras.palabrita.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Puzzle(
  val id: Long = 0,
  val word: String,
  val wordDisplay: String,
  val language: String,
  val difficulty: Int,
  val category: String,
  val hints: List<String>,
  val source: PuzzleSource,
  val generatedAt: Long,
  val playedAt: Long? = null,
  val isPlayed: Boolean = false,
  val isValid: Boolean = true,
)

package com.woliveiras.palabrita.core.model

data class GameSession(
    val id: Long = 0,
    val puzzleId: Long,
    val attempts: List<String> = emptyList(),
    val startedAt: Long,
    val completedAt: Long? = null,
    val hintsUsed: Int = 0,
    val won: Boolean = false,
)

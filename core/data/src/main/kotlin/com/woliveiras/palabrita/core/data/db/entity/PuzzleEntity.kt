package com.woliveiras.palabrita.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "puzzles",
    indices =
        [
            Index(value = ["isPlayed", "language"]),
            Index(value = ["word"], unique = true),
        ],
)
data class PuzzleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val wordDisplay: String,
    val language: String,
    val difficulty: Int,
    val category: String,
    val hints: String,
    val source: String,
    val generatedAt: Long,
    val playedAt: Long? = null,
    val isPlayed: Boolean = false,
    val isValid: Boolean = true,
)

package com.woliveiras.palabrita.core.data.seeder

import android.content.Context
import com.woliveiras.palabrita.core.data.db.dao.PuzzleDao
import com.woliveiras.palabrita.core.data.mapper.toEntity
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.PuzzleSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface StaticPuzzleSeeder {
  suspend fun seedIfNeeded(language: String)
  suspend fun seedAllLanguages()
}

@Singleton
class StaticPuzzleSeederImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val puzzleDao: PuzzleDao,
) : StaticPuzzleSeeder {

  private val json = Json { ignoreUnknownKeys = true }

  override suspend fun seedIfNeeded(language: String) {
    val existing = puzzleDao.countAllUnplayed(language)
    if (existing > 0) return

    val puzzles = loadStaticPuzzles(language)
    puzzleDao.insertAll(puzzles.map { it.toEntity() })
  }

  override suspend fun seedAllLanguages() {
    SUPPORTED_LANGUAGES.forEach { lang -> seedIfNeeded(lang) }
  }

  private fun loadStaticPuzzles(language: String): List<Puzzle> {
    val fileName = "static_puzzles_$language.json"
    val jsonString = try {
      context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
      return emptyList()
    }

    val raw = json.decodeFromString<List<StaticPuzzleJson>>(jsonString)
    val now = System.currentTimeMillis()
    return raw.map { entry ->
      Puzzle(
        word = entry.word,
        wordDisplay = entry.wordDisplay,
        language = entry.language,
        difficulty = entry.difficulty,
        category = entry.category,
        hints = entry.hints,
        source = PuzzleSource.STATIC,
        generatedAt = now,
      )
    }
  }

  companion object {
    val SUPPORTED_LANGUAGES = listOf("pt", "en", "es")
  }
}

@Serializable
private data class StaticPuzzleJson(
  val word: String,
  val wordDisplay: String,
  val language: String,
  val difficulty: Int,
  val category: String,
  val hints: List<String>,
  val source: String,
)

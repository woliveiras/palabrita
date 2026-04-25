package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.ai.GenerationActivity
import com.woliveiras.palabrita.core.ai.PuzzleGenerator
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.Puzzle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakePuzzleGenerator : PuzzleGenerator {
  private val _activity = MutableStateFlow<GenerationActivity?>(null)
  override val activity: StateFlow<GenerationActivity?> = _activity

  override suspend fun generateBatch(
    count: Int,
    language: String,
    wordLength: Int,
    recentWords: List<String>,
    allExistingWords: Set<String>,
    modelId: ModelId,
    onPuzzleAttempted: suspend (successCount: Int) -> Unit,
  ): List<Puzzle> = emptyList()
}

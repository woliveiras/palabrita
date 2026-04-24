package com.woliveiras.palabrita.core.common

import java.text.Normalizer

object TextNormalizer {

  private val COMBINING_MARKS = Regex("\\p{InCombiningDiacriticalMarks}+")

  /** Strips diacritics and returns lowercase ASCII. "ação" → "acao", "niño" → "nino". */
  fun normalizeToAscii(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(COMBINING_MARKS, "")
}

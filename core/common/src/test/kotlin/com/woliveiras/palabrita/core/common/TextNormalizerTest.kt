package com.woliveiras.palabrita.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextNormalizerTest {

  @Test
  fun `strips Portuguese accents`() {
    assertThat(TextNormalizer.normalizeToAscii("ação")).isEqualTo("acao")
    assertThat(TextNormalizer.normalizeToAscii("coração")).isEqualTo("coracao")
    assertThat(TextNormalizer.normalizeToAscii("café")).isEqualTo("cafe")
    assertThat(TextNormalizer.normalizeToAscii("pão")).isEqualTo("pao")
    assertThat(TextNormalizer.normalizeToAscii("avô")).isEqualTo("avo")
  }

  @Test
  fun `strips Spanish accents`() {
    assertThat(TextNormalizer.normalizeToAscii("niño")).isEqualTo("nino")
    assertThat(TextNormalizer.normalizeToAscii("más")).isEqualTo("mas")
    assertThat(TextNormalizer.normalizeToAscii("canción")).isEqualTo("cancion")
    assertThat(TextNormalizer.normalizeToAscii("pingüino")).isEqualTo("pinguino")
  }

  @Test
  fun `returns ASCII text unchanged`() {
    assertThat(TextNormalizer.normalizeToAscii("gatos")).isEqualTo("gatos")
    assertThat(TextNormalizer.normalizeToAscii("house")).isEqualTo("house")
  }

  @Test
  fun `lowercases input`() {
    assertThat(TextNormalizer.normalizeToAscii("AÇÃO")).isEqualTo("acao")
    assertThat(TextNormalizer.normalizeToAscii("Gatos")).isEqualTo("gatos")
  }

  @Test
  fun `handles empty string`() {
    assertThat(TextNormalizer.normalizeToAscii("")).isEqualTo("")
  }

  @Test
  fun `handles cedilla`() {
    assertThat(TextNormalizer.normalizeToAscii("ç")).isEqualTo("c")
    assertThat(TextNormalizer.normalizeToAscii("Ç")).isEqualTo("c")
  }
}

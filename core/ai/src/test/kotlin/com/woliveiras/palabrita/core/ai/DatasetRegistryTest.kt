package com.woliveiras.palabrita.core.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DatasetRegistryTest {

  private val registry = DatasetRegistry()

  @Test
  fun `availableLanguages returns entries from manifest in order`() {
    val languages = registry.availableLanguages()
    assertThat(languages).isNotEmpty()
    assertThat(languages.map { it.code }).containsAtLeast("pt", "en", "es").inOrder()
  }

  @Test
  fun `availableLanguages only returns entries with existing wordlist files`() {
    val languages = registry.availableLanguages()
    languages.forEach { info ->
      val stream = DatasetRegistry::class.java.getResourceAsStream("/wordlists/${info.code}.json")
      assertThat(stream).isNotNull()
    }
  }

  @Test
  fun `findByCode returns DatasetInfo for known code`() {
    val pt = registry.findByCode("pt")
    assertThat(pt).isNotNull()
    assertThat(pt!!.code).isEqualTo("pt")
    assertThat(pt.displayName).isEqualTo("Português")
    assertThat(pt.promptName).isEqualTo("Brazilian Portuguese")
  }

  @Test
  fun `findByCode returns null for unknown code`() {
    val result = registry.findByCode("xx")
    assertThat(result).isNull()
  }

  @Test
  fun `promptName returns promptName for known code`() {
    assertThat(registry.promptName("pt")).isEqualTo("Brazilian Portuguese")
    assertThat(registry.promptName("en")).isEqualTo("English")
    assertThat(registry.promptName("es")).isEqualTo("Spanish")
  }

  @Test
  fun `promptName falls back to raw code for unknown language`() {
    assertThat(registry.promptName("xx")).isEqualTo("xx")
  }

  @Test
  fun `each entry has non-empty required fields`() {
    val languages = registry.availableLanguages()
    languages.forEach { info ->
      assertThat(info.code).isNotEmpty()
      assertThat(info.displayName).isNotEmpty()
      assertThat(info.flag).isNotEmpty()
      assertThat(info.promptName).isNotEmpty()
    }
  }

  @Test
  fun `availableLanguages result is cached across calls`() {
    val first = registry.availableLanguages()
    val second = registry.availableLanguages()
    assertThat(first).isSameInstanceAs(second)
  }
}

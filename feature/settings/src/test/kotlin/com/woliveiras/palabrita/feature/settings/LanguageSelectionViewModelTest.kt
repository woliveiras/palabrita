package com.woliveiras.palabrita.feature.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.testing.FakeAppPreferences
import com.woliveiras.palabrita.core.testing.FakePuzzleRepository
import com.woliveiras.palabrita.core.testing.FakeStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LanguageSelectionViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // --- Initialization ---

  @Test
  fun `init loads app language from preferences`() = runTest {
    val prefs = FakeAppPreferences().apply { /* default locale */ }
    val vm = createViewModel(appPrefs = prefs)
    testDispatcher.scheduler.advanceUntilIdle()
    // State should reflect what's in FakeAppPreferences (default locale language)
    assertThat(vm.state.value.appLanguage).isNotEmpty()
  }

  @Test
  fun `init loads game language from stats repository`() = runTest {
    val statsRepo = FakeStatsRepository(PlayerStats(preferredLanguage = "es"))
    val vm = createViewModel(statsRepo = statsRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.gameLanguage).isEqualTo("es")
  }

  // --- App language ---

  @Test
  fun `changing app language saves to preferences and emits RestartForLocale`() = runTest {
    val prefs = FakeAppPreferences()
    prefs.setAppLanguage("pt") // set BEFORE creating VM so init reads "pt"
    val vm = createViewModel(appPrefs = prefs)
    testDispatcher.scheduler.advanceUntilIdle()

    vm.events.test {
      vm.onAction(LanguageSelectionAction.ChangeAppLanguage("en"))
      testDispatcher.scheduler.advanceUntilIdle()
      val event = awaitItem()
      assertThat(event).isInstanceOf(LanguageSelectionEvent.RestartForLocale::class.java)
      assertThat((event as LanguageSelectionEvent.RestartForLocale).language).isEqualTo("en")
      cancelAndIgnoreRemainingEvents()
    }
    assertThat(prefs.appLanguage.first()).isEqualTo("en")
  }

  @Test
  fun `changing app language to same value emits no event`() = runTest {
    val prefs = FakeAppPreferences()
    prefs.setAppLanguage("pt") // set BEFORE creating VM
    val vm = createViewModel(appPrefs = prefs)
    testDispatcher.scheduler.advanceUntilIdle()

    vm.events.test {
      vm.onAction(LanguageSelectionAction.ChangeAppLanguage("pt"))
      testDispatcher.scheduler.advanceUntilIdle()
      expectNoEvents()
      cancelAndIgnoreRemainingEvents()
    }
  }

  // --- Game language ---

  @Test
  fun `changing game language to different value shows confirm dialog`() = runTest {
    val statsRepo = FakeStatsRepository(PlayerStats(preferredLanguage = "pt"))
    val vm = createViewModel(statsRepo = statsRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(LanguageSelectionAction.ChangeGameLanguage("en"))
    assertThat(vm.state.value.showConfirmDialog).isTrue()
    assertThat(vm.state.value.pendingGameLanguage).isEqualTo("en")
  }

  @Test
  fun `changing game language to same value does not show dialog`() = runTest {
    val statsRepo = FakeStatsRepository(PlayerStats(preferredLanguage = "pt"))
    val vm = createViewModel(statsRepo = statsRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(LanguageSelectionAction.ChangeGameLanguage("pt"))
    assertThat(vm.state.value.showConfirmDialog).isFalse()
    assertThat(vm.state.value.pendingGameLanguage).isNull()
  }

  @Test
  fun `dismiss dialog clears pending language and hides dialog`() = runTest {
    val statsRepo = FakeStatsRepository(PlayerStats(preferredLanguage = "pt"))
    val vm = createViewModel(statsRepo = statsRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(LanguageSelectionAction.ChangeGameLanguage("en"))
    vm.onAction(LanguageSelectionAction.DismissDialog)
    assertThat(vm.state.value.showConfirmDialog).isFalse()
    assertThat(vm.state.value.pendingGameLanguage).isNull()
  }

  // --- Confirm game language ---

  @Test
  fun `confirming game language with generateNow=true deletes old puzzles and emits NavigateToGeneration`() =
    runTest {
      val statsRepo = FakeStatsRepository(PlayerStats(preferredLanguage = "pt"))
      val puzzleRepo = FakePuzzleRepository()
      val vm = createViewModel(statsRepo = statsRepo, puzzleRepo = puzzleRepo)
      testDispatcher.scheduler.advanceUntilIdle()
      vm.onAction(LanguageSelectionAction.ChangeGameLanguage("en"))

      vm.events.test {
        vm.onAction(LanguageSelectionAction.ConfirmGameLanguage(generateNow = true))
        testDispatcher.scheduler.advanceUntilIdle()
        val event = awaitItem()
        assertThat(event).isInstanceOf(LanguageSelectionEvent.NavigateToGeneration::class.java)
        assertThat((event as LanguageSelectionEvent.NavigateToGeneration).language).isEqualTo("en")
        cancelAndIgnoreRemainingEvents()
      }

      assertThat(puzzleRepo.unplayedByLanguageDeleted).isEqualTo("pt")
      assertThat(vm.state.value.gameLanguage).isEqualTo("en")
    }

  @Test
  fun `confirming game language with generateNow=false deletes old puzzles and emits NavigateBack`() =
    runTest {
      val statsRepo = FakeStatsRepository(PlayerStats(preferredLanguage = "es"))
      val puzzleRepo = FakePuzzleRepository()
      val vm = createViewModel(statsRepo = statsRepo, puzzleRepo = puzzleRepo)
      testDispatcher.scheduler.advanceUntilIdle()
      vm.onAction(LanguageSelectionAction.ChangeGameLanguage("pt"))

      vm.events.test {
        vm.onAction(LanguageSelectionAction.ConfirmGameLanguage(generateNow = false))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(awaitItem()).isInstanceOf(LanguageSelectionEvent.NavigateBack::class.java)
        cancelAndIgnoreRemainingEvents()
      }

      assertThat(puzzleRepo.unplayedByLanguageDeleted).isEqualTo("es")
    }

  @Test
  fun `confirming game language updates stats repository`() = runTest {
    val statsRepo = FakeStatsRepository(PlayerStats(preferredLanguage = "pt"))
    val vm = createViewModel(statsRepo = statsRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(LanguageSelectionAction.ChangeGameLanguage("en"))
    vm.onAction(LanguageSelectionAction.ConfirmGameLanguage(generateNow = false))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(statsRepo.getStats().preferredLanguage).isEqualTo("en")
  }

  @Test
  fun `changing game language to same value while dialog is open closes dialog`() = runTest {
    val statsRepo = FakeStatsRepository(PlayerStats(preferredLanguage = "pt"))
    val vm = createViewModel(statsRepo = statsRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(LanguageSelectionAction.ChangeGameLanguage("en")) // opens dialog
    assertThat(vm.state.value.showConfirmDialog).isTrue()
    vm.onAction(LanguageSelectionAction.ChangeGameLanguage("pt")) // same as current
    assertThat(vm.state.value.showConfirmDialog).isFalse()
    assertThat(vm.state.value.pendingGameLanguage).isNull()
  }

  // --- Helpers ---

  private fun createViewModel(
    appPrefs: FakeAppPreferences = FakeAppPreferences(),
    statsRepo: FakeStatsRepository = FakeStatsRepository(PlayerStats(preferredLanguage = "pt")),
    puzzleRepo: FakePuzzleRepository = FakePuzzleRepository(),
  ) =
    LanguageSelectionViewModel(
      datasetRegistry = com.woliveiras.palabrita.core.ai.DatasetRegistry(),
      appPreferences = appPrefs,
      statsRepository = statsRepo,
      puzzleRepository = puzzleRepo,
    )
}

package com.woliveiras.palabrita.feature.home

sealed class HomeAction {
  data class StartDailyChallenge(val index: Int) : HomeAction()
  data object StartFreePlay : HomeAction()
  data class NavigateToChat(val puzzleId: Long) : HomeAction()
  data object DismissGenerationBanner : HomeAction()
}

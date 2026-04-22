package com.woliveiras.palabrita.feature.home

sealed class HomeAction {
  data object StartGame : HomeAction()

  data object DismissGenerationBanner : HomeAction()
}

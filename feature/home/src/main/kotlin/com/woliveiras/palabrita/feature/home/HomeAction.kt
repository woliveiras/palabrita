package com.woliveiras.palabrita.feature.home

sealed class HomeAction {
  data object Play : HomeAction()

  data object GenerateMore : HomeAction()

  data object OpenSettings : HomeAction()

  data object OpenAboutAi : HomeAction()

  data object DismissGenerationBanner : HomeAction()
}

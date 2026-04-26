pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "palabrita"

include(":app")

include(":core:common")

include(":core:model")

include(":core:data")

include(":core:ai")

include(":core:testing")

include(":feature:onboarding")

include(":feature:game")

include(":feature:home")

include(":feature:settings")

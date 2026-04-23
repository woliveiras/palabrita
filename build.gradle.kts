plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.hilt) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.spotless)
  alias(libs.plugins.detekt)
}

spotless {
  kotlin {
    target("**/*.kt")
    targetExclude("**/build/**")
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
  }
  kotlinGradle {
    target("**/*.gradle.kts")
    targetExclude("**/build/**")
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
  }
}

detekt {
  buildUponDefaultConfig = true
  baseline = file("detekt-baseline.xml")
  parallel = true
  source.setFrom(
    fileTree(rootProject.projectDir) {
      include("**/src/**/*.kt")
      exclude("**/build/**")
    }
  )
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
  jvmTarget = "21"
  reports {
    html.required.set(true)
    xml.required.set(false)
    txt.required.set(false)
  }
}

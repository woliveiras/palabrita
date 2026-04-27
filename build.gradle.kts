// Force safe versions of transitive build-tool dependencies.
// These come from AGP (signing, emulator proto, sdklib) — not shipped in the APK.
buildscript {
  configurations.configureEach {
    resolutionStrategy.eachDependency {
      when (requested.group) {
        "io.netty" -> useVersion("4.1.132.Final")
        "org.bouncycastle" -> useVersion("1.84")
      }
      if (requested.group == "org.jdom" && requested.name == "jdom2") useVersion("2.0.6.1")
      if (requested.group == "org.bitbucket.b_c" && requested.name == "jose4j") useVersion("0.9.6")
      if (requested.group == "org.apache.commons" && requested.name == "commons-lang3")
        useVersion("3.17.0")
      if (requested.group == "org.apache.httpcomponents" && requested.name == "httpclient")
        useVersion("4.5.14")
    }
  }
}

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

allprojects {
  configurations.configureEach {
    resolutionStrategy.eachDependency {
      when (requested.group) {
        "io.netty" -> useVersion("4.1.132.Final")
        "org.bouncycastle" -> useVersion("1.84")
      }
      if (requested.group == "org.jdom" && requested.name == "jdom2") useVersion("2.0.6.1")
      if (requested.group == "org.bitbucket.b_c" && requested.name == "jose4j") useVersion("0.9.6")
      if (requested.group == "org.apache.commons" && requested.name == "commons-lang3")
        useVersion("3.17.0")
      if (requested.group == "org.apache.httpcomponents" && requested.name == "httpclient")
        useVersion("4.5.14")
    }
  }
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

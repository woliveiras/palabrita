plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
}

android {
  namespace = "com.woliveiras.palabrita"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.woliveiras.palabrita"
    minSdk = 31
    targetSdk = 35
    versionCode = 1
    versionName = "0.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }


  buildFeatures { compose = true }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
  }
}

dependencies {
  implementation(project(":core:model"))
  implementation(project(":core:data"))
  implementation(project(":core:ai"))
  implementation(project(":core:common"))
  implementation(project(":feature:onboarding"))
  implementation(project(":feature:home"))
  implementation(project(":feature:game"))
  implementation(project(":feature:chat"))
  implementation(project(":feature:settings"))

  implementation(platform(libs.compose.bom))
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.graphics)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.compose.material3)
  implementation(libs.compose.material.icons.extended)
  debugImplementation(libs.compose.ui.tooling)
  debugImplementation(libs.compose.ui.test.manifest)

  implementation(libs.core.ktx)
  implementation(libs.activity.compose)
  implementation(libs.lifecycle.runtime.compose)
  implementation(libs.lifecycle.viewmodel.compose)
  implementation(libs.navigation.compose)
  implementation(libs.kotlinx.serialization.json)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.hilt.navigation.compose)

  implementation(libs.workmanager)
  implementation(libs.hilt.work)
  ksp(libs.hilt.work.compiler)

  testImplementation(libs.junit)
  testImplementation(libs.truth)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.compose.ui.test.junit4)
}

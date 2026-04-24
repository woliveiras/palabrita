plugins {
  alias(libs.plugins.android.library)
}

android {
  namespace = "com.woliveiras.palabrita.core.testing"
  compileSdk = 36

  defaultConfig { minSdk = 31 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) } }

dependencies {
  implementation(project(":core:model"))
  implementation(project(":core:ai"))
  implementation(libs.kotlinx.coroutines.core)
}

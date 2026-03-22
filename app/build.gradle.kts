plugins {
  alias(libs.plugins.android.application)

  id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"
}

android {
  namespace = "com.sheryv.p2premotecontroller"
  compileSdk {
    version = release(36) {
      minorApiLevel = 1
    }
  }

  packaging {
    resources.excludes.add("META-INF/INDEX.LIST")
    resources.excludes.add("META-INF/io.netty.versions.properties")
  }

  defaultConfig {
    applicationId = "com.sheryv.p2premotecontroller"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.lifecycle.process)
  implementation(libs.androidx.preference)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)

  implementation(files("libs/virtualjoystick-3.0.0.aar"))

  implementation("org.java-websocket:Java-WebSocket:1.6.0")

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
  implementation("io.ktor:ktor-server-core-jvm:3.4.1")
  implementation("io.ktor:ktor-server-netty-jvm:3.4.1")
  implementation("io.ktor:ktor-server-websockets:3.4.1")
  implementation("androidx.preference:preference-ktx:1.2.1")

  val lifecycle_version = "2.10.0"
  val arch_version = "2.2.0"

  implementation("androidx.activity:activity-ktx:1.12.4")
  implementation("androidx.fragment:fragment-ktx:1.8.9")

  // ViewModel
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
  // ViewModel utilities for Compose
//    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version")
  // LiveData
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
  // Lifecycle utilities for Compose
//    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycle_version")

  // Saved state module for ViewModel
  implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycle_version")

  // Typed DataStore for custom data objects (for example, using Proto or JSON).
//    implementation("androidx.datastore:datastore:1.2.0")
//
//    // Alternatively - without an Android dependency.
//    implementation("androidx.datastore:datastore-core:1.2.0")

  // ViewModel integration with Navigation3
//    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.11.0-alpha01")

  // Annotation processor
//    kapt("androidx.lifecycle:lifecycle-compiler:$lifecycle_version")

//    // optional - helpers for implementing LifecycleOwner in a Service
//    implementation("androidx.lifecycle:lifecycle-service:$lifecycle_version")
//
//    // optional - ProcessLifecycleOwner provides a lifecycle for the whole application process
//    implementation("androidx.lifecycle:lifecycle-process:$lifecycle_version")
//
//    // optional - ReactiveStreams support for LiveData
//    implementation("androidx.lifecycle:lifecycle-reactivestreams-ktx:$lifecycle_version")

  // optional - Test helpers for LiveData
  testImplementation("androidx.arch.core:core-testing:$arch_version")

  // optional - Test helpers for Lifecycle runtime
  testImplementation("androidx.lifecycle:lifecycle-runtime-testing:$lifecycle_version")
  testImplementation("androidx.lifecycle:lifecycle-extensions:$arch_version")
}

kotlin {
  sourceSets.all {
    languageSettings.enableLanguageFeature("ExplicitBackingFields")
  }
//    compilerOptions {
//        freeCompilerArgs.add("-Xexplicit-backing-fields")
//    }
}
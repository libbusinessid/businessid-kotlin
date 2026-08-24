// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

// AGP 9 carries Kotlin support itself; a separate Kotlin plugin is refused.
plugins {
    id("com.android.library") version "9.3.1"
}

android {
    namespace = "io.libbusinessid.consumer.android"
    compileSdk = 36

    defaultConfig {
        // The floor this library announces for Android consumers.
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        // A library whose bytecode or API an Android toolchain could not accept
        // fails here rather than at a consumer's build. NewApi and InvalidPackage
        // are the two that would catch it, and both are on.
        warningsAsErrors = true
        abortOnError = true
        // Two findings about this build file rather than about the library: it
        // reads its coordinates from a property on purpose, and the SDK it
        // compiles against is pinned so the check is reproducible.
        disable += setOf("UseTomlInstead", "GradleDependency")
    }
}

dependencies {
    implementation("io.libbusinessid:businessid:${providers.gradleProperty("businessid.version").get()}")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

// AGP 9 carries Kotlin support itself; a separate Kotlin plugin is refused.
plugins {
    id("com.android.library") version "9.3.2"
}

android {
    namespace = "org.entid.consumer.android"
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
    implementation("org.entid:entid:${providers.gradleProperty("entid.version").get()}")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

// Lint's verdict depends on a network index of released plugin versions, which
// it never declares as an input. A warm build directory therefore replays a
// stale pass: this check reported success locally on a commit CI failed. It is
// worth a few seconds to have a local run mean what it says.
tasks.matching { it.name.startsWith("lint") }.configureEach {
    outputs.upToDateWhen { false }
}

tasks.withType<Test>().configureEach {
    // The version of the ruleset the engine build compiled in, so a resync does
    // not have to be repeated by hand in a second build.
    systemProperty("entid.rules.version", providers.gradleProperty("entid.rules").get())
}

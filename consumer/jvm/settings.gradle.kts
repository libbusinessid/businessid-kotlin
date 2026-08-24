// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

// A build of its own, on purpose. Including it in the engine build would test
// the project rather than the published artefact.

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

// The consumer compiles and runs on the floor this library announces. Fetching
// that toolchain is what makes the claim a measurement rather than a promise.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        maven {
            name = "engineStaging"
            url = uri(providers.gradleProperty("businessid.repository").get())
        }
        mavenCentral()
    }
}

rootProject.name = "businessid-consumer-jvm"

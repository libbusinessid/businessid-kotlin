// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

// A build of its own: the point is to consume the published artefact the way an
// Android project would, not to compile the library again with other settings.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

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
        google()
        mavenCentral()
    }
}

rootProject.name = "businessid-consumer-android"

// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

// Lets a build on any JDK fetch the toolchain this project pins.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "entid-kotlin"

include("entid")
include("generator")
include("testee")
include("benchmarks")

// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "businessid-kotlin"

include("businessid")
include("generator")
include("testee")
include("benchmarks")

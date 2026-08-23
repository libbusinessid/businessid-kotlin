// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
    implementation(libs.bcv.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.protobuf.gradle.plugin)
}

kotlin {
    jvmToolchain(17)
}

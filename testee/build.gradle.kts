// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("businessid.kotlin-base")
    application
}

description = "Conformance testee: reads TesteeRequest on stdin, calls the public API, writes TesteeResponse."

dependencies {
    implementation(project(":businessid"))
    // The generator answers the load_ruleset cases, exactly as testee.proto
    // field 7 describes for an engine that generates code ahead of time.
    implementation(project(":generator"))
    implementation(libs.protobuf.javalite)
}

application {
    mainClass.set("io.libbusinessid.testee.MainKt")
    applicationName = "businessid-testee"
}

// The runner speaks to this process over stdout. Nothing else may write there.
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

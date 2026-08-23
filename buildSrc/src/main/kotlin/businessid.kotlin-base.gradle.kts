// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlinx.kover")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

// The toolchain is locked so that a build produces the same bytecode everywhere.
kotlin {
    jvmToolchain(BuildConstants.TOOLCHAIN_JDK)
}

// Published bytecode targets 11: the floor Android's toolchain accepts without
// desugaring gymnastics, and low enough for any maintained JVM. Test code is not
// published and compiles against the toolchain, which lets the test libraries
// require a newer runtime than the library itself does.
tasks.withType<KotlinCompile>().configureEach {
    val isTest = name.contains("Test")
    compilerOptions {
        jvmTarget.set(if (isTest) JvmTarget.JVM_17 else JvmTarget.JVM_11)
        allWarningsAsErrors.set(true)
        extraWarnings.set(true)
        freeCompilerArgs.add(if (isTest) "-Xjdk-release=17" else "-Xjdk-release=11")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(if (name.contains("Test")) 17 else 11)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Tests read the pinned schemas, bundle and corpus from spec/. They are
    // inputs of the build, never resources of the published jar.
    val specDir = rootProject.layout.projectDirectory.dir("spec")
    inputs.dir(specDir).withPropertyName("spec")
    systemProperty("businessid.spec.dir", specDir.asFile.absolutePath)
    systemProperty("businessid.project.version", project.version.toString())
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    // A deterministic engine must not depend on the ambient locale or timezone.
    systemProperty("user.language", "tr")
    systemProperty("user.country", "TR")
    systemProperty("user.timezone", "Pacific/Kiritimati")
    systemProperty("file.encoding", "UTF-8")
}

// Reproducible archives: no timestamps, stable entry order.
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    dirPermissions { unix("rwxr-xr-x") }
    filePermissions { unix("rw-r--r--") }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    parallel = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "11"
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(false)
        md.required.set(false)
        txt.required.set(false)
    }
}
tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "11"
}

dependencies {
    add("detektPlugins", libs.findLibrary("detekt-formatting").get())
    add("testImplementation", libs.findLibrary("kotlin-test").get())
    add("testImplementation", libs.findLibrary("junit-jupiter").get())
    add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
}

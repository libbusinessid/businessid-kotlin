// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

plugins {
    kotlin("jvm") version "2.4.10"
    application
}

// The library, exactly as a project would take it from a repository.
dependencies {
    implementation("io.github.libbusinessid:businessid:${providers.gradleProperty("businessid.version").get()}")
    testImplementation(kotlin("test"))
}

kotlin {
    // The floor this library announces: a consumer on Java 11 must be able to
    // take it. Compiling here on 11 is what proves the bytecode target holds.
    jvmToolchain(11)
}

application {
    mainClass.set("io.libbusinessid.consumer.MainKt")
}

tasks.test {
    useJUnitPlatform()
    // The version of the ruleset the engine build compiled in, so a resync does
    // not have to be repeated by hand in a second build.
    systemProperty("businessid.rules.version", providers.gradleProperty("businessid.rules").get())
}

// What a caller actually inherits.
//
// The library declares no dependency of its own, so the runtime classpath of a
// project that takes it holds the library, the Kotlin standard library, and the
// annotations artefact the standard library itself brings. Anything else here
// would be something the library imposed without saying so.
tasks.register("verifyDependencies") {
    val resolved = configurations.named("runtimeClasspath").map { classpath ->
        classpath.incoming.resolutionResult.allComponents
            .map { it.moduleVersion?.module?.toString() ?: it.id.displayName }
            // The consumer itself is the root of the graph, not a dependency.
            .filterNot { it.startsWith("project ") || it.startsWith(":") }
            .distinct()
            .sorted()
    }
    doLast {
        val expected = listOf(
            "io.github.libbusinessid:businessid",
            "org.jetbrains.kotlin:kotlin-stdlib",
            "org.jetbrains:annotations",
        )
        val actual = resolved.get()
        check(actual == expected) {
            "the runtime classpath is\n  ${actual.joinToString("\n  ")}\nwhere it should be\n  " +
                expected.joinToString("\n  ")
        }
        logger.lifecycle("the library brings nothing but the Kotlin standard library")
    }
}

tasks.named("check") {
    dependsOn("verifyDependencies")
}

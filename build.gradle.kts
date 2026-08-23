// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

plugins {
    base
}

// Every Gradle plugin this build applies is on the classpath through buildSrc,
// pinned by gradle/libs.versions.toml. Nothing resolves a plugin version here.

val generatedSourceDir: Directory =
    layout.projectDirectory.dir("businessid/src/main/kotlin/io/libbusinessid/generated")

val generatorClasspath: Configuration by configurations.creating

dependencies {
    generatorClasspath(project(":generator"))
}

/**
 * Emits the engine sources from the pinned bundle.
 *
 * The bundle is read from a path. It is a build input, not a resource: nothing
 * about it survives into the published jar.
 */
val generateEngine = tasks.register<JavaExec>("generateEngine") {
    group = "build"
    description = "Regenerates io.libbusinessid.generated from spec/businessid-rules.binpb."
    classpath = generatorClasspath
    mainClass.set("io.libbusinessid.generator.MainKt")
    val bundle = layout.projectDirectory.file("spec/businessid-rules.binpb")
    val lock = layout.projectDirectory.file("rules.lock")
    inputs.file(bundle)
    inputs.file(lock)
    outputs.dir(generatedSourceDir)
    args(
        "--bundle", bundle.asFile.absolutePath,
        "--lock", lock.asFile.absolutePath,
        "--out", generatedSourceDir.asFile.absolutePath,
    )
}

/**
 * Fails when the committed sources differ from what the bundle produces today.
 */
val checkGenerated = tasks.register<JavaExec>("checkGenerated") {
    group = "verification"
    description = "Verifies that the committed generated sources match the pinned bundle."
    classpath = generatorClasspath
    mainClass.set("io.libbusinessid.generator.MainKt")
    val bundle = layout.projectDirectory.file("spec/businessid-rules.binpb")
    val lock = layout.projectDirectory.file("rules.lock")
    inputs.file(bundle)
    inputs.file(lock)
    inputs.dir(generatedSourceDir)
    outputs.upToDateWhen { false }
    args(
        "--bundle", bundle.asFile.absolutePath,
        "--lock", lock.asFile.absolutePath,
        "--out", generatedSourceDir.asFile.absolutePath,
        "--check",
    )
}

tasks.named("check") {
    dependsOn(checkGenerated)
}

// ---------------------------------------------------------------------------
// ktlint, run from its CLI so the version is pinned in the catalogue like every
// other tool. Detekt carries the same rule set through detekt-formatting; the
// two disagreeing would be a finding worth having.
// ---------------------------------------------------------------------------

val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(libs.ktlint.cli)
}

val ktlintSources = listOf(
    "businessid/src/**/*.kt",
    "generator/src/**/*.kt",
    "testee/src/**/*.kt",
    "benchmarks/src/**/*.kt",
    "buildSrc/src/**/*.kt",
    "**/*.gradle.kts",
    "!**/build/**",
    "!businessid/src/main/kotlin/io/libbusinessid/generated/**",
)

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Runs ktlint over the hand written sources."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args("--relative", "--reporter=plain")
    args(ktlintSources)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Applies ktlint fixes to the hand written sources."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args("--relative", "--format")
    args(ktlintSources)
}

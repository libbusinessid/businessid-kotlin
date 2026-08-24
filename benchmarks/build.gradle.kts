// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("businessid.kotlin-base")
}

description = "JMH benchmarks: cold load, simple validation, complex checksum, early rejection, parallel execution."

dependencies {
    implementation(project(":businessid"))
    implementation(libs.jmh.core)
    compileOnly(libs.jmh.generator)
}

// JMH's annotation processor is driven over compiled bytecode rather than
// through kapt: one fewer Kotlin compiler plugin in the build, and the same
// generated harness.
val jmhGeneratorClasspath: Configuration = configurations.create("jmhGeneratorClasspath")

dependencies {
    jmhGeneratorClasspath(libs.jmh.core)
    jmhGeneratorClasspath(libs.jmh.generator)
    jmhGeneratorClasspath(libs.jmh.generator.bytecode)
}

val jmhGeneratedSources = layout.buildDirectory.dir("jmh-generated/sources")
val jmhGeneratedResources = layout.buildDirectory.dir("jmh-generated/resources")

val jmhGenerate = tasks.register<JavaExec>("jmhGenerate") {
    description = "Generates the JMH harness from the compiled benchmark classes."
    dependsOn(tasks.named("compileKotlin"))
    classpath =
        jmhGeneratorClasspath +
        sourceSets.main.get().output +
        sourceSets.main.get().runtimeClasspath
    mainClass.set("org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator")
    // Kotlin and Java each get their own output directory; the benchmarks are
    // Kotlin, so that is the one the generator reads.
    val classesDir = layout.buildDirectory.dir("classes/kotlin/main").get().asFile
    args(
        classesDir.absolutePath,
        jmhGeneratedSources.get().asFile.absolutePath,
        jmhGeneratedResources.get().asFile.absolutePath,
    )
    outputs.dir(jmhGeneratedSources)
    outputs.dir(jmhGeneratedResources)
}

val jmhCompile = tasks.register<JavaCompile>("jmhCompileGenerated") {
    dependsOn(jmhGenerate)
    source = fileTree(jmhGeneratedSources)
    classpath =
        jmhGeneratorClasspath +
        sourceSets.main.get().output +
        sourceSets.main.get().runtimeClasspath
    destinationDirectory.set(layout.buildDirectory.dir("jmh-generated/classes"))
    options.release.set(11)
    // The generated harness is not ours to lint.
    options.compilerArgs.clear()
}

tasks.register<JavaExec>("jmh") {
    group = "verification"
    description = "Runs the JMH benchmarks."
    dependsOn(jmhCompile)
    mainClass.set("org.openjdk.jmh.Main")
    classpath =
        files(layout.buildDirectory.dir("jmh-generated/classes")) +
        files(jmhGeneratedResources) +
        sourceSets.main.get().output +
        sourceSets.main.get().runtimeClasspath +
        jmhGeneratorClasspath
    // Defaults keep a CI smoke run short; override with -Pjmh.args.
    args(
        (project.findProperty("jmh.args") as String? ?: "-f 1 -wi 3 -i 5 -r 1s -w 1s")
            .split(" ")
            .filter { it.isNotBlank() },
    )
}

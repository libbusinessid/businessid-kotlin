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

/** The business version of the ruleset this build compiles in, from rules.lock. */
val rulesVersion: String = rootProject.layout.projectDirectory.file("rules.lock").asFile
    .readLines()
    .first { it.trimStart().startsWith("rules_version") }
    .substringAfter('"')
    .substringBefore('"')

// The toolchain is locked so that a build produces the same bytecode everywhere.
//
// `-Pbusinessid.toolchain=N` moves it, and that is how `scripts/verify.sh`
// covers the far end of the supported range without a second runner. It moves
// the *toolchain*, not the daemon, and the distinction is the whole point:
// detekt and ktlint each embed a Kotlin compiler that refuses a class file
// version newer than the release it was built against, and both run inside the
// daemon. A daemon on JDK 25 stops them before they read a line — which is why
// the CI job that used to carry the range set `java-version: 25` and got a
// daemon on 25 with the tests still running on 17, since this toolchain pinned
// them there. It never once ran the code on the JDK it was named after.
private val toolchainJdk: Int =
    providers.gradleProperty("businessid.toolchain").map(String::toInt).getOrElse(BuildConstants.TOOLCHAIN_JDK)

kotlin {
    jvmToolchain(toolchainJdk)
}

// A run on another toolchain builds into a directory of its own.
//
// Two reasons, and both have a defect behind them. It must not leave its class
// files and its jar where the pinned build's are, because the next thing to read
// them would publish bytecode from a compiler the project does not ship with —
// `test` depends on `jar`, so the jar is rebuilt whether or not anyone asked.
// And `verify.sh` judges a step by the evidence it left: sharing the directory
// would let the pinned run's own results stand in for a step that never ran.
if (toolchainJdk != BuildConstants.TOOLCHAIN_JDK) {
    layout.buildDirectory.set(layout.projectDirectory.dir("build/jdk$toolchainJdk"))
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
    // The README is read by ReadmeTest, so an edit to it has to re-run the
    // tests. Without this the guard passes on a stale result, which is worse
    // than having no guard.
    inputs.file(rootProject.layout.projectDirectory.file("README.md")).withPropertyName("readme")
    systemProperty("businessid.spec.dir", specDir.asFile.absolutePath)
    systemProperty("businessid.project.version", project.version.toString())
    // The published groupId, read by the tests that freeze the coordinates.
    systemProperty("businessid.project.group", project.group.toString())
    // Read from rules.lock rather than repeated in each test: a resync moves it,
    // and a literal in six files is six ways to forget one.
    systemProperty("businessid.rules.version", rulesVersion)
    // TestHygieneTest walks these classes to prove no test method is silently
    // dropped by JUnit for returning a value.
    systemProperty(
        "businessid.test.classes",
        layout.buildDirectory.dir("classes/kotlin/test").get().asFile.absolutePath,
    )
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

// Jazzer runs the fuzz targets in regression mode during an ordinary build, and
// actually fuzzes only here, where JAZZER_FUZZ turns it on.
private val fuzzSourceSet = extensions.getByType<JavaPluginExtension>().sourceSets.named("test")
private val fuzzClasses = fuzzSourceSet.map { it.output.classesDirs }
private val fuzzClasspath = fuzzSourceSet.map { it.runtimeClasspath }

tasks.register<Test>("fuzz") {
    group = "verification"
    description = "Fuzzes the targets under io.libbusinessid.fuzz with Jazzer."
    testClassesDirs = files(fuzzClasses)
    classpath = files(fuzzClasspath)
    filter {
        includeTestsMatching("io.libbusinessid.fuzz.*")
        isFailOnNoMatchingTests = false
    }
    environment("JAZZER_FUZZ", "1")
    // libFuzzer takes over the process, so Jazzer fuzzes one target per JVM and
    // skips the rest. One fork per target is what makes them all run.
    forkEvery = 1
    // The scheduled run fuzzes for longer than the smoke run in CI.
    (project.findProperty("fuzz.seconds") as String?)?.let {
        systemProperty("jazzer.max_duration", it + "s")
    }
    outputs.upToDateWhen { false }
    reports.junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/fuzz"))
    reports.html.outputLocation.set(layout.buildDirectory.dir("reports/tests/fuzz"))
}

// Coverage. Only protoc output is filtered out of the report; that exclusion is
// what `engine.md` section 12.2 allows for generated Protobuf code. Everything
// else stays in, including the rules emitted from the bundle, because the split
// between hand written and emitted is what the gate reads and publishes.
kover {
    currentProject {
        instrumentation {
            // Coverage is measured from the test suite alone. Letting the fuzz
            // task contribute would make the figure depend on whether Jazzer
            // happened to run, and on which inputs it happened to generate.
            disabledForTestTasks.add("fuzz")
        }
    }

    reports {
        filters {
            excludes {
                classes("libbusinessid.*")
            }
        }
    }
}

// Resolves every declared dependency, so a missing or moved artefact fails here
// rather than at a release.
tasks.register("resolveAllDependencies") {
    group = "verification"
    description = "Resolves every resolvable configuration of this project."
    // Resolved at configuration time and carried into the task as file trees, so
    // nothing reaches for the project while the task runs.
    val label = project.path
    val resolvable = configurations
        .filter { it.isCanBeResolved }
        .associate { it.name to files(provider { runCatching { it.files }.getOrDefault(emptySet()) }) }
    doLast {
        var artefacts = 0
        for ((_, tree) in resolvable) artefacts += tree.files.size
        logger.lifecycle("resolved ${resolvable.size} configurations of $label, $artefacts artefacts")
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    parallel = true
}

// Detekt runs inside the Gradle daemon and carries its own Kotlin compiler,
// which refuses a class file version newer than the release it was built
// against — 1.23.8 stops at "25.0.4" on a JDK 25 daemon, and there is no
// launcher to point elsewhere. So the analysers run once, on the JDK this
// project pins, in a CI job of their own; the compiler and the tests still run
// across the whole supported range.
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

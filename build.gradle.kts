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
        "--bundle",
        bundle.asFile.absolutePath,
        "--lock",
        lock.asFile.absolutePath,
        "--out",
        generatedSourceDir.asFile.absolutePath,
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
        "--bundle",
        bundle.asFile.absolutePath,
        "--lock",
        lock.asFile.absolutePath,
        "--out",
        generatedSourceDir.asFile.absolutePath,
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

val ktlint: Configuration by configurations.creating {
    // ktlint-cli publishes a plain and a shadowed variant; the shadowed one
    // carries its own dependencies, which is what a standalone run needs.
    attributes {
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.SHADOWED))
    }
}

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

// ---------------------------------------------------------------------------
// Coverage gate.
//
// `engine.md` section 12.2 draws a line the tooling does not: the thresholds
// cover hand written code — the engine, its primitives, its API, its generator.
// The rules emitted from the bundle are covered by the conformance corpus, and
// their figure measures the corpus rather than the engine: a rule branch nothing
// reaches means no case reaches it, which the report of unused operations says
// better. So it is measured, printed, and never turned into a threshold — an
// irreproachable engine would otherwise fail on a gap in the corpus, and the
// only way back to green would be to lower the number.
// ---------------------------------------------------------------------------

abstract class CoverageGate : DefaultTask() {
    @get:InputFiles
    abstract val reports: ConfigurableFileCollection

    @get:Input
    abstract val minimumLinePercent: Property<Int>

    @get:Input
    abstract val minimumBranchPercent: Property<Int>

    @get:OutputFile
    abstract val summary: RegularFileProperty

    private class Counts {
        var linesCovered = 0L
        var linesMissed = 0L
        var branchesCovered = 0L
        var branchesMissed = 0L

        fun linePercent() = percent(linesCovered, linesMissed)

        fun branchPercent() = percent(branchesCovered, branchesMissed)

        private fun percent(covered: Long, missed: Long) =
            if (covered + missed == 0L) 100.0 else 100.0 * covered / (covered + missed)

        fun describe(what: String) = String.format(
            java.util.Locale.ROOT,
            "%-14s lines %6.2f%% (%d/%d)  branches %6.2f%% (%d/%d)",
            what,
            linePercent(),
            linesCovered,
            linesCovered + linesMissed,
            branchPercent(),
            branchesCovered,
            branchesCovered + branchesMissed,
        )
    }

    @TaskAction
    fun check() {
        val handWritten = Counts()
        val emitted = Counts()
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        // The Kover report names the JaCoCo DTD; nothing is fetched to read it.
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        for (file in reports.files) {
            if (!file.isFile) continue
            val document = factory.newDocumentBuilder().parse(file)
            val packages = document.getElementsByTagName("package")
            for (i in 0 until packages.length) {
                val node = packages.item(i) as org.w3c.dom.Element
                val name = node.getAttribute("name").replace('/', '.')
                val into = if (name.startsWith("io.libbusinessid.generated")) emitted else handWritten
                var child = node.firstChild
                while (child != null) {
                    if (child is org.w3c.dom.Element && child.tagName == "counter") {
                        val covered = child.getAttribute("covered").toLong()
                        val missed = child.getAttribute("missed").toLong()
                        when (child.getAttribute("type")) {
                            "LINE" -> {
                                into.linesCovered += covered
                                into.linesMissed += missed
                            }

                            "BRANCH" -> {
                                into.branchesCovered += covered
                                into.branchesMissed += missed
                            }
                        }
                    }
                    child = child.nextSibling
                }
            }
        }

        val text = buildString {
            appendLine(handWritten.describe("hand written"))
            appendLine(emitted.describe("emitted"))
            appendLine(
                "the emitted figure measures the conformance corpus, not the engine, and is never a threshold",
            )
        }
        summary.get().asFile.writeText(text)
        logger.lifecycle(text.trimEnd())

        val failures = buildList {
            val line = String.format(java.util.Locale.ROOT, "%.2f", handWritten.linePercent())
            val branch = String.format(java.util.Locale.ROOT, "%.2f", handWritten.branchPercent())
            if (handWritten.linePercent() < minimumLinePercent.get()) {
                add("hand written line coverage $line% is below ${minimumLinePercent.get()}%")
            }
            if (handWritten.branchPercent() < minimumBranchPercent.get()) {
                add("hand written branch coverage $branch% is below ${minimumBranchPercent.get()}%")
            }
        }
        if (failures.isNotEmpty()) throw GradleException(failures.joinToString("; "))
    }
}

val coverage = tasks.register<CoverageGate>("coverage") {
    group = "verification"
    description = "Splits coverage between hand written and emitted code, and gates the first."
    dependsOn(":businessid:koverXmlReport", ":generator:koverXmlReport", ":testee:koverXmlReport")
    reports.from(
        project(":businessid").layout.buildDirectory.file("reports/kover/report.xml"),
        project(":generator").layout.buildDirectory.file("reports/kover/report.xml"),
        project(":testee").layout.buildDirectory.file("reports/kover/report.xml"),
    )
    minimumLinePercent.set(95)
    minimumBranchPercent.set(90)
    summary.set(layout.buildDirectory.file("reports/coverage-summary.txt"))
    outputs.upToDateWhen { false }
}

tasks.named("check") {
    dependsOn(coverage)
}

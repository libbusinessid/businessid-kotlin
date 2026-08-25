// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("entid.kotlin-base")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    `java-library`
    `maven-publish`
    signing
}

description = "Offline canonicalization, format and checksum validation of business identifiers."

kotlin {
    // Every public declaration states its visibility and its return type.
    explicitApi()

    // The published library is compiled by the current Kotlin but speaks an
    // older one. A Kotlin compiler reads metadata up to one minor version above
    // its own, so metadata 2.2 is what a consumer on Kotlin 2.1 or later can
    // read — and the built-in Kotlin of the Android Gradle plugin is 2.2 today.
    // Compiling at the newest version instead would make the library unusable on
    // Android, which is half of what this release announces.
    coreLibrariesVersion = libs.versions.kotlinCompatibility.get()

    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}

// The published library has no dependency: it decodes nothing, downloads
// nothing and reads no resource. A `dependencies` block adding anything here
// is a defect, and PackagingTest fails the build when the jar disagrees.
dependencies {
    testImplementation(libs.kotest.property)
    testImplementation(libs.jazzer.junit)
}

java {
    withSourcesJar()
}

val dokkaJavadocJar = tasks.register<Jar>("dokkaJavadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaGeneratePublicationHtml"))
}

apiValidation {
    // Emitted sources declare nothing public: the API dump covers hand written
    // declarations only, which is exactly what SemVer freezes.
    nonPublicMarkers.add("org.entid.internal.InternalEntIdApi")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(dokkaJavadocJar)
            pom {
                name.set("entid")
                description.set(project.description)
                url.set("https://github.com/entid-org/entid-kotlin")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("entid")
                        name.set("The EntID Authors")
                    }
                }
                scm {
                    url.set("https://github.com/entid-org/entid-kotlin")
                    connection.set("scm:git:https://github.com/entid-org/entid-kotlin.git")
                    developerConnection.set("scm:git:ssh://git@github.com/entid-org/entid-kotlin.git")
                }
            }
        }
    }
    repositories {
        maven {
            name = "localStaging"
            url = layout.buildDirectory.dir("staging-repository").get().asFile.toURI()
        }
    }
}

signing {
    // A snapshot build needs no secret. A release build refuses to produce an
    // unsigned artefact, and that is the point: keying this off the presence of
    // `SIGNING_KEY` instead — which is what it did — makes a missing secret look
    // like a successful release. Gradle would skip the signing task, the bundle
    // would carry no `.asc`, and the first thing to notice would be the Central
    // Portal, after the tag was pushed.
    isRequired = !version.toString().endsWith("-SNAPSHOT")
    useInMemoryPgpKeys(
        providers.environmentVariable("SIGNING_KEY").orNull,
        providers.environmentVariable("SIGNING_PASSWORD").orNull,
    )
    sign(publishing.publications["maven"])
}

tasks.test {
    // The packaging test opens what would actually be published.
    val jar = tasks.jar.flatMap { it.archiveFile }
    val pom = layout.buildDirectory.file("publications/maven/pom-default.xml")
    dependsOn(tasks.jar, tasks.named("generatePomFileForMavenPublication"))
    inputs.file(jar).withPropertyName("publishedJar")
    systemProperty("entid.jar", jar.get().asFile.absolutePath)
    systemProperty("entid.pom", pom.get().asFile.absolutePath)
}

// Emitted sources are machine written and never edited, so they are linted at
// emission time by the generator rather than judged here. Every one of them
// carries a header saying so.
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude("**/org/entid/generated/**")
}

// ---------------------------------------------------------------------------
// Dependency audit.
//
// The published library declares nothing but the Kotlin standard library, and
// that is a property worth failing a build over: a dependency added here is one
// every caller inherits, and section 10.4 of engine.md forbids an HTTP one
// outright.
// ---------------------------------------------------------------------------

tasks.register("auditPublishedDependencies") {
    group = "verification"
    description = "Fails when the published library declares a dependency beyond the Kotlin standard library."
    // Read from the project rather than repeated: the coordinates moved once
    // already, and a literal here would have turned the library itself into a
    // dependency of itself and failed the audit for the wrong reason.
    val self = "${project.group}:${project.name}"
    val resolved = configurations.named("runtimeClasspath").map { classpath ->
        classpath.incoming.resolutionResult.allComponents
            .mapNotNull { it.moduleVersion?.module?.toString() }
            // The library itself is the root of the graph, not a dependency.
            .filterNot { it == self }
            .distinct()
            .sorted()
    }
    doLast {
        val expected = listOf("org.jetbrains.kotlin:kotlin-stdlib", "org.jetbrains:annotations")
        val actual = resolved.get()
        check(actual == expected) {
            "the published library would bring\n  ${actual.joinToString("\n  ")}\nwhere it should bring\n  " +
                expected.joinToString("\n  ")
        }
        logger.lifecycle("the published library brings: ${actual.joinToString(", ")}")
    }
}

tasks.named("check") {
    dependsOn("auditPublishedDependencies")
}

// ---------------------------------------------------------------------------
// Mutation testing.
//
// Pitest is driven from its command line rather than through its Gradle plugin:
// the plugin reads `reporting.baseDir`, which Gradle 9 removed, so applying it
// fails before it runs anything.
//
// It is aimed where engine-kotlin.md aims it: the runtime primitives and the
// pipeline, where an off-by-one in a comparison or a flipped bound is a wrong
// verdict rather than a compile error. The emitted rules are left out —
// mutating a table produced from the ruleset measures the corpus again.
// ---------------------------------------------------------------------------

val pitestClasspath: Configuration = configurations.create("pitestClasspath")

dependencies {
    pitestClasspath(libs.pitest.command.line)
    pitestClasspath(libs.pitest.entry)
    pitestClasspath(libs.pitest.junit5)
    pitestClasspath(libs.junit.jupiter)
    pitestClasspath(libs.junit.platform.launcher)
}

tasks.register<JavaExec>("mutationTest") {
    group = "verification"
    description = "Runs Pitest over the runtime primitives and the pipeline."
    // The suite has to be green under Pitest's own JVM, which means it gets the
    // same system properties the test task sets.
    dependsOn(tasks.named("testClasses"), tasks.jar, tasks.named("generatePomFileForMavenPublication"))
    mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
    val main = sourceSets.main.get()
    val test = sourceSets.test.get()
    classpath = pitestClasspath + test.runtimeClasspath
    val reportDir = layout.buildDirectory.dir("reports/pitest").get().asFile
    val sources = main.kotlin.srcDirs.filter { it.isDirectory }.joinToString(",") { it.absolutePath }
    val classes = main.output.classesDirs.joinToString(",") { it.absolutePath }
    val runtime = test.runtimeClasspath.files.map { it.absolutePath }
    // Read off `tasks.test` rather than restated. Pitest runs the suite in a JVM
    // of its own, and a test that throws for a missing property is reported as
    // "the suite is not green" with no hint of which property — which is exactly
    // what a hand written copy of this list produced when the test task gained
    // `entid.project.group` and the copy did not. There is one definition
    // of what the suite needs, and it is the task that already runs it.
    val jvmProperties = tasks.test.get().systemProperties
        .toSortedMap()
        .map { (key, value) -> "-D$key=$value" }
        .joinToString(",")
    args(
        "--reportDir", reportDir.absolutePath,
        "--targetClasses", "org.entid.runtime.*,org.entid.internal.*",
        // The test classes share the package of the primitives they exercise,
        // and mutating a test measures nothing.
        "--excludedClasses", "org.entid.generated.*,*Test,*Test$*,org.entid.runtime.ViewsKt",
        "--targetTests", "org.entid.*",
        "--excludedTestClasses", "org.entid.fuzz.*,org.entid.coverage.*",
        "--sourceDirs", sources,
        "--classPath", (runtime + classes).joinToString(","),
        "--mutators", "STRONGER",
        "--outputFormats", "HTML,XML",
        "--timestampedReports", "false",
        "--threads", Runtime.getRuntime().availableProcessors().toString(),
        "--verbosity", "QUIET",
        "--jvmArgs", jvmProperties,
    )
}

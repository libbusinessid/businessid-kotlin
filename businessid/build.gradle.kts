// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("businessid.kotlin-base")
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
    nonPublicMarkers.add("io.libbusinessid.internal.InternalBusinessIdApi")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(dokkaJavadocJar)
            pom {
                name.set("businessid")
                description.set(project.description)
                url.set("https://github.com/libbusinessid/businessid-kotlin")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("libbusinessid")
                        name.set("The LibBusinessID Authors")
                    }
                }
                scm {
                    url.set("https://github.com/libbusinessid/businessid-kotlin")
                    connection.set("scm:git:https://github.com/libbusinessid/businessid-kotlin.git")
                    developerConnection.set("scm:git:ssh://git@github.com/libbusinessid/businessid-kotlin.git")
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
    // Only signs when a key is configured, so a local build needs no secret.
    isRequired = providers.environmentVariable("SIGNING_KEY").isPresent
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
    systemProperty("businessid.jar", jar.get().asFile.absolutePath)
    systemProperty("businessid.pom", pom.get().asFile.absolutePath)
}

// Emitted sources are machine written and never edited, so they are linted at
// emission time by the generator rather than judged here. Every one of them
// carries a header saying so.
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude("**/io/libbusinessid/generated/**")
}

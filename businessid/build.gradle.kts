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

val dokkaJavadocJar by tasks.registering(Jar::class) {
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

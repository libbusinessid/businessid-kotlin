// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.jar.JarFile

/**
 * What the published artefact actually contains.
 *
 * This is the test that tells a generated engine from an interpreter. A
 * misplaced Gradle dependency, a stray resource, a `.binpb` copied into the
 * source set: each of them would betray the split silently, and each of them is
 * a failure here.
 */
class PackagingTest {
    private val jar = JarFile(File(requireNotNull(System.getProperty("businessid.jar"))))
    private val entries = jar.entries().asSequence().map { it.name }.toList()

    @Test
    fun `the jar carries no ruleset`() {
        val bundles = entries.filter { it.endsWith(".binpb") || it.endsWith(".jsonl") }
        assertEquals(emptyList<String>(), bundles, "the library decodes nothing, so it embeds nothing to decode")
    }

    @Test
    fun `the jar carries no Protobuf class`() {
        val protobuf = entries.filter {
            it.startsWith("com/google/protobuf/") ||
                it.startsWith("libbusinessid/ir/") ||
                it.startsWith("libbusinessid/conformance/") ||
                it.startsWith("libbusinessid/testee/")
        }
        assertEquals(emptyList<String>(), protobuf, "Protobuf lives in the generator, never in the library")
    }

    @Test
    fun `every class of the jar belongs to this library`() {
        val foreign = entries.filter { it.endsWith(".class") && !it.startsWith("io/libbusinessid/") }
        assertEquals(emptyList<String>(), foreign)
    }

    @Test
    fun `the jar carries no resource at all beyond its manifest`() {
        val resources = entries.filter { !it.endsWith(".class") && !it.endsWith("/") && it != "META-INF/MANIFEST.MF" }
        assertEquals(
            emptyList<String>(),
            resources.filterNot { it.startsWith("META-INF/") && it.endsWith(".kotlin_module") },
            "the library reads no resource, so it ships none",
        )
    }

    @Test
    fun `the emitted rules are in the jar`() {
        assertTrue(
            entries.any { it == "io/libbusinessid/generated/Ruleset.class" },
            "the rules ship as code: ${entries.take(20)}",
        )
    }

    @Test
    fun `the published POM carries the coordinates Maven Central freezes`() {
        val pom = File(requireNotNull(System.getProperty("businessid.pom"))).readText()
        // A groupId cannot be changed after a first publication: every consumer
        // names it, and the old coordinates keep resolving to the old artefact
        // for ever. So it is frozen by a test rather than left to a property
        // file, and `io.libbusinessid` is not it — the Central Portal verifies a
        // namespace against a domain or a GitHub account, and nobody here owns
        // `libbusinessid.io`.
        assertTrue(
            "<groupId>io.github.libbusinessid</groupId>" in pom,
            "the POM declares another groupId than the verified namespace",
        )
        assertTrue("<artifactId>businessid</artifactId>" in pom, "the POM declares another artifactId")
    }

    @Test
    fun `the published POM declares nothing but the Kotlin standard library`() {
        val pom = File(requireNotNull(System.getProperty("businessid.pom"))).readText()
        val dependencies = Regex("<artifactId>([^<]+)</artifactId>\\s*<version>")
            .findAll(pom.substringAfter("<dependencies>", "").substringBefore("</dependencies>"))
            .map { it.groupValues[1] }
            .toList()
        assertEquals(
            listOf("kotlin-stdlib"),
            dependencies,
            "a dependency here is one every caller inherits",
        )
    }

    @Test
    fun `no public type exposes a registry`() {
        // Section 10 of engine.md defers remote lookup, and a public type is a
        // commitment SemVer freezes. Nothing named after one may exist yet.
        val registry = entries.filter { it.contains("Registry", ignoreCase = true) }
        assertEquals(emptyList<String>(), registry)
    }

    @Test
    fun `no class of the jar reaches a network or a file system`() {
        val forbidden = listOf(
            "java/net/URL",
            "java/net/HttpURLConnection",
            "java/net/http/HttpClient",
            "java/io/FileInputStream",
            "java/nio/file/Files",
            "java/lang/ClassLoader.getResource",
        )
        val constantPools = entries.filter { it.endsWith(".class") }.map { name ->
            jar.getInputStream(jar.getEntry(name)).use { it.readBytes() }.decodeToString()
        }
        for (symbol in forbidden) {
            assertTrue(
                constantPools.none { symbol in it },
                "a class of the published jar references $symbol",
            )
        }
    }
}

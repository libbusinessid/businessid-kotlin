// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The prohibitions of `engine.md` section 19 and of `engine-kotlin.md`, read off
 * the sources rather than trusted.
 *
 * Each of these is something a reviewer would have to notice, and a compiler
 * never will. They are cheap to check and expensive to lose.
 */
class SourceConstraintsTest {
    private val sources: List<File> by lazy {
        val root = File(System.getProperty("entid.spec.dir")).parentFile
            .resolve("entid/src/main/kotlin")
        assertTrue(root.isDirectory, "no library source at $root")
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private val handWritten: List<File>
        get() = sources.filterNot { "generated" in it.path }

    private fun findings(files: List<File>, needle: Regex): List<String> = files.flatMap { file ->
        file.readLines().withIndex()
            .filter { (_, line) -> needle.containsMatchIn(line) }
            .map { (i, line) -> "${file.name}:${i + 1}: ${line.trim()}" }
    }

    @Test
    fun `there are sources to check`() {
        assertTrue(sources.size >= 15, "found ${sources.size} sources")
        assertTrue(handWritten.size >= 10, "found ${handWritten.size} hand written sources")
    }

    @Test
    fun `nothing in the library asserts non nullity with two exclamation marks`() {
        assertEquals(emptyList<String>(), findings(sources, Regex("""[^!=<>]!!""")))
    }

    @Test
    fun `no rule is expressed as a regular expression`() {
        // engine.md section 19 forbids a generic regular expression for
        // interpreting the IR, and engine-kotlin.md forbids depending on
        // java.util.regex for the rules.
        assertEquals(
            emptyList<String>(),
            findings(sources, Regex("""\bRegex\b|java\.util\.regex|\bPattern\.compile\b|\.toRegex\(""")),
        )
    }

    @Test
    fun `no case mapping consults a locale`() {
        assertEquals(
            emptyList<String>(),
            findings(sources, Regex("""\.uppercase\(|\.lowercase\(|toUpperCase|toLowerCase""")),
        )
    }

    @Test
    fun `nothing reaches a network, a file or a resource`() {
        assertEquals(
            emptyList<String>(),
            findings(
                sources,
                Regex("""java\.net\.|java\.nio\.file|\bFile\(|getResource|openStream|HttpClient"""),
            ),
        )
    }

    @Test
    fun `no validation operation is a coroutine`() {
        // Local validation stays synchronous, permanently. A suspend function on
        // this side would be the one change that transforms every caller.
        assertEquals(
            emptyList<String>(),
            findings(sources, Regex("""\bsuspend\b|kotlinx\.coroutines""")),
        )
    }

    @Test
    fun `no registry type exists, not even an experimental one`() {
        assertEquals(emptyList<String>(), findings(sources, Regex("""(?i)\bregistryprovider\b""")))
        // The reason code is reserved and must stay, without anything using it.
        val reserved = findings(sources, Regex("""REGISTRY_NOT_CONFIGURED"""))
        assertEquals(1, reserved.size, "the reserved reason code should be declared exactly once: $reserved")
    }

    @Test
    fun `no Android class reaches the core`() {
        assertEquals(emptyList<String>(), findings(sources, Regex("""\bandroid\.|androidx\.""")))
    }

    @Test
    fun `no Protobuf type reaches the library`() {
        // The emitted files name the ruleset they came from in their header,
        // which is provenance rather than a dependency; nothing may import a
        // Protobuf type or read a ruleset.
        assertEquals(
            emptyList<String>(),
            findings(sources, Regex("""com\.google\.protobuf|libbusinessid\.ir\.v1""")),
        )
        assertEquals(
            emptyList<String>(),
            findings(sources, Regex("""\.binpb""")).filterNot { "Generated from entid-rules.binpb" in it },
        )
    }

    @Test
    fun `every hand written file carries the licence header`() {
        val missing = handWritten.filterNot { it.readText().startsWith("// Copyright The EntID Authors.") }
        assertEquals(emptyList<File>(), missing)
    }

    @Test
    fun `every emitted file says it is emitted`() {
        val emitted = sources.filter { "generated" in it.path }
        assertTrue(emitted.isNotEmpty())
        val silent = emitted.filterNot { "Do not edit by hand" in it.readText().take(200) }
        assertEquals(emptyList<File>(), silent)
    }
}

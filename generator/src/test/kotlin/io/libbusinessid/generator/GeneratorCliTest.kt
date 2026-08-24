// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.assertFailsWith

/**
 * The generator as the build invokes it.
 *
 * The entry point returns an exit code rather than ending the process, which is
 * what lets the refusal paths be exercised here instead of being taken on trust.
 */
class GeneratorCliTest {
    private class Run(val code: Int, val out: String, val err: String)

    private fun run(vararg argv: String): Run {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val code = run(
            arrayOf(*argv),
            PrintStream(out, true, "UTF-8"),
            PrintStream(err, true, "UTF-8"),
        )
        return Run(code, out.toString("UTF-8"), err.toString("UTF-8"))
    }

    private fun lockFile(dir: File, digest: String): File = File(dir, "rules.lock").apply {
        writeText(
            """
                # a comment, and a line without an equals sign
                rules_version = "2026.08.26"
                rules_sha256 = "$digest"
            """.trimIndent(),
        )
    }

    @Test
    fun `it emits from the pinned ruleset`(@TempDir dir: File) {
        val result = run(
            "--bundle",
            SpecFiles.file("businessid-rules.binpb").path,
            "--out",
            dir.path,
        )
        assertEquals(0, result.code, result.err)
        assertTrue("emitted 5 files" in result.out, result.out)
        assertEquals(
            listOf("Canonicalizers.kt", "Checksums.kt", "Constants.kt", "Formats.kt", "Ruleset.kt"),
            dir.list()?.sorted(),
        )
    }

    @Test
    fun `emission is deterministic, byte for byte`(@TempDir first: File, @TempDir second: File) {
        val bundle = SpecFiles.file("businessid-rules.binpb").path
        run("--bundle", bundle, "--out", first.path)
        run("--bundle", bundle, "--out", second.path)
        for (name in requireNotNull(first.list())) {
            assertEquals(File(first, name).readText(), File(second, name).readText(), name)
        }
    }

    @Test
    fun `the check mode accepts what the same ruleset produced`(@TempDir dir: File) {
        val bundle = SpecFiles.file("businessid-rules.binpb").path
        run("--bundle", bundle, "--out", dir.path)
        val result = run("--bundle", bundle, "--out", dir.path, "--check")
        assertEquals(0, result.code, result.err)
        assertTrue("match the pinned ruleset" in result.out, result.out)
    }

    @Test
    fun `the check mode refuses a stale file`(@TempDir dir: File) {
        val bundle = SpecFiles.file("businessid-rules.binpb").path
        run("--bundle", bundle, "--out", dir.path)
        File(dir, "Ruleset.kt").appendText("\n// edited by hand\n")
        val result = run("--bundle", bundle, "--out", dir.path, "--check")
        assertEquals(1, result.code)
        assertTrue("Ruleset.kt" in result.err, result.err)
        assertTrue("generateEngine" in result.err, result.err)
    }

    @Test
    fun `the check mode refuses a missing file`(@TempDir dir: File) {
        val bundle = SpecFiles.file("businessid-rules.binpb").path
        run("--bundle", bundle, "--out", dir.path)
        File(dir, "Formats.kt").delete()
        assertEquals(1, run("--bundle", bundle, "--out", dir.path, "--check").code)
    }

    @Test
    fun `the check mode refuses a file the ruleset no longer produces`(@TempDir dir: File) {
        val bundle = SpecFiles.file("businessid-rules.binpb").path
        run("--bundle", bundle, "--out", dir.path)
        File(dir, "Leftover.kt").writeText("// from an older ruleset\n")
        val result = run("--bundle", bundle, "--out", dir.path, "--check")
        assertEquals(1, result.code)
        assertTrue("Leftover.kt" in result.err, result.err)
    }

    @Test
    fun `emission removes a file the ruleset no longer produces`(@TempDir dir: File) {
        val bundle = SpecFiles.file("businessid-rules.binpb").path
        run("--bundle", bundle, "--out", dir.path)
        File(dir, "Leftover.kt").writeText("// from an older ruleset\n")
        run("--bundle", bundle, "--out", dir.path)
        assertTrue(!File(dir, "Leftover.kt").exists())
    }

    @Test
    fun `it refuses to generate from a hostile ruleset, naming the check`(@TempDir dir: File) {
        val fixture = File(dir, "hostile.binpb")
        fixture.writeBytes(SpecFiles.loaderCases.first { it.id == "loader-call-cycle-014" }.rulesPayload.toByteArray())
        val result = run("--bundle", fixture.path, "--out", File(dir, "out").path)
        assertEquals(1, result.code)
        assertTrue("invalid_ruleset at check 24" in result.err, result.err)
        assertTrue(!File(dir, "out").exists(), "nothing is emitted from a refused ruleset")
    }

    @Test
    fun `it verifies the digest rules dot lock declares`(@TempDir dir: File) {
        val bundle = SpecFiles.file("businessid-rules.binpb")
        val lock = lockFile(dir, sha256Hex(bundle.readBytes()))
        val result = run("--bundle", bundle.path, "--lock", lock.path, "--out", File(dir, "out").path)
        assertEquals(0, result.code, result.err)
    }

    @Test
    fun `it refuses a ruleset the lock does not describe`(@TempDir dir: File) {
        val bundle = SpecFiles.file("businessid-rules.binpb")
        val lock = lockFile(dir, "0".repeat(64))
        val result = run("--bundle", bundle.path, "--lock", lock.path, "--out", File(dir, "out").path)
        assertEquals(2, result.code)
        assertTrue("does not match" in result.err, result.err)
    }

    @Test
    fun `it refuses a lock that declares no digest`(@TempDir dir: File) {
        val lock = File(dir, "rules.lock").apply { writeText("rules_version = \"2026.08.26\"\n") }
        val bundle = SpecFiles.file("businessid-rules.binpb")
        val result = run("--bundle", bundle.path, "--lock", lock.path, "--out", File(dir, "out").path)
        assertEquals(2, result.code)
        assertTrue("rules_sha256" in result.err, result.err)
    }

    @Test
    fun `a usage error is reported rather than guessed`() {
        assertEquals(2, run("--out", "x").code)
        assertEquals(2, run("--bundle", "x").code)
        assertEquals(2, run("--bundle").code)
        assertEquals(2, run("--nonsense").code)
        assertTrue("--bundle is required" in run("--out", "x").err)
        assertTrue("unknown argument" in run("--nonsense").err)
    }

    @Test
    fun `arguments are parsed into the shape the build passes`() {
        val parsed = parseArguments(arrayOf("--bundle", "b", "--lock", "l", "--out", "o", "--check"))
        assertEquals("b", parsed.bundle.path)
        assertEquals("l", parsed.lock?.path)
        assertEquals("o", parsed.out.path)
        assertTrue(parsed.check)
        assertEquals(null, parseArguments(arrayOf("--bundle", "b", "--out", "o")).lock)
    }

    @Test
    fun `the digest helper spells a SHA-256 in lowercase hexadecimal`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha256Hex(ByteArray(0)),
        )
    }

    @Test
    fun `verifyLock throws for a caller that wants the failure rather than an exit code`() {
        val failure = assertFailsWith<UsageException> {
            verifyLock(SpecFiles.file("../rules.lock"), ByteArray(1))
        }
        assertTrue("does not match" in failure.message)
    }

    @Test
    fun `the gate answers for the conformance testee`() {
        assertEquals(null, RulesetGate.inspect(SpecFiles.rulesBundle))
        val refusal = requireNotNull(RulesetGate.inspect(byteArrayOf(0x08, 0x02)))
        assertEquals("incompatible_ruleset", refusal.errorKind)
        assertEquals(3, refusal.check)
        assertTrue("format_version" in refusal.detail)
    }
}

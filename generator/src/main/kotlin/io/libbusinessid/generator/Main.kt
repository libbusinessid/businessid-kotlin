// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import java.io.File
import java.io.PrintStream
import java.security.MessageDigest
import java.util.Locale
import kotlin.system.exitProcess

/**
 * The generator: reads the ruleset from a path, runs the twenty-five load
 * checks, and emits Kotlin.
 *
 * The ruleset is a build input, not a resource: nothing about it reaches the
 * published library, which holds only the emitted code, the primitives it calls
 * and the public API.
 */
class Arguments(
    val bundle: File,
    val lock: File?,
    val out: File,
    val check: Boolean,
)

/** Thrown for a usage error, so a caller can report it rather than exit. */
class UsageException(override val message: String) : Exception(message)

fun parseArguments(argv: Array<String>): Arguments {
    var bundle: String? = null
    var lock: String? = null
    var out: String? = null
    var check = false
    var i = 0
    while (i < argv.size) {
        fun next(): String {
            i++
            if (i >= argv.size) throw UsageException("${argv[i - 1]} needs a value")
            return argv[i]
        }
        when (argv[i]) {
            "--bundle" -> bundle = next()
            "--lock" -> lock = next()
            "--out" -> out = next()
            "--check" -> check = true
            else -> throw UsageException("unknown argument ${argv[i]}")
        }
        i++
    }
    return Arguments(
        bundle = File(bundle ?: throw UsageException("--bundle is required")),
        lock = lock?.let { File(it) },
        out = File(out ?: throw UsageException("--out is required")),
        check = check,
    )
}

internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { String.format(Locale.ROOT, "%02x", it) }

/**
 * Compares the ruleset against the digest `rules.lock` declares.
 *
 * The engine verifies the digest of the file it received and never re-serialises
 * a decoded message to recompute it: Protobuf is not a canonical serialisation.
 */
fun verifyLock(lock: File, bundleBytes: ByteArray) {
    val declared = lock.readLines()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || "=" !in trimmed) {
                null
            } else {
                val (k, v) = trimmed.split("=", limit = 2)
                k.trim() to v.trim().trim('"')
            }
        }.toMap()
    val expected = declared["rules_sha256"]
        ?: throw UsageException("${lock.name} declares no rules_sha256")
    val actual = sha256Hex(bundleBytes)
    if (expected != actual) {
        throw UsageException("the ruleset digest $actual does not match the $expected that ${lock.name} declares")
    }
}

/**
 * Runs the generator and returns the exit code, so that everything above this
 * line is reachable from a test without ending the process.
 */
@Suppress("ReturnCount")
fun run(argv: Array<String>, out: PrintStream, err: PrintStream): Int {
    val args =
        try {
            parseArguments(argv)
        } catch (e: UsageException) {
            err.println("usage: ${e.message}")
            return 2
        }

    val bytes = args.bundle.readBytes()
    args.lock?.let {
        try {
            verifyLock(it, bytes)
        } catch (e: UsageException) {
            err.println(e.message)
            return 2
        }
    }

    val loaded =
        try {
            Loader.load(bytes)
        } catch (e: RulesetException) {
            err.println("refusing to generate: ${e.errorKind.wireName} at check ${e.check}: ${e.message}")
            return 1
        }

    val emitted = Emitter(loaded).emit()
    if (args.check) return verifyEmitted(emitted, args.out, out, err)

    args.out.mkdirs()
    for (name in (args.out.listFiles()?.map { it.name } ?: emptyList()) - emitted.keys) {
        File(args.out, name).delete()
    }
    for ((name, content) in emitted) File(args.out, name).writeText(content)
    out.println("emitted ${emitted.size} files from rules ${loaded.rulesVersion} into ${args.out}")
    return 0
}

private fun verifyEmitted(
    emitted: Map<String, String>,
    target: File,
    out: PrintStream,
    err: PrintStream,
): Int {
    val differences = emitted.filter { (name, content) ->
        val file = File(target, name)
        !file.isFile || file.readText() != content
    }
    val stale = (target.listFiles()?.map { it.name }?.toSet() ?: emptySet()) - emitted.keys
    if (differences.isNotEmpty() || stale.isNotEmpty()) {
        err.println(
            "the committed sources are stale: " +
                (differences.keys + stale).sorted().joinToString(", ") +
                " — run ./gradlew generateEngine",
        )
        return 1
    }
    out.println("generated sources match the pinned ruleset (${emitted.size} files)")
    return 0
}

fun main(argv: Array<String>) {
    exitProcess(run(argv, System.out, System.err))
}

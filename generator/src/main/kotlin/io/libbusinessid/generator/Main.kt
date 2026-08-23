// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.system.exitProcess

/**
 * The generator: reads the bundle from a path, runs the twenty-five load
 * checks, and emits Kotlin.
 *
 * The bundle is a build input, not a resource: nothing about it reaches the
 * published library, which holds only the emitted code, the primitives it calls
 * and the public API.
 */
private class Arguments(
    val bundle: File,
    val lock: File?,
    val out: File,
    val check: Boolean,
)

private fun parse(argv: Array<String>): Arguments {
    var bundle: String? = null
    var lock: String? = null
    var out: String? = null
    var check = false
    var i = 0
    while (i < argv.size) {
        when (argv[i]) {
            "--bundle" -> bundle = argv[++i]
            "--lock" -> lock = argv[++i]
            "--out" -> out = argv[++i]
            "--check" -> check = true
            else -> error("unknown argument ${argv[i]}")
        }
        i++
    }
    return Arguments(
        bundle = File(requireNotNull(bundle) { "--bundle is required" }),
        lock = lock?.let { File(it) },
        out = File(requireNotNull(out) { "--out is required" }),
        check = check,
    )
}

internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { String.format(Locale.ROOT, "%02x", it) }

@Suppress("ReturnCount")
private fun verifyLock(lock: File, bundleBytes: ByteArray) {
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
        ?: error("rules.lock declares no rules_sha256")
    val actual = sha256Hex(bundleBytes)
    if (expected != actual) {
        error("the bundle digest $actual does not match the $expected that rules.lock declares")
    }
}

fun main(argv: Array<String>) {
    val args = parse(argv)
    val bytes = args.bundle.readBytes()
    args.lock?.let { verifyLock(it, bytes) }

    val loaded =
        try {
            Loader.load(bytes)
        } catch (e: RulesetException) {
            System.err.println("refusing to generate: ${e.errorKind.wireName} at check ${e.check}: ${e.message}")
            exitProcess(1)
        }

    val emitted = Emitter(loaded).emit()
    if (args.check) {
        val differences = emitted.filter { (name, content) ->
            val file = File(args.out, name)
            !file.isFile || file.readText() != content
        }
        val stale = (args.out.listFiles()?.map { it.name }?.toSet() ?: emptySet()) - emitted.keys
        if (differences.isNotEmpty() || stale.isNotEmpty()) {
            System.err.println(
                "the committed sources are stale: " +
                    (differences.keys + stale).sorted().joinToString(", ") +
                    " — run ./gradlew generateEngine",
            )
            exitProcess(1)
        }
        println("generated sources match the pinned bundle (${emitted.size} files)")
        return
    }

    args.out.mkdirs()
    for (name in (args.out.listFiles()?.map { it.name } ?: emptyList()) - emitted.keys) {
        File(args.out, name).delete()
    }
    for ((name, content) in emitted) File(args.out, name).writeText(content)
    println("emitted ${emitted.size} files from rules ${loaded.rulesVersion} into ${args.out}")
}

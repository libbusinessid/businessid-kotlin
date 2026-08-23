// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

/**
 * A bounded scan of the Protobuf wire encoding, against a static description of
 * the schema.
 *
 * It exists because check 5 needs to see unknown fields at any depth, and the
 * lite runtime this generator decodes with preserves them but exposes no way to
 * read them. `engine.md` section 7.3 sanctions exactly this: a bounded wire
 * pre-scan against the descriptors when the runtime does not expose them.
 *
 * The scan answers two more questions no Protobuf runtime surfaces at all: a
 * singular field encoded twice, and two branches of the same `oneof` present in
 * the same message. `ir.md` section 10 calls such an encoding not a valid
 * bundle.
 *
 * Nothing here resolves an opcode, an enum value or a capability id: check 2
 * stays at the wire level so a bundle built against a later version is reported
 * as a version gap and not as a forgery.
 */
internal object Wire {
    const val VARINT: Int = 0
    const val FIXED64: Int = 1
    const val LENGTH_DELIMITED: Int = 2
    const val START_GROUP: Int = 3
    const val END_GROUP: Int = 4
    const val FIXED32: Int = 5

    /** Nesting bound, well above the six levels the schema actually reaches. */
    private const val MAX_DEPTH = 64

    /** What a scan found. */
    sealed interface Finding {
        /** A field number the schema does not declare, at [path]. */
        data class UnknownField(val path: String, val number: Int) : Finding

        /** A field the schema declares as singular, encoded more than once. */
        data class RepeatedSingular(val path: String, val number: Int) : Finding

        /** Two branches of the same `oneof` in one message. */
        data class TwoOneofBranches(val path: String, val first: Int, val second: Int) : Finding

        /** The bytes do not parse. */
        data class Malformed(val path: String, val detail: String) : Finding
    }

    /**
     * Scans [bytes] as an instance of [type] and returns every finding, in the
     * order the encoding presents them.
     */
    fun scan(bytes: ByteArray, type: Descriptors.Message): List<Finding> {
        val findings = mutableListOf<Finding>()
        scanMessage(bytes, 0, bytes.size, type, type.name, 0, findings)
        return findings
    }

    @Suppress("LongParameterList", "CyclomaticComplexMethod", "NestedBlockDepth", "LongMethod", "ReturnCount")
    private fun scanMessage(
        b: ByteArray,
        from: Int,
        to: Int,
        type: Descriptors.Message,
        path: String,
        depth: Int,
        out: MutableList<Finding>,
    ) {
        if (depth > MAX_DEPTH) {
            out += Finding.Malformed(path, "nesting deeper than $MAX_DEPTH")
            return
        }
        var i = from
        val seen = HashSet<Int>()
        var oneofTaken = -1
        while (i < to) {
            val keyRead = readVarint(b, i, to) ?: run {
                out += Finding.Malformed(path, "truncated field key")
                return
            }
            i = keyRead.next
            val number = (keyRead.value ushr 3).toInt()
            val wireType = (keyRead.value and 7L).toInt()
            if (number == 0) {
                out += Finding.Malformed(path, "field number 0")
                return
            }

            val field = type.fields[number]
            if (field == null) {
                out += Finding.UnknownField(path, number)
            } else {
                if (!field.repeated && !seen.add(number)) {
                    out += Finding.RepeatedSingular(path, number)
                }
                if (field.oneof != null) {
                    if (oneofTaken >= 0 && oneofTaken != number) {
                        out += Finding.TwoOneofBranches(path, oneofTaken, number)
                    }
                    oneofTaken = number
                }
            }

            when (wireType) {
                VARINT -> {
                    val v = readVarint(b, i, to) ?: run {
                        out += Finding.Malformed(path, "truncated varint")
                        return
                    }
                    i = v.next
                }
                FIXED64 -> {
                    if (i + 8 > to) {
                        out += Finding.Malformed(path, "truncated fixed64")
                        return
                    }
                    i += 8
                }
                FIXED32 -> {
                    if (i + 4 > to) {
                        out += Finding.Malformed(path, "truncated fixed32")
                        return
                    }
                    i += 4
                }
                LENGTH_DELIMITED -> {
                    val len = readVarint(b, i, to) ?: run {
                        out += Finding.Malformed(path, "truncated length")
                        return
                    }
                    i = len.next
                    val size = len.value
                    if (size < 0 || i + size > to) {
                        out += Finding.Malformed(path, "length runs past the message")
                        return
                    }
                    val end = i + size.toInt()
                    val nested = field?.message
                    if (nested != null) {
                        scanMessage(b, i, end, nested, "$path.${field.name}", depth + 1, out)
                    }
                    i = end
                }
                START_GROUP, END_GROUP -> {
                    out += Finding.Malformed(path, "group wire type $wireType")
                    return
                }
                else -> {
                    out += Finding.Malformed(path, "wire type $wireType")
                    return
                }
            }
        }
    }

    private class Varint(val value: Long, val next: Int)

    private fun readVarint(b: ByteArray, from: Int, to: Int): Varint? {
        var result = 0L
        var shift = 0
        var i = from
        while (i < to) {
            val byte = b[i].toInt()
            i++
            result = result or ((byte.toLong() and 0x7F) shl shift)
            if (byte and 0x80 == 0) return Varint(result, i)
            shift += 7
            if (shift > 63) return null
        }
        return null
    }
}

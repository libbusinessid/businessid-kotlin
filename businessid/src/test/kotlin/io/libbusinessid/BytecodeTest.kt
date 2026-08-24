// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.DataInputStream
import java.io.File
import java.util.jar.JarFile

/**
 * What the published bytecode actually claims.
 *
 * The build says the target is Java 11 and the Kotlin metadata is 2.2. Both are
 * promises a consumer's toolchain either accepts or refuses at its own build,
 * long after this one, so they are read back out of the artefact here.
 */
class BytecodeTest {
    private val jar = JarFile(File(requireNotNull(System.getProperty("businessid.jar"))))

    /** Java 11 is class file major version 55. */
    private val expectedMajor = 55

    @Test
    fun `every class targets Java 11`() {
        val versions = mutableMapOf<String, Int>()
        for (entry in jar.entries()) {
            if (!entry.name.endsWith(".class")) continue
            jar.getInputStream(entry).use { stream ->
                val data = DataInputStream(stream)
                val magic = data.readInt()
                assertEquals(-0x35014542, magic, "${entry.name} is not a class file")
                data.readUnsignedShort() // minor
                versions[entry.name] = data.readUnsignedShort()
            }
        }
        assertTrue(versions.isNotEmpty())
        val wrong = versions.filterValues { it != expectedMajor }
        assertEquals(emptyMap<String, Int>(), wrong, "class file major version")
    }

    @Test
    fun `the Kotlin metadata is one an Android toolchain can read`() {
        // A Kotlin compiler reads metadata up to one minor version above its own.
        // Metadata 2.2 is therefore readable from Kotlin 2.1 onwards, which
        // includes the built-in Kotlin of the current Android Gradle plugin.
        val module = jar.entries().asSequence()
            .first { it.name.endsWith(".kotlin_module") }
        val bytes = jar.getInputStream(module).use { it.readBytes() }
        assertTrue(bytes.isNotEmpty())

        val metadata = Class.forName("io.libbusinessid.BusinessIdEngine")
            .getAnnotation(Metadata::class.java)
        val version = metadata.metadataVersion.toList()
        assertEquals(listOf(2, 2, 0), version, "the metadata version consumers have to read")
    }
}

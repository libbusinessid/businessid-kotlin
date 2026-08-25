// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.hygiene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File

/**
 * A test that no test in this module can be dropped without anyone noticing.
 *
 * JUnit refuses to run a `@Test` method that returns a value, and it does so in
 * silence: the method simply never appears in the report. That happened here to
 * nine of the ten property tests, which were written as
 * `fun x() = runBlocking { checkAll(...) }` — an expression body whose type is
 * the `PropertyContext` `checkAll` returns, not `Unit`. Every one of them looked
 * green while none of them ran.
 *
 * This walks the compiled test classes rather than the sources, so it sees what
 * JUnit sees.
 */
class TestHygieneTest {
    private val classesDir = File(
        requireNotNull(System.getProperty("entid.test.classes")) {
            "the test task must set entid.test.classes"
        },
    )

    private fun testClasses(): List<Class<*>> {
        val loader = TestHygieneTest::class.java.classLoader
        return classesDir.walkTopDown()
            .filter { it.isFile && it.extension == "class" && '$' !in it.name }
            .map { it.relativeTo(classesDir).path.removeSuffix(".class").replace(File.separatorChar, '.') }
            .mapNotNull { runCatching { loader.loadClass(it) }.getOrNull() }
            .toList()
    }

    @Test
    fun `the compiled test classes are where the build says they are`() {
        assertTrue(classesDir.isDirectory, "$classesDir does not exist")
        assertTrue(testClasses().size >= 10, "found only ${testClasses().size} test classes")
    }

    @Test
    fun `no test method returns a value, which JUnit would drop in silence`() {
        val offenders = mutableListOf<String>()
        for (type in testClasses()) {
            for (method in type.declaredMethods) {
                val annotated = method.annotations.any {
                    it.annotationClass.qualifiedName == "org.junit.jupiter.api.Test"
                }
                if (annotated && method.returnType != Void.TYPE) {
                    offenders += "${type.name}.${method.name} returns ${method.returnType.name}"
                }
            }
        }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `a test factory returns something JUnit can iterate`() {
        val offenders = mutableListOf<String>()
        for (type in testClasses()) {
            for (method in type.declaredMethods) {
                val annotated = method.annotations.any {
                    it.annotationClass.qualifiedName == "org.junit.jupiter.api.TestFactory"
                }
                if (annotated && method.returnType == Void.TYPE) {
                    offenders += "${type.name}.${method.name} returns nothing"
                }
            }
        }
        assertEquals(emptyList<String>(), offenders)
    }

    @TestFactory
    fun `the factory annotation itself is exercised`(): List<org.junit.jupiter.api.DynamicTest> =
        listOf(org.junit.jupiter.api.DynamicTest.dynamicTest("a factory runs") { assertTrue(true) })
}

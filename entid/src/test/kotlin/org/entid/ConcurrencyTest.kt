// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The engine is immutable after construction, keeps no state between two calls,
 * and gives every thread the same answer.
 *
 * The threads start together on a latch rather than one after another: a race in
 * a lazily initialised table only shows when several threads reach it at once.
 */
class ConcurrencyTest {
    private val inputs = listOf(
        IdentifierInput(IdentifierKind.SIRET, "01234567400001"),
        IdentifierInput(IdentifierKind.SIRET, "01234567400000"),
        IdentifierInput(IdentifierKind.LEI, "00000000000000000098"),
        IdentifierInput(IdentifierKind.VAT, "BE0123456749"),
        IdentifierInput(IdentifierKind.VAT, "0123456749", "FR"),
        IdentifierInput(IdentifierKind.DUNS, "01-234-5678"),
        IdentifierInput(IdentifierKind.SIREN, "012345674", "DE"),
        IdentifierInput(IdentifierKind("nope"), "X"),
    )

    @Test
    fun `many threads validating at once agree with a single one`() {
        val engine = EntIdEngine.default()
        val expected = inputs.map { engine.validate(it) }
        val threads = 16
        val start = CountDownLatch(1)
        val failures = ConcurrentLinkedQueue<String>()
        val pool = Executors.newFixedThreadPool(threads)
        try {
            repeat(threads) {
                pool.submit {
                    start.await()
                    repeat(400) {
                        inputs.forEachIndexed { index, input ->
                            val observed = engine.validate(input)
                            if (observed != expected[index]) failures += "$input gave $observed"
                        }
                    }
                }
            }
            start.countDown()
            pool.shutdown()
            assertTrue(pool.awaitTermination(120, TimeUnit.SECONDS), "the pool did not finish")
        } finally {
            pool.shutdownNow()
        }
        assertEquals(emptyList<String>(), failures.toList())
    }

    @Test
    fun `the first touch of the engine from several threads yields one instance`() {
        val threads = 8
        val start = CountDownLatch(1)
        val seen = AtomicReference<EntIdEngine?>(null)
        val distinct = ConcurrentLinkedQueue<EntIdEngine>()
        val pool = Executors.newFixedThreadPool(threads)
        try {
            repeat(threads) {
                pool.submit {
                    start.await()
                    val engine = EntIdEngine.default()
                    if (!seen.compareAndSet(null, engine) && seen.get() !== engine) distinct += engine
                }
            }
            start.countDown()
            pool.shutdown()
            assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS))
        } finally {
            pool.shutdownNow()
        }
        assertEquals(emptyList<EntIdEngine>(), distinct.toList())
    }

    @Test
    fun `canonicalisation of one input is unaffected by another running beside it`() {
        val engine = EntIdEngine.default()
        val a = IdentifierInput(IdentifierKind.VAT, "0123456749", "BE")
        val b = IdentifierInput(IdentifierKind.VAT, "  el 012345670  ")
        val expectedA = engine.canonicalize(a).canonicalValue
        val expectedB = engine.canonicalize(b).canonicalValue
        val failures = ConcurrentLinkedQueue<String>()
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        try {
            repeat(8) { worker ->
                pool.submit {
                    start.await()
                    repeat(2000) {
                        val input = if (worker % 2 == 0) a else b
                        val expected = if (worker % 2 == 0) expectedA else expectedB
                        val observed = engine.canonicalize(input).canonicalValue
                        if (observed != expected) failures += "$input gave $observed"
                    }
                }
            }
            start.countDown()
            pool.shutdown()
            assertTrue(pool.awaitTermination(120, TimeUnit.SECONDS))
        } finally {
            pool.shutdownNow()
        }
        assertEquals(emptyList<String>(), failures.toList())
    }

    @Test
    fun `repeated calls return equal reports and share the immutable metadata`() {
        val engine = EntIdEngine.default()
        assertSame(engine.rulesInfo(), engine.rulesInfo())
        assertSame(engine.capabilities(), engine.capabilities())
        val input = IdentifierInput(IdentifierKind.SIRET, "01234567400001")
        assertEquals(engine.validate(input), engine.validate(input))
    }
}

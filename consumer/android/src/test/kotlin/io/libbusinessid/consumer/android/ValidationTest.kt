// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.consumer.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The published artefact, compiled and run by an Android toolchain.
 *
 * Every value is synthetic and comes from the shared conformance corpus.
 */
class ValidationTest {
    @Test
    fun canonicalisesFromAnAndroidModule() {
        assertEquals("01234567400001", Validation.canonical("siret", "012 345 674 00001"))
    }

    @Test
    fun validatesFromAnAndroidModule() {
        assertTrue(Validation.isFullyValidated("siret", "01234567400001"))
        assertTrue(!Validation.isFullyValidated("siret", "01234567400000"))
    }

    @Test
    fun readsTheRulesVersion() {
        assertEquals(System.getProperty("businessid.rules.version"), Validation.rulesVersion())
    }
}

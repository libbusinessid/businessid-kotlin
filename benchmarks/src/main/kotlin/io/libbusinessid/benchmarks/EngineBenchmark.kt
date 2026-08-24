// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.benchmarks

import io.libbusinessid.BusinessIdEngine
import io.libbusinessid.IdentifierInput
import io.libbusinessid.IdentifierKind
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * The five measurements `engine.md` section 14 asks for: cold load, simple
 * validation, complex checksum, early rejection and parallel execution.
 *
 * Every value is synthetic and drawn from the shared conformance corpus.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
open class EngineBenchmark {
    private lateinit var engine: BusinessIdEngine

    /** siren-valid, nine digits closed by a Luhn check digit. */
    private val simple = IdentifierInput(IdentifierKind.SIREN, "012345674")

    /** siret: a call into the SIREN checksum, then a branch over two algorithms. */
    private val complex = IdentifierInput(IdentifierKind.SIRET, "01234567400001")

    /** A separated form, so canonicalisation has work to do. */
    private val separated = IdentifierInput(IdentifierKind.SIRET, "012 345 674 00001")

    /** Rejected at the first step of dispatch, before any program runs. */
    private val unknownKind = IdentifierInput(IdentifierKind("no_such_kind"), "0123456789")

    /** Refused by the input bound, without being processed. */
    private val tooLong = IdentifierInput(IdentifierKind.SIREN, "1".repeat(1025))

    /** Twenty alphanumeric characters through the ISO 7064 modulo 97 expansion. */
    private val mod97 = IdentifierInput(IdentifierKind.LEI, "000000ABCDEF12345670")

    /**
     * A German EUID, whose register identifier is looked up among 2 566 court
     * codes. The list is a sorted string constant read by binary search, so this
     * should sit beside the other validations rather than above them.
     *
     * euid-de-valid-001, synthetic.
     */
    private val membership = IdentifierInput(IdentifierKind.EUID, "DEF1103.HRB12345")

    /** The same, refused by the list rather than by the shape. */
    private val membershipRefused = IdentifierInput(IdentifierKind.EUID, "DEZZZZZ.HRB12345")

    @Setup
    fun setUp() {
        engine = BusinessIdEngine.default()
    }

    @Benchmark
    fun simpleValidation(blackhole: Blackhole) {
        blackhole.consume(engine.validate(simple))
    }

    @Benchmark
    fun complexChecksum(blackhole: Blackhole) {
        blackhole.consume(engine.validate(complex))
    }

    @Benchmark
    fun mod97Checksum(blackhole: Blackhole) {
        blackhole.consume(engine.validate(mod97))
    }

    @Benchmark
    fun membershipCheckedValidation(blackhole: Blackhole) {
        blackhole.consume(engine.validate(membership))
    }

    @Benchmark
    fun membershipRefusedValidation(blackhole: Blackhole) {
        blackhole.consume(engine.validate(membershipRefused))
    }

    @Benchmark
    fun canonicalisationWithSeparators(blackhole: Blackhole) {
        blackhole.consume(engine.canonicalize(separated))
    }

    @Benchmark
    fun formatOnly(blackhole: Blackhole) {
        blackhole.consume(engine.validateFormat(complex))
    }

    @Benchmark
    fun earlyRejectionUnknownKind(blackhole: Blackhole) {
        blackhole.consume(engine.validate(unknownKind))
    }

    @Benchmark
    fun earlyRejectionInputTooLong(blackhole: Blackhole) {
        blackhole.consume(engine.validate(tooLong))
    }

    @Benchmark
    @Threads(8)
    fun parallelValidation(blackhole: Blackhole) {
        blackhole.consume(engine.validate(complex))
    }
}

/**
 * What a generated engine pays before its first answer.
 *
 * There is no ruleset to decode, so this measures the class initialisation of
 * the emitted tables and nothing else. It is a single shot per fork on purpose:
 * measuring it twice would measure a field read.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1, batchSize = 1)
@Fork(5)
open class ColdStartBenchmark {
    @Benchmark
    fun firstValidation(blackhole: Blackhole) {
        blackhole.consume(
            BusinessIdEngine.default().validate(IdentifierInput(IdentifierKind.SIRET, "01234567400001")),
        )
    }
}

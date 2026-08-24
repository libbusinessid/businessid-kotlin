# businessid — Kotlin engine

Offline canonicalisation, format validation and checksum validation of business
identifiers, for the JVM and for Android.

The rules are not interpreted at runtime. A generator reads
`spec/businessid-rules.binpb` when this library is built, runs the twenty-five
load checks of `ir.md` section 10, and emits Kotlin. What ships is that emitted
code, the primitives it calls, and a hand-written API — no ruleset, no Protobuf,
no decoder.

```text
rules 2026.08.26, format version 1
94 identifier definitions · 37 kinds · 250 programs · 2376 IR nodes
conformance: 666 of 666 cases matched, 0 differed
```

## What it validates, and what it does not

| Step | What a `valid` answer means |
| --- | --- |
| `format` | The shape matches a variant the issuer documents. |
| `checksum` | The check digits the issuer publishes are satisfied. |

It says nothing about whether a company exists, is active, or owns the
identifier. Answering that needs a register, and this version queries none.

Three statuses are not verdicts and must not be read as one:

- `unsupported` — nothing here can conclude. No rule is published, no algorithm
  is documented, the country is unknown to this ruleset. Refusing a legitimate
  identifier is the most serious defect this project recognises, so absence of
  knowledge never becomes invalidity.
- `not_run` — an earlier step made this one pointless.
- `invalid` — a documented, applicable rule proved the value wrong.

## Install

Gradle:

```kotlin
dependencies {
    implementation("io.libbusinessid:businessid:0.1.0")
}
```

Maven:

```xml
<dependency>
  <groupId>io.libbusinessid</groupId>
  <artifactId>businessid</artifactId>
  <version>0.1.0</version>
</dependency>
```

The library declares nothing but the Kotlin standard library. A CI job fails the
build if that stops being true.

## Kotlin

```kotlin
import io.libbusinessid.BusinessIdEngine
import io.libbusinessid.IdentifierInput
import io.libbusinessid.IdentifierKind

val engine = BusinessIdEngine.default()

val report = engine.validate(IdentifierInput(IdentifierKind.SIRET, "012 345 674 00001"))
report.canonicalValue   // "01234567400001"
report.isFullyValidated // true
```

Every identifier in this file is **synthetic**, produced by the documented
generator of the shared conformance corpus, and named with the case it comes
from. `01234567400001` is `siret-synthetic-valid-001`. None of them designates a
company, and none should be treated as if it did.

### Format only

```kotlin
val report = engine.validateFormat(IdentifierInput(IdentifierKind.SIRET, "01234567400001"))
report.format.status          // VALID
report.checksum.status        // NOT_RUN
report.checksum.reasonCode    // NOT_REQUESTED
```

### An algorithm the issuer does not publish

```kotlin
// duns-valid-001, synthetic.
val report = engine.validate(IdentifierInput(IdentifierKind.DUNS, "012345678"))
report.format.status        // VALID
report.checksum.status      // UNSUPPORTED
report.checksum.reasonCode  // CHECKSUM_NOT_PUBLISHED
report.isInvalid            // false — unsupported is not a verdict
```

### Canonicalisation alone

```kotlin
// lei-canonicalize-020, synthetic.
val result = engine.canonicalize(IdentifierInput(IdentifierKind.LEI, "0000-0000-0000-0000-0098"))
result.canonicalValue  // "00000000000000000098"
result.status          // VALID
```

### The country context

```kotlin
// vat-gr-country-context-006, synthetic. Greece routes VAT under the prefix EL
// and the reported country stays the ISO code.
val report = engine.validate(IdentifierInput(IdentifierKind.VAT, "012345670", countryCode = "GR"))
report.canonicalValue  // "EL012345670"
report.countryCode     // "GR"
```

A country that contradicts a recognised prefix is the one dispatch failure that
is a verdict:

```kotlin
// dispatch-country-mismatch-004, synthetic.
val report = engine.validate(IdentifierInput(IdentifierKind.VAT, "BE0123456749", countryCode = "FR"))
report.format.reasonCode  // COUNTRY_MISMATCH
```

### Profiles

The absence of a profile is meaningful: it is what lets the selected definition
apply its own default. Passing `ValidationProfile.COMPATIBLE` explicitly is a
different request from passing nothing.

```kotlin
engine.validate(input)                                                   // the definition decides
engine.validate(input, ValidationOptions(ValidationProfile.STRICT_CURRENT)) // current variants only
```

## Java

```java
import io.libbusinessid.*;

BusinessIdEngine engine = BusinessIdEngine.defaultEngine();

ValidationReport report = engine.validate(IdentifierInput.of("siret", "01234567400001"));
report.getFormat().getStatus();          // VALID
report.getKindToken();                   // "siret"
```

Three things read differently from Java, and each has a reason:

- `BusinessIdEngine.defaultEngine()` — `default` is a Java keyword, so the
  method Kotlin reads best is one Java cannot spell.
- `IdentifierInput.of(kind, value)` — `IdentifierKind` is a value class, which
  the specification requires so an unknown kind stays representable. Java sees
  the erased `String`, and this factory is the entry point written for it.
- `getKindToken()` — for the same reason, the accessor returning an
  `IdentifierKind` carries a mangled JVM name. This one returns the token.

## Supported versions

| | Minimum | Built and tested with |
| --- | --- | --- |
| Kotlin (consumer) | 2.1 | 2.4.10 compiles it, metadata 2.2 |
| JDK (consumer) | 11 | 17, 21 and 25 in CI |
| Android | minSdk 21, AGP 8.0 | AGP 9.3.1, compileSdk 36 |
| Gradle (consumer) | 8.0 | 9.7.0 |

The published bytecode is Java 11 (class file major version 55), and
`BytecodeTest` reads that back out of the jar rather than trusting the build
settings.

The Kotlin floor is worth a word. A Kotlin compiler reads metadata up to one
minor version above its own, and the built-in Kotlin of the current Android
Gradle plugin is 2.2. A library compiled at the newest Kotlin is therefore
unusable on Android. This one is compiled by Kotlin 2.4 but speaks 2.2:
language version, API version and core libraries alike.

**Kotlin Multiplatform is not announced.** This release targets the JVM and
Android, and nothing else has been run against the conformance corpus.

## A custom ruleset

There is no factory taking a ruleset as bytes. Such an API would put the
decoder, the twenty-five load checks and the whole execution machinery into
every caller — an interpreter, which `engine.md` section 1.2 forbids.

A custom ruleset goes through the generator, at build time:

```bash
./gradlew generateEngine \
  -PbusinessidBundle=/path/to/your/businessid-rules.binpb
```

or directly:

```bash
java -cp generator.jar io.libbusinessid.generator.MainKt \
  --bundle your-rules.binpb \
  --out src/main/kotlin/io/libbusinessid/generated
```

The generator refuses to emit a single line from a ruleset that fails any of the
twenty-five checks, and names the check that refused it.

## What is in this repository

| Path | Role |
| --- | --- |
| `businessid/` | The published library: emitted rules, primitives, API. |
| `generator/` | Reads the ruleset, runs the load checks, emits Kotlin. Not published. |
| `testee/` | The conformance testee. Not published. |
| `benchmarks/` | JMH harness. Not published. |
| `consumer/jvm`, `consumer/android` | Builds of their own that take the published artefact. |
| `spec/` | The pinned specification, schemas, ruleset and corpus. |
| `rules.lock` | The digests of every file above. |

## Conformance

The runner comes from the `spec` repository and from nowhere else, pinned to the
commit `rules.lock` records — the same commit as the corpus, which makes it
impossible to judge a corpus with another comparator. This repository provides a
testee and the tests that prove it does not cheat; it contains no comparator.

```bash
./gradlew :testee:installDist
GOTOOLCHAIN=auto go run \
  "github.com/libbusinessid/spec/cmd/conformance-runner@$(grep '^source_commit' rules.lock | cut -d'"' -f2)" \
  -corpus spec/businessid-conformance.binpb \
  -- ./testee/build/install/businessid-testee/bin/businessid-testee
```

```text
rules 2026.08.26: 666 cases, 666 matched, 0 differed
conformant
```

## Building

```bash
./scripts/verify-lock.sh   # the eight digests rules.lock declares
./gradlew build            # compile, lint, test, coverage, consumers
./gradlew generateEngine   # re-emit the rules from the ruleset
./gradlew checkGenerated   # fail when the committed sources are stale
./gradlew fuzz             # Jazzer, beyond the regression corpus
./gradlew :benchmarks:jmh  # the five measurements engine.md section 14 asks for
```

Coverage is split the way `engine.md` section 12.2 splits it. The thresholds —
95 % of lines, 90 % of branches — cover hand-written code. The rules emitted
from the ruleset are reported beside it and never gated: that figure measures
the conformance corpus, not the engine, and a threshold there would fail an
irreproachable engine on a gap in the corpus.

```text
hand written   lines  99.01%   branches  92.64%
emitted        lines  88.58%   branches  68.61%
```

The figures come from the test suite alone: the fuzz task is excluded from
instrumentation, because a number that depended on whether Jazzer happened to
run, and on which inputs it happened to generate, would not be a measurement.

### Mutation testing

```bash
./gradlew :businessid:mutationTest
```

Pitest is aimed at the runtime primitives and the pipeline, where an off-by-one
in a comparison or a flipped bound is a wrong verdict rather than a compile
error. It scores **90.4 %** — 807 of 893 mutants killed, against the 80 % that
`engine.md` section 12.5 recommends. The emitted rules are left out: mutating a
table produced from the ruleset measures the corpus again.

It found two real gaps, both now closed. The target index was compared with
`< 0` where `-1` is a sentinel and not an ordering, and a boundary slip survived
because the one dispatcher owning target 0 has a single target, so every
fallback converged on it; the comparisons are now `== NO_TARGET`. And neither
bound of the six code points a dispatch trim removes was exercised anywhere,
which `TokensTest` now pins on both sides.

The eighty-six survivors fall into three families, each equivalent by
construction: the fast-path early returns of `CanonBuffer` and `Tokens`, whose
removal leaves the value identical and only the work larger; the null-check
intrinsics Kotlin inserts, which are not this project's code; and the hash
multiplier and a `StringBuilder` capacity, neither of which any contract
observes.

## Threads, I/O and the network

The engine is immutable after construction, safe to share between threads, and
keeps no state between two calls. It performs no I/O, reads no resource, and
makes no network call.

Every operation is synchronous, permanently. A remote register lookup, when it
exists, will be a separate asynchronous operation in a separate module; it will
never become a mode of these. This version exposes no registry type at all, not
even an experimental one: a public type is a commitment SemVer freezes.

## Licence

Apache License 2.0. See [LICENSE](LICENSE).

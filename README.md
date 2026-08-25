# businessid — Kotlin engine

Offline canonicalisation, format validation and checksum validation of business
identifiers, for the JVM and for Android.

The rules are not interpreted at runtime. A generator reads
`spec/businessid-rules.binpb` when this library is built, runs the twenty-five
load checks of `ir.md` section 10, and emits Kotlin. What ships is that emitted
code, the primitives it calls, and a hand-written API — no ruleset, no Protobuf,
no decoder.

```text
rules 2026.08.33, format version 1
94 identifier definitions · 37 kinds · 250 programs · 2386 IR nodes
conformance: 676 of 676 cases matched, 0 differed
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

## Ill formed text

The input is bounded at 1024 UTF-8 bytes before anything looks at it, and text
that is not well formed is refused as `invalid_encoding`. The bound is measured
first, and ill formed text has no UTF-8 encoding, so the count has to come from
somewhere. `ir.md` section 6 step 1 leaves an engine two answers and requires it
to state which it gives.

**This engine counts what its own encoder produces.** A Kotlin `String` holding
one unpaired surrogate — the only ill formed text the type admits — encodes to a
single replacement byte, so it counts as one. `Utf.utf8Length` therefore agrees
with `String.toByteArray(UTF_8).size` on every string, well formed or not, which
is stated as a property over generated input rather than left as a convention.
The array is never allocated; only its length is computed.

The other answer the specification allows is to refuse ill formed text before
measuring it. The difference is reachable only by text that is both ill formed
and within a few bytes of the bound, and both answers are `unsupported`.

## Install

Gradle:

```kotlin
dependencies {
    implementation("io.github.libbusinessid:businessid:0.1.0")
}
```

Maven:

```xml
<dependency>
  <groupId>io.github.libbusinessid</groupId>
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

## Membership lists

Three rules check an identifier against a list a register publishes: 2 566
German court codes, 148 French greffe codes, and the Luxembourg section letter.
Those lists are emitted as sorted string constants read by binary search, not as
arrays walked from the front. A string literal lives in the class constant pool:
no initialiser bytecode, no allocation before the first call, and a lookup that
does not grow with the list.

That form was not a preference. Emitted one array literal per entry, 2 714
entries overflow the sixty-four kilobyte limit the JVM places on a class
initialiser and the library stops compiling. Any array literal this generator
emits is now split across methods once it passes five hundred elements, whatever
a future ruleset carries.

The German list is split by length, and the emitted search reads one group per
length. 782 of the 818 six-character codes begin with a five-character one, so
over a single list *starting with* a code would not mean *being* one, and
`DEB1000X` would have passed on the strength of `B1000`.

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

## How the rules get here

The engine fetches the release; the release does not push into the engine.
`.github/workflows/rules-sync.yml` runs daily and on demand, compares the newest
`spec` release to `rules.lock`, and stops when they agree. When they differ it
downloads the artefacts into the runner's temporary directory, verifies
`SHA256SUMS` and then the provenance attestation — owner, repository, signing
workflow, tag, and the commit read from the signing certificate rather than from
the manifest the release itself wrote — and only then writes `spec/`,
`rules.lock` and `spec/PROVENANCE.md`, regenerates the emitted code, runs
`./scripts/verify.sh` and opens a pull request. A release whose attestation does
not verify never touches the working tree.

Two things follow from doing it this way. Regeneration needs this engine's
toolchain, which `spec` does not have, so the pull request carries the emitted
code and not only the bundle. And `spec` needs no write token here: the workflow
uses the `GITHUB_TOKEN` GitHub already gives it, so a compromise of `spec` stops
at `spec`. The trigger is a clock rather than a pushed event for the same
reason — a `repository_dispatch` would hand that token straight back.

The pull request is opened green or red. Green is the ordinary case: new business
rules are data, and the emitted code follows. Red means the release brought
something this engine does not do yet, and **a red pull request is never merged
to unblock the chain** — it is corrected, or the release is refused with the
reason written down.

### What the repository has to allow

The workflow asks for auto-merge on a green pull request, so a mechanical
resynchronization needs nobody and only what stayed red wants attention. Three
settings decide whether that is safe, and none of them can be granted by the
token:

| Setting | Where | Why |
| --- | --- | --- |
| Allow GitHub Actions to create and approve pull requests | Settings → Actions → General → Workflow permissions | Without it `gh pr create` refuses and step 6 cannot run at all. |
| Allow auto-merge | Settings → General → Pull Requests | Without it `gh pr merge --auto` refuses. |
| A branch protection on `main` requiring the CI job that runs `./scripts/verify.sh`, and requiring nothing else | Settings → Branches | Auto-merge merges when nothing *blocks*, which is not the same as on green. A second required check would give "green" two definitions, and auto-merge follows the weaker one. The scheduled workflow and Dependabot must not be required checks. |

Tagging and publishing stay manual. Nothing in this workflow releases anything.

## Releasing

The library is published to Maven Central as `io.github.libbusinessid:businessid`
— the Maven coordinates; the Kotlin package namespace is `io.libbusinessid` and
does not move.

A tag `vx.y.z` is the only thing that publishes.
`.github/workflows/release.yml` checks that the tag, `EngineVersion.VALUE` and
the changelog agree, runs the whole CI suite at that commit, builds a signed
bundle with `./scripts/release-bundle.sh`, and uploads it to the Central Portal
as a `USER_MANAGED` deployment. It stops at `VALIDATED`; a person presses
Publish, having read what the Portal says it would publish. A tag can be deleted
and a deployment can be dropped — a version on Maven Central can only ever be
deprecated.

The upload is `curl` against the Portal's publisher API. Sonatype ships no Gradle
plugin for the Portal, and what does the upload runs with a PGP private key and a
publishing token in its environment, so it is four documented endpoints rather
than a release orchestrator or a plugin on the build script classpath.
`CONTRIBUTING.md` names the four repository secrets and the one namespace
verification a human has to do before any of it works.

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
rules 2026.08.33: 676 cases, 676 matched, 0 differed
conformant
```

## Building

One command verifies everything, and it is the one CI runs:

```bash
./scripts/verify.sh
```

```text
verify ok — rules 2026.08.33 · conformance 676/676 · tests 568 · toolchains 17+25 · coverage 99.04%/93.07% · jar 143431 B
```

It covers the lock digests, the regeneration of the emitted sources,
compilation, tests, the shared conformance suite against the runner from `spec`,
lint, format, coverage and its thresholds, the resolution of every declared
dependency, packaging including both consumer projects, and both ends of the
supported JDK range — the pinned toolchain throughout, and the far end compiled
and run again with `-Pbusinessid.toolchain`. It prints that one line when
everything passes, the failing step's
output and only that when something does not, and exits non-zero either way it
should. `engine.md` section 12.5 asks for it; `CLAUDE.md` says why it is worth
preferring to the pieces.

The pieces are still there when a single one is what you want:

```bash
./scripts/verify-lock.sh   # the eight digests rules.lock declares
./gradlew build            # compile, lint, test, coverage, consumers
./gradlew generateEngine   # re-emit the rules from the ruleset
./gradlew checkGenerated   # fail when the committed sources are stale
./gradlew fuzz             # Jazzer, beyond the regression corpus
./gradlew test -Pbusinessid.toolchain=25   # the far end of the range, alone
./gradlew :benchmarks:jmh  # the five measurements section 14 asks for, and more
```

Coverage is split the way `engine.md` section 12.2 splits it. The thresholds —
95 % of lines, 90 % of branches — cover hand-written code. The rules emitted
from the ruleset are reported beside it and never gated: that figure measures
the conformance corpus, not the engine, and a threshold there would fail an
irreproachable engine on a gap in the corpus.

```text
hand written   lines  99.04%   branches  93.04%
emitted        lines  88.58%   branches  68.74%
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
error. It scores **90.5 %** — 856 of 946 mutants killed, against the 80 % that
`engine.md` section 12.5 recommends. The emitted rules are left out: mutating a
table produced from the ruleset measures the corpus again.

It found two real gaps, both now closed. The target index was compared with
`< 0` where `-1` is a sentinel and not an ordering, and a boundary slip survived
because the one dispatcher owning target 0 has a single target, so every
fallback converged on it; the comparisons are now `== NO_TARGET`. And neither
bound of the six code points a dispatch trim removes was exercised anywhere,
which `TokensTest` now pins on both sides.

The ninety survivors fall into three families, each equivalent by construction:
the fast-path early returns of `CanonBuffer` and `Tokens`, whose removal leaves
the value identical and only the work larger; the null-check intrinsics Kotlin
inserts, which are not this project's code; and the hash multiplier and a
`StringBuilder` capacity, neither of which any contract observes.

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

# Contributing

## Where a change belongs

This repository holds an engine, not the rules. A rule, a country, a checksum
algorithm or a conformance case belongs in
[`libbusinessid/spec`](https://github.com/libbusinessid/spec), and reaches every
engine from there.

What belongs here: the generator, the runtime primitives, the public API, the
build and the tests.

**Never edit anything under `spec/` or `businessid/src/main/kotlin/io/libbusinessid/generated/`.**
The first is a pinned copy of the specification; the second is emitted, and
`./gradlew checkGenerated` fails when it drifts from the ruleset.

## The loop

```bash
./scripts/verify-lock.sh   # the eight digests rules.lock declares
./gradlew build            # compile, lint, test, coverage, consumers
```

Run it on **JDK 17**, the toolchain this project pins. Detekt and ktlint each
embed a Kotlin compiler that refuses a class file version newer than the release
it was built against, and neither offers a way to point at another JVM — on a
JDK 25 daemon both stop with `25.0.4` before reading a line of your code. CI runs
them in a job of their own for that reason, and builds and tests the code itself
across the whole supported range.

Then the shared conformance suite, whose runner comes from `spec` and from
nowhere else:

```bash
./gradlew :testee:installDist
GOTOOLCHAIN=auto go run \
  "github.com/libbusinessid/spec/cmd/conformance-runner@$(grep '^source_commit' rules.lock | cut -d'"' -f2)" \
  -corpus spec/businessid-conformance.binpb \
  -- ./testee/build/install/businessid-testee/bin/businessid-testee
```

## What a change has to carry

- **A test that failed before it.** A bug is proved by a test that fails first.
  If you did not watch it fail, you proved nothing — make the guard fail on
  purpose once, then fix it.
- **No disabled check.** No skipped test, no lowered threshold, no conformance
  case excluded. A case that cannot pass is a release blocker, not a candidate
  for exclusion.
- **A local, commented suppression, or none.** Two file-level Detekt
  suppressions exist and both say why in the file. A third needs the same
  justification.
- **English.** Code, comments, commit messages and documentation.

## Identifiers in code and documentation

Never invent one. A value that looks like a real identifier and came from
memory is forbidden everywhere in this repository.

A README example demonstrates an API, so a synthetic value is the right choice
there — and it must say what it is and name the conformance case it comes from.
A real value would designate a company without anyone needing it to.

## Interpreting the specification

If two documents disagree, or one is silent on something observable, **stop and
record it in `SPEC-ISSUES.md`** rather than choosing quietly. Say what you
measured, what the engine does meanwhile, and what you propose upstream. Every
entry there was found by measuring, and each one is a question the specification
answers next.

## The generator, not an interpreter

The rules are compiled into source code when this library is built. The
published library holds that code, the primitives it calls and the API — no
ruleset, no Protobuf, no decoder.

Adding a Protobuf dependency to the `businessid` module, embedding a `.binpb` as
a resource, or adding a factory that takes a ruleset as bytes each break that,
and each is a test failure: `PackagingTest` opens the published jar.

## Reviewing a change to the core

- Which shared semantics does it affect?
- Can the four engines implement it without diverging?
- What risk of a false negative does it introduce?
- Which limits and hostile inputs are tested?
- Does the conformance corpus need to change — and therefore `spec`?
- Does the public API stay compatible? `./gradlew apiCheck` answers half of it.

An optimisation must show by test that it changes no result.

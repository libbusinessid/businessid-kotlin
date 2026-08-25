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
JDK 25 daemon both stop with `25.0.4` before reading a line of your code.

That is about the *daemon*, not about the range this library supports.
`verify.sh` compiles and runs the code on the far end of the range as well, with
`-Pbusinessid.toolchain=25`: the toolchain moves, the daemon does not, and the
analysers are left alone. Gradle fetches that JDK the first time and builds into
`build/jdk25/` so it never overwrites what the pinned toolchain produced. The two
ends of the range are named once, in `buildSrc/src/main/kotlin/BuildConstants.kt`.

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

## Releasing

A tag publishes; nothing else does. Merging verified code and publishing a
package are two acts, and the second is the only one that cannot be taken back —
a version on Maven Central is never replaced or removed, only deprecated.

The steps, in order:

1. Move the `## [Unreleased]` section of `CHANGELOG.md` to `## [x.y.z]`.
2. Set `EngineVersion.VALUE` to `x.y.z` and `version` in `gradle.properties` to
   `x.y.z-SNAPSHOT` for the work that follows. `./scripts/verify.sh` has to pass.
3. Push the tag `vx.y.z`. `.github/workflows/release.yml` checks that the tag,
   `EngineVersion.VALUE` and the changelog agree, runs the whole CI suite at that
   commit, builds and signs the bundle, and uploads it to the Central Portal as a
   `USER_MANAGED` deployment.
4. The deployment stops at `VALIDATED`. Open
   <https://central.sonatype.com/publishing/deployments>, read what it says it
   would publish, and press Publish.

`./scripts/release-bundle.sh x.y.z` builds the same archive locally, given
`SIGNING_KEY` and `SIGNING_PASSWORD`, and refuses to produce one the Portal would
reject. CI runs it on every pull request with a throwaway key it generates and
discards, so the script a tag depends on is never running for the first time.

### What a human has to set up once

Neither of these can be done by a workflow, and until both are done the release
workflow fails on the tag rather than publishing half of anything.

**Verify the namespace.** The published groupId is `io.github.libbusinessid` —
the Maven coordinates, not the Kotlin package namespace, which stays
`io.libbusinessid`. The Central Portal verifies `io.github.<account>` by asking
you to prove you control that GitHub account or organisation, which for
`libbusinessid` means an owner of the organisation doing it at
<https://central.sonatype.com/publishing/namespaces>.

**Create four repository secrets**, and only these four:

| Secret | Where it comes from |
| --- | --- |
| `CENTRAL_PORTAL_USERNAME` | <https://central.sonatype.com/account>, *Generate User Token*. The token is a username and password pair; regenerating it invalidates the previous one. |
| `CENTRAL_PORTAL_PASSWORD` | The password half of that same token. |
| `SIGNING_KEY` | An ASCII armoured PGP private key: `gpg --armor --export-secret-keys <id>`. Its public half has to be on a keyserver Central checks — `gpg --keyserver keys.openpgp.org --send-keys <id>`. |
| `SIGNING_PASSWORD` | That key's passphrase. |

The publishing job checks all four before it checks out the repository, and names
every one that is missing. A release build also refuses to produce an unsigned
artefact at all: `signing` is required whenever the version is not a snapshot, so
a missing key is a failed build rather than a jar nobody signed.

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

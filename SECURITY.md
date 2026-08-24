# Security

## Reporting

Report a vulnerability through GitHub's private advisory form on this
repository, or by e-mail to the maintainers. Please do not open a public issue
for something exploitable.

Include what you did, what happened, and what you expected. A ruleset or an
input that reproduces it is worth more than a description.

You should get an acknowledgement within a few days. A fix ships with a
regression case in the shared conformance corpus wherever the corpus can carry
one, so every engine of the project inherits it.

## What this library does, and does not do

It validates the shape and the check digits of a business identifier, offline.
It never says a company exists, is active, or owns the identifier: answering that
needs a register, and this version queries none.

Nothing here reads a file, opens a socket, or resolves a name. `PackagingTest`
fails the build if a class of the published jar so much as references
`java.net.URL`, `java.nio.file.Files` or `ClassLoader.getResource`.

## The trust boundary

Two kinds of input reach this project, and they are treated very differently.

**A user identifier is untrusted, and never dangerous.** It is bounded at 1024
UTF-8 bytes before anything looks at it, it is refused if it is not well formed
text, and it drives no allocation that is not bounded by that limit. Every rule
that could reject it is data, compiled at build time; there is no interpreter and
no dynamic dispatch at validation time. Ordinary input never raises: a value the
rules reject produces a report.

**A ruleset is untrusted, and only reaches the generator.** The published library
never loads one — it holds no decoder, no Protobuf runtime and no `.binpb`, which
`PackagingTest` verifies by opening the jar. The generator applies the
twenty-five load checks of `ir.md` section 10 before emitting a line, including a
bounded wire pre-scan for unknown fields at any depth, an arithmetic proof that no
emitted operation can overflow a checked `int64`, and a bound on the expansion of
a graph that would otherwise be a denial of service against the generator itself.

A ruleset that fails any check produces no code at all, and the refusal names the
check.

## What is fuzzed

Jazzer runs against both boundaries: arbitrary strings through the four public
operations, and arbitrary bytes and mutated rulesets through the generator. The
properties are that nothing throws, nothing hangs, nothing allocates without a
bound, and that the contract still holds on whatever comes out. A smoke run is
part of every build; a long run is scheduled weekly.

## Supply chain

Every dependency version is pinned in `gradle/libs.versions.toml`. Every GitHub
Action is pinned to a commit SHA. The Gradle wrapper is committed and its
checksum validated in CI.

The rules artefact is pinned by digest in `rules.lock`, and `scripts/verify-lock.sh`
checks all eight digests before anything else runs. Once a release exists, the
synchronisation pull request will verify its attestation as well.

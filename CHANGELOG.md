# Changelog

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and
this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The engine version, the rules version and the IR format version move
independently.

## [Unreleased]

### Added

- The first Kotlin engine: a generator that reads `businessid-rules.binpb`, runs
  the twenty-five load checks of `ir.md` section 10 and emits Kotlin; the
  emitted rules; the primitives they call; and the public API.
- Support for all eighteen frozen capability identifiers and all sixty-three IR
  operations. The published ruleset uses fifty-two of them; a synthetic ruleset
  in the generator's tests exercises the other eleven.
- A conformance testee speaking the protocol of `testee.proto`, and the tests
  that prove it does not read the corpus, does not look at the case identifier,
  and answers the same whatever the order of requests.
- Maven publication with sources, Dokka documentation, a POM, checksums and
  optional signing.

### Measured

- The shared conformance runner, pinned to the commit `rules.lock` records,
  reports 666 of 666 cases matched, 0 differed, against rules `2026.08.26`.
- Coverage of hand-written code: 99.21 % of lines, 94.41 % of branches. Coverage
  of the emitted rules under the corpus: 91.03 % of lines, 75.50 % of branches,
  reported apart and never gated.
- The published jar holds 122 KB of classes, no `.binpb`, no Protobuf class, and
  no reference to a network or file system API.
- The published ruleset expands to 3069 operation instances, the figure `ir.md`
  section 2 states.

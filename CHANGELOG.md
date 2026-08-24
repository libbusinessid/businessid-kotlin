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

### Changed

- Compiled against rules `2026.08.31`. Three rules gained membership checks —
  2 566 German court codes, 148 French greffe codes, and the Luxembourg section
  letter — and the ruleset grew from 99 677 to 120 872 bytes.
- A membership list is emitted as a sorted string constant read by binary search
  rather than an array of arrays walked from the front. Emitted the old way,
  2 714 entries overflow the sixty-four kilobyte limit the JVM places on a class
  initialiser and the library stops compiling. Any array literal the generator
  emits is now split across methods past five hundred elements.
- `Utf.utf8Length` counts an unpaired surrogate as the one byte the platform
  encoder emits rather than the three its code unit would have taken, which
  `ir.md` section 6 step 1 now names as one of the two answers an engine may give
  and must state. It makes the count equal to `toByteArray(UTF_8).size` for every
  string, stated as a property.
- A `WHEN` checksum branch that no `CHOOSE` reads is refused, matching the
  reference loader, with the program root left to the rule that owns it.

### Measured

- The shared conformance runner, pinned to the commit `rules.lock` records,
  reports 673 of 673 cases matched, 0 differed, against rules `2026.08.31`.
- Coverage of hand-written code: 99.04 % of lines, 93.04 % of branches. Coverage
  of the emitted rules under the corpus: 88.58 % of lines, 68.74 % of branches,
  reported apart and never gated.
- Mutation testing scores 90.5 % over the runtime primitives and the pipeline,
  856 of 946 mutants killed. It found two real gaps, both closed: a sentinel
  compared as an ordering, and neither bound of the dispatch trim set tested.
- The published jar holds 143 KB of classes, no `.binpb`, no Protobuf class, and
  no reference to a network or file system API.
- The published ruleset expands to 3094 operation instances under the
  reachable-root reading of `ir.md` section 2, and 3229 under the reading that
  section warns about.
- Benchmarks on an Apple M-series machine, JDK 17: a simple validation 218 ns, a
  SIRET with its nested SIREN checksum 311 ns, an ISO 7064 modulo 97 over twenty
  characters 472 ns, an unknown kind rejected in 31 ns, and a first validation
  in a fresh JVM 46 us. No figure is normative; they exist so a regression has
  something to be measured against.

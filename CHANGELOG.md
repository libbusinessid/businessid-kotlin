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
- `./scripts/verify.sh`, the single entry point `engine.md` section 12.5
  requires: lock digests, regeneration of the emitted sources, compilation,
  tests, the shared conformance suite against the runner from `spec`, lint,
  format, coverage and its thresholds, and packaging including both consumer
  projects. One line on success, the failing step's output and only that on
  failure, non-zero the moment a step fails. It is what CI runs, so "green" has
  one definition. `CLAUDE.md` carries the rule, as section 12.5 asks.
- The command refuses to believe an exit code of zero. Every step that owns a
  verdict is forced to execute, and afterwards must show evidence newer than the
  run; a step that was skipped or replayed fails the whole command. A step that
  *cannot* run — the Android consumer with no SDK — fails too, rather than being
  omitted from a line that claims the work was done.
- `.github/workflows/rules-sync.yml`, the synchronization `engine.md` section
  11.4 asks for: the engine fetches the release rather than the release pushing
  into the engine. Daily and on demand, it compares the newest `spec` release to
  `rules.lock` and does nothing when they agree; otherwise it downloads the
  artefacts, verifies `SHA256SUMS` and then the provenance attestation, writes
  `spec/`, `rules.lock` and `spec/PROVENANCE.md`, regenerates the emitted code,
  runs `./scripts/verify.sh`, opens a pull request green or red, and asks for
  auto-merge only when it is green. Nothing reaches the working tree before the
  attestation verifies: the artefacts are downloaded into the runner's temporary
  directory and the specification is cloned there too.
- The commit the synchronization pins to is read from the signing certificate,
  not from the manifest. The manifest is a file the release workflow wrote, so
  its `sourceCommit` is predicate data that workflow controls; the certificate's
  source repository digest comes from the OIDC token and cannot be forged by it.
  The two are compared, and disagreement fails the run.
- The workflow rehearses itself on the pull requests that change it, in dry run:
  download, both verifications, the writes, the regeneration and
  `./scripts/verify.sh`, but no push and no pull request. `workflow_dispatch`
  cannot reach a file that is not on the default branch yet, so without this the
  first run of a change to it would be its first run in production — the shape of
  defect this repository keeps finding.
- The sync uses this repository's own `GITHUB_TOKEN`. The cross-repository write
  token `spec` held on four engines is no longer needed, and the blast radius of
  a compromise of `spec` stops at `spec`. `README.md` names the three repository
  settings the token cannot grant by itself.

### Fixed

- Building the entry point found three ways a partial command reports a verdict
  it never computed. `--rerun` is a task option that binds to the task it
  follows, so `a b c --rerun` forces `c` alone; it does not reach a lifecycle
  task's dependencies, so `assemble --rerun` re-runs nothing; and the consumer
  projects' nested Gradle builds replayed their own test results, which
  `--rerun-tasks` now prevents. Each was caught by the freshness check refusing a
  zero exit, not by reading the output.
- The first version of `verify.sh` printed the failing step and exited zero:
  inside `if ! cmd`, the `$?` it read was the status of the negation, which is
  zero exactly when the command failed. Caught on the first run against a real
  failure.

- The scheduled benchmarks job could never have written its results. JMH resolves
  a relative `-rff` against the working directory of its own process, which for a
  `JavaExec` task is the project directory and not the repository root, so
  `benchmarks/build/jmh.json` aimed at `benchmarks/benchmarks/build/jmh.json`:
  JMH refused with "Can not touch the result file" before running a single
  benchmark, and had that directory existed it would have written there while the
  upload step collected nothing. The result file is now chosen by
  `benchmarks/build.gradle.kts`, which knows its own build directory, and an
  absolute path cannot be doubled.
- The scheduled toolchain job ran `./gradlew build`, which pulls in `check` and so
  detekt, which cannot run in a daemon JVM newer than the release it was built
  against. It ran `test coverage assemble` in `ci.yml` for that reason and `build`
  here. Now both run the same tasks.
- The scheduled workflow never ran on a pull request, so neither failure could be
  seen before merging. It now also runs on pull requests that touch it or the
  benchmarks.

### Changed

- Compiled against rules `2026.08.32`. The version moves backwards on purpose:
  `PATCH` in `YYYY.MM.PATCH` is a counter within a month with no upper bound, and
  four releases had announced September while still in August. Nothing in this
  repository compares rules versions for order — they are compared for equality
  against `rules.lock` and never sorted, and the published Maven version is
  independent and SemVer — so the correction costs a visible discontinuity and
  nothing else.
- Earlier, against rules `2026.09.2`. The bundle is byte identical to
  `2026.08.31` apart from the version string across all three releases, so
  nothing emitted moved but the constant that carries it. The corpus gained
  three cases and now stands at 676.
- A `prefix_in` may carry only one element length, counted in UTF-8 bytes, and a
  bundle mixing lengths is refused at check 13. "Starts with one of these" over
  one sorted list of mixed lengths answers **wrongly**, not slowly: with
  `["AB", "ABA"]` against `"ABCD"`, a search for the greatest element not after
  the input finds `ABA`, which is not a prefix, while `AB` is. All four
  membership nodes of the published ruleset hold one length each — 1 748 of
  five bytes, 818 of six, 148 of four, 41 of two — so no conformance case could
  ever catch an engine that accepted the shape, which is why the bundle may not
  carry it.
- This engine's packed search was already right on the shape and stays right:
  the emitter groups by code point count and UTF-16 length before packing, which
  is **finer** than the length the specification fixes, so a mixed list becomes
  one search per shape combined with `||` — the form `ir.md` prescribes, reached
  independently. The synthetic ruleset now carries a list of one byte length and
  two code point counts to hold that distinction still: reading the rule in code
  points instead of bytes refuses that list, and would refuse bundles the
  reference accepts.
- `loader-when-unreferenced-038` pins the check 16 rule refusing a `WHEN` branch
  no `CHOOSE` reads. It differs from `loader-stray-when-branch-022` at one byte,
  the `root_node` varint, and both expect `invalid_ruleset`, so only a message
  level assertion can tell the two rules apart. Neutralising that one rule makes
  the fixture load clean, which is how it was confirmed that nothing earlier
  refuses it.
- The declared order of a parameter list is refused at **check 13**, not check
  12. `ir.md` section 10 gives check 12 the parameters an operation declares and
  check 13 "the declared order of a parameter list as section 9 states it"; the
  list named the order at `2026.09.1`, and this engine had been refusing the same
  bundles under the wrong number. Both `prefix_in` values and `length_in` lengths
  were already enforced ascending and deduplicated, so the fixture
  `loader-prefix-in-unsorted-039` was refused before this change — at 12 instead
  of 13.
- The emitter no longer sorts a membership list before packing it: it checks the
  order and reports a list that arrives wrong. `ir.md` guarantees the values of
  `prefix_in` are sorted and deduplicated by the compiler so an engine can search
  them without reordering, and the loader already refuses any list `compareUtf8`
  does not find sorted — an order that coincides with the code point order the
  packed search compares, measured over 576 pairs including supplementary
  characters. Sorting again would only have hidden the day that stopped being
  true.
- Earlier, against rules `2026.08.31`. Three rules gained membership checks —
  2 566 German court codes, 148 French greffe codes, and the Luxembourg section
  letter — and the ruleset grew from 99 677 to 120 872 bytes.
- A membership list is emitted as a sorted string constant read by binary search
  rather than an array of arrays walked from the front, and any array literal the
  generator emits is split across methods past five hundred elements. Emitted the
  old way the ruleset's 2 755 membership entries become 2 891 array literals, and
  they overflow the sixty-four kilobyte limit the JVM places on a class
  initialiser: `Constants.kt` reaches 99 892 bytes and the library stops
  compiling. Measured by disabling each mechanism in turn, the splitting alone is
  what restores compilation; the packing is what takes `Constants.kt` to 23 834
  bytes and the lookup off a linear walk.
- `Utf.utf8Length` counts an unpaired surrogate as the one byte the platform
  encoder emits rather than the three its code unit would have taken, which
  `ir.md` section 6 step 1 now names as one of the two answers an engine may give
  and must state. It makes the count equal to `toByteArray(UTF_8).size` for every
  string, stated as a property.
- A `WHEN` checksum branch that no `CHOOSE` reads is refused, matching the
  reference loader, with the program root left to the rule that owns it.

### Measured

- The shared conformance runner, pinned to the commit `rules.lock` records,
  reports 674 of 674 cases matched, 0 differed, against rules `2026.09.0`.
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

# Working in this repository

## One command verifies everything

```sh
./scripts/verify.sh
```

That is the entry point `engine.md` section 12.6 requires, and it is what CI
calls, so "green" never has two definitions. It runs the lock digests, the
regeneration of the emitted sources, compilation, tests, the shared conformance
suite against the runner from `spec`, lint, format, coverage and its thresholds,
packaging including both consumer projects, and the far end of the supported JDK
range.

Its contract:

- **success prints one line**, carrying the rules version, the conformance
  tally, the test count, hand-written coverage and the published jar size;
- **failure prints the failing step's output and only that**, named;
- the exit code is non-zero the moment a step fails.

**Prefer it to running the pieces.** A resync round driven by hand costs about
thirty commands and thirty full outputs, twenty-nine of which say only "this
passes". One command that is silent when all is well makes complete verification
cheaper than partial verification, which is the point.

## Why the pieces lie, and this does not

Three traps, all of which have bitten this repository:

- **A Gradle task that is up to date replays a verdict it never recomputed.**
  Detekt reported success on a commit CI refused, twice, because its verdict
  depends on the daemon's JVM version, which it never declares as an input.
  Android lint passed a stale pin from a warm directory for the same reason.
- **`--rerun` is a task option that binds to the task it follows.**
  `a b c --rerun` forces `c` and says nothing about `a` or `b`.
- **`--rerun` does not reach a lifecycle task's dependencies.** Asking for
  `assemble --rerun` re-runs nothing, because `assemble` has no actions of its
  own.

So `verify.sh` forces each step that owns a verdict, and then requires it to have
left evidence newer than the run. A step that was skipped, or replayed, fails the
whole command instead of passing quietly. A step that *cannot* run — the Android
consumer without an SDK — is a failure too, never an omission: the line printed
on success claims that work was done.

## The specification is upstream

`spec/` is a verbatim copy of the specification repository, pinned by
`rules.lock` to a digest and a commit. **Never edit anything under `spec/` by
hand** — not the bundle, not the corpus, not the schemas, not the prose.
Problems with the specification are reported upstream and recorded in
`SPEC-ISSUES.md`; a resync replaces the whole directory and re-verifies the eight
digests.

**`spec/PROVENANCE.md` is the exception to where these files come from, and not
to the rule above.** It is written downstream rather than copied, because it
describes *this* engine's copy: which commit, which rules version, what to build.
It still has exactly one writer — `tools/write_provenance.sh` from the
specification checkout, called by `.github/workflows/rules-sync.yml` — because it
had two and they drifted, and a test now fails if it names a version or a commit
`rules.lock` does not. Editing it by hand puts the second writer back.

## The engine fetches the release

`.github/workflows/rules-sync.yml` is `engine.md` section 11.4: on a daily clock
and on demand it compares the newest `spec` release to `rules.lock`, and when
they differ it downloads the artefacts, verifies `SHA256SUMS` and then the
provenance attestation, writes `spec/` — the prose contracts included, which
section 11.4 step 3 names outright — `rules.lock` and `spec/PROVENANCE.md`,
regenerates the emitted code, runs `./scripts/verify.sh` and opens a pull
request. Nothing reaches the working tree before the attestation verifies.

Because a pull request opened with the repository's own `GITHUB_TOKEN` starts no
`pull_request` workflow, `ci.yml` never runs on one of these. The workflow has
already run the entry point on that tree, so it publishes the result as the
commit status `Verify` — the name the branch protection requires. That protection
must require `Verify` and nothing else: a second required check would give
"green" two definitions, and would never be published on a synchronization pull
request at all.

The pull request is opened green or red. **A red one is never merged to unblock
the chain**: it is corrected, or the release is refused with the reason written
down. Red means the release brought something this engine does not yet do, which
is exactly the case that wanted a human.

## A tag publishes, nothing else does

`.github/workflows/release.yml` is the only thing that reaches Maven Central, and
only a tag starts it. It uploads to the Central Portal through the publisher API
with `curl` — no plugin runs with the signing key in scope — and as a
`USER_MANAGED` deployment, which stops at `VALIDATED` and waits for a person.
`engine.md` section 11.4: merging verified code and publishing a package are not
the same act, and the second is the only irreversible one.

The published groupId is `org.entid`, verified on the Central Portal against the
domain `entid.org` by a DNS TXT record. It happens to equal the Kotlin package
namespace and is still a separate decision: **a groupId cannot be changed after
a first publication without breaking every consumer**, so `PackagingTest` freezes
it rather than leaving it to a property file, and `ReadmeTest` holds the install
snippets to whatever the build publishes. The `io.github.<account>` route was
rejected on purpose — it needs an account whose name stays claimed, and the
previous one was released when the organisation was renamed.

`CONTRIBUTING.md` names the four secrets and the one namespace verification a
human has to do; nothing here invents a fifth.

## Rules versions

Nothing here compares rules versions for order — `PATCH` in `YYYY.MM.PATCH` is a
counter within a month with no upper bound, so `2026.08.32` legitimately follows
`2026.09.0`. Versions are compared for equality against `rules.lock` and never
sorted. The published Maven version is independent and is SemVer.

## Conventions

- Code, commits and documentation in English.
- A bug is proved by a test that fails before it is fixed.
- Never invent a business identifier; real ones come from the issuer's register.
- Invent no semantics the specification does not state. If something is
  ambiguous, stop and report it rather than choosing silently.

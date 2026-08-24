# Working in this repository

## One command verifies everything

```sh
./scripts/verify.sh
```

That is the entry point `engine.md` section 12.5 requires, and it is what CI
calls, so "green" never has two definitions. It runs the lock digests, the
regeneration of the emitted sources, compilation, tests, the shared conformance
suite against the runner from `spec`, lint, format, coverage and its thresholds,
and packaging including both consumer projects.

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
`rules.lock` to a digest and a commit. **Never edit anything under `spec/`.**
Problems with the specification are reported upstream and recorded in
`SPEC-ISSUES.md`; a resync replaces the whole directory and re-verifies the eight
digests.

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

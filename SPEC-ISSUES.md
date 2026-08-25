# Questions for the specification

What the Kotlin engine measured that the specification does not settle, or
settles in two places that disagree. Each entry says what was measured, what
this engine does meanwhile, and what it proposes.

Nothing here blocks the release: every item has a reading the engine follows,
and the whole corpus passes under it.

## Open

### `tools/check_lock.sh` verifies seven of the eight lock digests

**Measured**, at `70c408b`, the attested source commit of `v2026.08.38`.
`engine.md` section 16 fixes the twelve `rules.lock` fields and says the list is
normative because *un champ qu'un écrivain porte et qu'un autre omet est une
release que les moteurs refusent* — naming `conformance_jsonl_sha256` as the
field that already caused it. `tools/check_lock.sh`, which a synchronization runs
from the attested commit against the lock it has just assembled, has seven
`expect_sha` calls for eight `*_sha256` fields, and the one it skips is that one:

```text
$ sed -i 's/^conformance_jsonl_sha256 = ".*"/…= "0000…0000"/' rules.lock.bad
$ tools/check_lock.sh rules.lock.bad ./artifacts 2026.08.38 ; echo exit=$?
ok rules_sha256 … ok features_doc_sha256 … ok attestation identity …
exit=0

$ ./scripts/verify-lock.sh ; echo exit=$?
MISMATCH spec/entid-conformance.jsonl  declared 0000…0000, actual 448b293e…
exit=1
```

It is the field that legitimately stays put while `conformance_sha256` moves —
the JSONL is the reviewed source and carries no rules version, the compiled
corpus injects one into every expected report — so it is exactly the value a
writer gets wrong by carrying it forward, and exactly the one this gate cannot
see.

**What this engine does.** `scripts/verify-lock.sh` verifies all eight, so the
lock is caught here even when the upstream gate passes it. `v2026.08.38`'s lock
is correct in all eight.

**Proposed**, and filed as [entid-org/spec#95][95]: add the eighth check, or
assert that the set of `*_sha256` fields the lock declares is exactly the set the
script checks, so the next field added to section 16 cannot be silently
unverified either.

[95]: https://github.com/entid-org/spec/issues/95

## Not a specification question, recorded because it shapes the build


### Detekt and ktlint cannot run on a JDK newer than the release they were built against

**Measured.** On a JDK 25 daemon, `detekt` 1.23.8 — the newest stable — stops
with `25.0.4` before reading a line: the Kotlin compiler it embeds refuses a
class file version it does not know. Its Gradle task runs inside the daemon and
offers no launcher to point at another JVM, and the same exposure applies to the
ktlint command line.

**What this engine does.** The analysers run once, in a CI job of their own, on
the JDK the project pins. The compiler and the tests still run across the whole
supported range, so nothing about the code goes unchecked on a newer JDK — only
the analysis of it is pinned. `CONTRIBUTING.md` says the same for a local run.

### Android lint fails a build that has not changed, and passes one that should fail

**Measured.** CI failed the Android consumer on a commit that touched nothing
in it: Google released AGP 9.3.2 and lint's `AndroidGradlePluginVersion`
detector turned a correct pin into an error. The same commit passed locally,
because the nested build directory was warm and `lintDebug` was up to date.
The detector's verdict depends on a network index of released versions that it
never declares as a task input, so its up-to-date check cannot be sound: a warm
directory replays a pass that a cold one would refuse.

**What this engine does.** The pin moves with the release rather than the check
being disabled, since a consumer project exists to prove the artefact builds
under the toolchain people actually have. Both consumer projects are standalone
builds absent from the root settings file, which is why dependabot at `/` never
saw them; they are named explicitly now, so the next release arrives as a pull
request instead of as a red build on an unrelated change. The lint tasks are
marked always out of date, so a local run means what it says — verified by
re-pinning to 9.3.0 against a warm directory and watching it fail, which it
would previously have skipped.

### A workflow that no pull request could run, and a job that had never passed

**Measured.** The scheduled workflow runs on `schedule` and `workflow_dispatch`
only, so nothing in it was ever exercised before merging. Its first and only
scheduled run failed twice over. The benchmarks job passed JMH a repository
relative `-rff benchmarks/build/jmh.json`, but JMH resolves a relative result
path against the working directory of its own process, which for a `JavaExec`
task is the project directory: it aimed at `benchmarks/benchmarks/build/jmh.json`
and refused with "Can not touch the result file" before running a single
benchmark. Reproduced locally, and confirmed by creating the doubled directory
and watching the run succeed with the results landing there — where the upload
step, which looks at the other path, would have collected nothing. That path was
wrong from the day the workflow was written; the job had never passed.

The same run failed the toolchain matrix on JDK 25 because it ran `./gradlew
build`, which pulls in `check` and so detekt. `ci.yml` runs `test coverage
assemble` across its matrix precisely to avoid that, and the two workflows had
drifted apart. Reproduced on JDK 26 — and the first attempt reported success
because the detekt task was up to date, which is the third time in this
repository a warm task directory has replayed a stale pass.

**What this engine does.** The result file is chosen by the build, which knows
its own build directory; an absolute path cannot be doubled. The toolchain job
runs the same tasks as its `ci.yml` counterpart. The scheduled workflow now also
runs on pull requests that touch it or the benchmarks, so the next breakage of
either is visible before it is merged rather than on a Monday.

### `Toolchain 25` ran, passed, and measured nothing

**Measured.** `ci.yml` carried a job named `Toolchain 25` and `scheduled.yml` a
matrix of `17`, `21` and `25`, both setting `java-version` to the JDK under test.
That moves the **Gradle daemon**. It does not move the compiler or the test JVM:
`entid.kotlin-base` sets `jvmToolchain(17)`, which binds every `JavaCompile`
and every `Test` to a launcher for the pinned JDK whatever the daemon is. Asked
directly, with the daemon on the newest JDK installed here:

```text
$ JAVA_HOME=…/temurin-26.jdk/Contents/Home ./gradlew --info :entid:test …
Starting process 'Gradle Test Executor 1'. Command: …/java/17.0.20-tem/bin/java
```

Four named runs across two workflows, one JDK ever exercised, and nothing could
have failed to say so. The one thing those jobs did prove — that the build
configures under a newer daemon — is not what either of them was named after, and
is contradicted by the entry above: the analysers cannot run on such a daemon at
all.

**What this engine does.** `-Pentid.toolchain=N` moves the toolchain and
leaves the daemon alone, which is the way round that matches what the range
means: the library's bytecode targets 11 and has to load and behave on the far
end, while detekt and ktlint have to run on the JDK they were built against.
`scripts/verify.sh` runs the whole suite on the pinned JDK and the test suite
again on the far end, so the entry point of section 12.6 covers the range and the
`ci.yml` job is gone. The weekly matrix keeps all three, through the property.

A run on another toolchain builds into `build/jdk<n>/`. Sharing the directory
would have let `test` — which depends on `jar` — leave a jar compiled by a
toolchain this project does not ship with where the packaging step reads one, and
would have let the pinned run's own test results stand as evidence that the other
step ran at all.

## Settled upstream

### 1. `engine.md` section 9.1 contradicted itself on an out of bounds access — clause removed

**Raised here, and it was the observable kind.** The section said an out of
bounds view is an absent value and never an exception, then said an out of bounds
access in a checksum after a valid format must produce an engine error. Two
engines could answer differently on real input, and no conformance case would
have caught it because no rule in the published ruleset reaches out of bounds.

`engine.md` section 9.1 now points at `ir.md` section 1.1 and the clause is gone.
The intuition behind it survives as what it was — a format rule is expected to
establish the bounds its checksum assumes — stated as a property of a ruleset
rather than as a runtime behaviour, because nothing proves it at load time.

This engine followed `ir.md`, which the specification records as the correct
reading. Nothing changed here.

### 2. The UTF-8 length of ill formed text — the freedom is now stated and bounded

**Raised here, with the measurement.** The input bound is counted in UTF-8 bytes
and is checked before the encoding, and ill formed text has no UTF-8 encoding, so
the count had to be invented. This engine measured both answers rather than
assuming one: `String.getBytes(UTF_8)` on a lone high surrogate returns a single
byte, `0x3F`; the encoding that surrogate would have had is three.

`ir.md` section 6 step 1 now states the freedom and bounds it the way check 14
before check 15 is bounded: an engine chooses, **MUST state which**, both answers
are `unsupported`, and no conformance case can carry such an input.

**What this engine chose, and where it says so.** It counts what its own encoder
produces — one byte per unpaired surrogate. This engine previously counted three,
and changed: counting what the encoder produces turns the rule from a convention
into an invariant, `Utf.utf8Length(s) == s.toByteArray(UTF_8).size` for every
string, which `UtfTest` states as a property over generated input rather than
over chosen examples. The choice is stated in the KDoc of `Utf.utf8Length` and in
a section of its own in `README.md`, not only in a test.

### 3. A `WHEN` no `CHOOSE` reads — the reference loader accepted it, and now refuses it

**Raised here, and this one went the other way.** This engine refused a `WHEN`
that nothing reads; the reference loader accepted it. Check 16 takes `WHEN` only
as a direct operand of a `CHOOSE`, and that loader enforced it by looking at each
node's parents — a node with no parent has none to look at, and section 2 permits
unreachable nodes, so nothing else caught it.

The reference loader is fixed, with the program root excluded from the scan
because `root_node` is a reference rather than an operand and a program rooted in
a `WHEN` keeps its own rule and its own message.

**This engine matches that exclusion.** The root case is answered earlier, by the
rule that owns it, with the message `checksum program N roots at a when branch`;
a branch nothing reads is answered by the new rule, with `program N node M is a
when branch no choose reads`. Both are check 16, and
`LoaderRefusalTest` asserts each message separately so the two rules cannot
collapse into one.

### 4. `tools/write_provenance.sh` now ships with the release it describes

**Raised here.** Section 11.4 step 3 has the engine write `spec/PROVENANCE.md`,
and `tools/write_provenance.sh` was its single writer since #84 — but both
releases published at the time were cut before that change, so a synchronization
that pins the specification checkout to the attested commit (the only commit it
has any reason to trust) found no writer there. This engine took that one script
from the default branch, warned, and recorded both commits in the pull request
body.

**Settled by construction.** `70c408b`, the attested source commit of
`v2026.08.38`, carries `tools/write_provenance.sh`. The fallback did not fire in
this synchronization and stays in the workflow for a re-run pinned to an older
release. The script itself is now a `cp` of `provenance-<engine>.md` out of the
release, which is the stronger form of the same fix: the note is assembled by the
compiler, attested with everything else, and an engine no longer has to clone
`spec` to write the last file of its sync.

### 5. `PROVENANCE.md` no longer claims the lock says something it does not

**Raised here.** The "Verifying integrity" paragraph ended with *`rules.lock`
carries no `attestation_identity` because no release exists yet; its header
explains this* — false in the committed tree of every engine that had
synchronized from a release, and false in the paragraph a reader consults to
decide what to check.

**Settled upstream.** The paragraph now describes both cases and keys off whether
the field is present: *`attestation_identity` names the workflow and tag that
produced these files, and its presence is what distinguishes an attested release
from a local build.* Which is what was proposed.

### 6. `engine.md` no longer numbers two different sections 12.5

**Raised here.** At `2026.08.32` the document carried both

```text
### 12.5 Mutation testing
## 12.5 Une seule commande, silencieuse quand tout passe
```

at different depths, so a citation of "section 12.5" was ambiguous and this
repository had to guess which one it meant.

**Settled upstream.** The entry point is `### 12.6 Une seule commande,
silencieuse quand tout passe`, at the same depth as its neighbours, and mutation
testing keeps 12.5. Every citation in this repository was retargeted with this
synchronization; `README.md` still cites 12.5 for mutation testing, which is now
unambiguous.

### 7. "the newest release" is no longer the endpoint that cannot see any release there is

**Raised here**, and filed as [entid-org/spec#93][93]. Section 11.4 had the
engine compare *la dernière release de `spec`* to its `rules.lock`. The endpoint
that phrase names — `/releases/latest`, which is what `gh release view` reads
with no tag — excludes pre-releases, and `release.yml` marks every ruleset that
is not `stable` as one. Every release published so far is a pre-release, so a
synchronization built on that endpoint would have had nothing to do for the whole
alpha phase, and would have looked perfectly healthy doing it.

**Settled upstream.** Section 11.4 now carries a paragraph of its own: *La plus
récente, pas « latest »* — the workflow lists the releases and takes the newest
that is not a draft, pre-release or not. Which is what this engine's
`rules-sync.yml` already did, and now it does it because the specification says
so rather than in spite of it.

[93]: https://github.com/entid-org/spec/issues/93

### 8. `tools/sync_engines.sh` refuses to overwrite an attested lock

**Raised here, after it happened to this checkout.** A developer run left the
working tree with a pre-release lock: `attestation_identity` gone and
`source_commit` rewound to a commit no release was built from — which is also
what pins the conformance runner, so the corpus would have been judged by a
comparator from another commit. Nothing warned, and the digests still matched, so
`verify-lock.sh` passed on either.

**Settled upstream**, as proposed. The script now refuses when the `rules.lock`
it is about to replace carries an `attestation_identity`, and `--force-local`
is what steps off the attested path on purpose, with a warning. The reason is
written beside the check, crediting the measurement.

## Settled upstream, earlier


### `loader-call-cycle-014` and `loader-unknown-call-target-015` are invalid twice over — and the order saves them

**Measured, not a defect.** Both fixtures add a `CALL_OP_KIND_CHECKSUM` node to
the reference ruleset without adding `CAPTURES_AND_CALLS_V1` (11) to
`required_feature_ids`. Each is therefore refusable by check 25 as well as by the
check its name targets.

Check 24 runs before check 25, so both are refused at 24, which is what their
names say and what this engine asserts. The observation is recorded because the
protection is the ordering rather than the fixtures: were check 25 ever to move
ahead of 24, both cases would still pass while proving nothing.

No change proposed. `LoaderFixtureTest` asserts the check number of all
thirty-five fixtures, so a reordering would be caught here.

### The five fixtures that were invalid for more than one reason

Nothing found, in either round. All thirty-five `load_ruleset` fixtures were
decoded byte for byte at rules `2026.08.26` and again at `2026.08.31`, and each
is refused at exactly the check its name targets, with the error kind the corpus
expects. The two above are the only ones with a second fault, and the order of
the checks resolves them.

### `dispatch-unsupported-country-006` does not test an unsupported country

**Cosmetic, and the expectation is right.** The case sends `vat` with the country
`UK`, which the dispatcher aliases to `GB`; the value then resolves and is judged
invalid on length by the British rule. Its description says exactly that — *"A
country alias resolves to its target"* — and only the identifier reads as
something else. `dispatch-unsupported-country-007`, the JP one beside it, is the
case the shared name belongs to.

Worth renaming the day the corpus is touched for another reason. Nothing depends
on it, and this engine passes it as written.

### The expansion count of the published ruleset

`ir.md` section 2 states both figures, and this engine measures both. At rules
`2026.08.26`: **3069** operation instances under the reachable-root reading and
**3204** under the reading that sums every capture — the figure the document says
two engines reported. At `2026.08.31`, after the three membership rules were
added: **3094** and **3229**. `PublishedRulesetTest` asserts both, so the correct
reading cannot regress into the other without a test saying so.

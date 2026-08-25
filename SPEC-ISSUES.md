# Questions for the specification

What the Kotlin engine measured that the specification does not settle, or
settles in two places that disagree. Each entry says what was measured, what
this engine does meanwhile, and what it proposes.

Nothing here blocks the release: every item has a reading the engine follows,
and the whole corpus passes under it.

## Open

### "the newest release" is the one endpoint that cannot see any release there is

**Measured.** `engine.md` section 11.4 has the engine compare *la dernière
release de `spec`* to its own `rules.lock`. The endpoint that phrase names —
`/repos/libbusinessid/spec/releases/latest`, which is what `gh release view`
reads with no tag — excludes pre-releases, and `release.yml` marks every ruleset
whose stability is not `stable` as a pre-release, deliberately, so that "a
consumer or a downstream script never picks it up by accident". Both releases
published so far are pre-releases. Run against `/latest` today:

```text
$ gh release view --repo libbusinessid/spec
release not found
```

A synchronization built on it would have nothing to do for as long as the ruleset
stays alpha, and would look perfectly healthy doing it.

**What this engine does.** `.github/workflows/rules-sync.yml` lists
`/repos/{owner}/{repo}/releases` and takes the newest non-draft entry by
publication date, pre-release or not, and says why in a comment. The safety
`release.yml` wants is real but it is not this repository's: a pre-release
arrives here as a pull request that a human reads, not as a published artefact.

**Proposed.** Say in 11.4 which release is meant while the ruleset is alpha —
either "the newest published release, pre-releases included" or "the newest
stable release, and nothing before the ruleset is stable".

### the only writer of `PROVENANCE.md` postdates every release it has to describe

**Measured.** Section 11.4 step 3 has the engine write `spec/PROVENANCE.md`, and
`tools/write_provenance.sh` is its single writer since #84 — the change that
fixed it having had two. Both published releases were cut before that change:

```text
$ git ls-tree --name-only b264614 tools/ | grep provenance
tools/check_provenance.sh
```

`b264614` is the source commit of `v0.1.1`, read from its signing certificate. A
downstream synchronization that pins the specification checkout to the attested
commit — which is the only commit it has any reason to trust — finds no writer
there, for `v0.1.0` and `v0.1.1` alike. Its inputs are all present and
unchanged: `docs/spec/provenance/body.md`, `kotlin.md` and
`docs/generated/coverage.md` are byte identical between `b264614` and the current
default branch.

**What this engine does.** It pins the checkout to the attested commit, and takes
`tools/write_provenance.sh` alone from the default branch when that commit
predates it, warning and recording both commits in the pull request body. Every
input the note quotes stays pinned to the release. Refusing instead would mean no
note at all, and the run reproduces the committed `spec/PROVENANCE.md` byte for
byte.

**Proposed.** Nothing, if the next release fixes it by construction — any tag cut
after #84 carries the writer. Worth a line in 11.4 saying the writer is taken
from the release, so that no engine invents a second one.

### `PROVENANCE.md` tells the reader the lock says something it does not

**Measured.** `docs/spec/provenance/body.md` ends with

```text
`rules.lock` carries no `attestation_identity` because no release exists yet;
its header explains this.
```

Two releases exist, this repository synchronized from one, and its `rules.lock`
carries `attestation_identity = "libbusinessid/spec/.github/workflows/release.yml@refs/tags/v0.1.1"`
and no header at all. The sentence is already false in the committed tree, and it
is the paragraph titled "Verifying integrity" — the one a reader consults to
decide what to check.

**What this engine does.** Nothing: it copies the note as assembled, because the
alternative is a second writer. The two figures a test does check — rules version
and source commit — are correct.

**Proposed.** Make that paragraph describe both cases, or key it off whether the
lock carries the field.

### `tools/sync_engines.sh` overwrites an attested lock with a pre-release one

**Measured.** In this checkout, `rules.lock` at `HEAD` names
`source_commit = "b264614…"` and the attested identity of `v0.1.1`. A developer
run of `tools/sync_engines.sh` left the working tree with the pre-release
template instead: the header saying no release has been tagged yet,
`attestation_identity` gone, and `source_commit` rewound to the specification's
local `HEAD` — a commit no release was built from. `spec/PROVENANCE.md` followed
it. Nothing warned; the digests still matched, so `verify-lock.sh` passes on
either.

**What this engine does.** It does not commit that regression, and its
synchronization workflow rebuilds the lock from the attested manifest, so a later
sync repairs it. But the lock also pins the conformance runner, and a lock that
silently rewinds it makes the corpus judged by a comparator from another commit.

**Proposed.** Have `sync_engines.sh` refuse, or at least warn, when the
`rules.lock` it is about to replace carries an `attestation_identity`: after the
first release it is downgrading a verified pin to an unverified one.

### `engine.md` numbers two different sections 12.5

**Measured.** At `2026.08.32` the document carries both

```text
### 12.5 Mutation testing
## 12.5 Une seule commande, silencieuse quand tout passe
```

They are different sections at different depths, and the second was added by the
change that introduced the single entry point. A citation of "section 12.5" is
now ambiguous, and this repository has to make one: `CLAUDE.md` and `README.md`
both point at 12.5 for the entry point, while `README.md` already pointed at 12.2
for the coverage split from the same run of subsections.

**What this engine does.** It reads 12.5 as the entry point section, because that
is the one the sentence about `CLAUDE.md` belongs to, and cites mutation testing
by name rather than by number where it needs to. Nothing depends on the number
being right, so this is a documentation defect and not a behavioural question.

**Proposed.** Renumber the new section, or renumber mutation testing — either
way, one number per section.

The three questions this engine raised against rules `2026.08.26` were all
settled in `2026.08.31`; they are below, with what changed.

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

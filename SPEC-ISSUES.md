# Questions for the specification

What the Kotlin engine measured that the specification does not settle, or
settles in two places that disagree. Each entry says what was measured, what
this engine does meanwhile, and what it proposes.

Nothing here blocks the release: every item has a reading the engine follows,
and the whole corpus passes under it.

## Open

Nothing. The three questions this engine raised against rules `2026.08.26` were
all settled in `2026.08.31`; they are below, with what changed.

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

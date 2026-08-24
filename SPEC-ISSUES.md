# Questions for the specification

What the Kotlin engine measured that the specification does not settle, or
settles in two places that disagree. Each entry says what was measured, what
this engine does meanwhile, and what it proposes.

Nothing here blocks the release: every item has a reading the engine follows,
and the whole corpus passes under it.

## Open

### 1. `engine.md` section 9.1 contradicts itself, and `ir.md`, on an out of bounds access in a checksum

**Measured.** The tension is inside the section, between its two sentences.
`engine.md` section 9.1 opens with *« Une vue hors limites produit une valeur
absente, jamais une exception »* and closes with *« un accès hors limites dans un
checksum après format valide indique un bundle invalide et doit produire une
erreur moteur »*. The first says absent, the second says engine error, of the
same access.

`ir.md` sides with the first, in three places: section 1.1, *"Absence is never an
error and never an exception"*; section 1.2, an index outside a remainder table
*"makes the enclosing checksum node evaluate to `unsupported`"*; and the
descriptions of `COMPARE_DIGIT`, `COMPARE_SLICE` and `SLICE`, each of which is
*"indeterminate when … `index` is out of range"*.

The two readings are observable and differ on real input. Under `engine.md` a
value whose format holds but whose checksum slice runs past the end raises an
engine error; under `ir.md` it answers `unsupported`/`unsupported_checksum`.

**What this engine does.** It follows `ir.md`. Three reasons, in order: `ir.md`
is designated by `engine.md` section 1.1 as the exhaustive specification of every
opcode, so it is the more specific document; an engine error would turn a value
the rules simply cannot speak about into a failure, against priority 1 of section
2; and `engine.md`'s own sentence reads as a diagnosis of what such an access
*indicates* rather than as a rule about what to answer.

The guard is still there and still typed: `guardEngineErrors` turns an
`ArithmeticException` or an `IndexOutOfBoundsException` escaping a program into a
`BusinessIdEngineException`. It never fires, because the load checks prove no
emitted arithmetic can overflow and every view constructor answers absence rather
than throwing.

**Proposed.** Drop the second sentence of `engine.md` section 9.1, or reduce it
to what it seems to mean: such an access *suggests* a ruleset whose format rule
does not establish the bounds its checksum assumes, which is worth saying, and
is not an instruction about what to answer. An engine error stays reserved for an
invariant the load checks were supposed to have made impossible.

### 2. The UTF-8 length of ill formed text is unspecified, and the order of the two bounds makes it observable

**Measured.** `ir.md` section 6 orders the input bound before the encoding check:
step 1 refuses an input above 1024 UTF-8 bytes, and step 1 of section 5 refuses
text that is not valid UTF-8. An unpaired surrogate has no UTF-8 encoding, so for
a Kotlin `String` that is both ill formed and near the bound, "its length in
UTF-8 bytes" has no defined value, and the two checks answer differently
depending on the count chosen.

This engine counts an unpaired surrogate as three bytes, which is what its code
unit would take and the only count that does not depend on a replacement policy.
The alternative was measured rather than assumed: `String.getBytes(UTF_8)` on a
lone high surrogate returns a single byte, `0x3F`, having substituted a question
mark. That reading moves the boundary by two bytes per surrogate.

**What this engine does.** Three bytes, documented at `Utf.utf8Length`. The
difference is reachable only by input that is both ill formed and within a few
bytes of 1024, and no conformance case can carry either, since a proto3 `string`
is valid UTF-8 by definition.

**Proposed.** One sentence in `ir.md` section 6: the byte length of ill formed
text is counted as if each unpaired code unit encoded to its own length, or the
encoding check moves ahead of the bound. Either settles it.

### 3. Whether a `WHEN` checksum branch nothing reads is refused

**Measured.** `ir.md` says `CHECKSUM_OP_KIND_WHEN` *"is accepted only as a direct
operand of `CHOOSE`"*. A `WHEN` node that no node reads at all — dead, reachable
from no root — is not an operand of `CHOOSE`, so the sentence read literally
refuses it. But section 2 explicitly allows dead nodes: *"a node no root reaches
costs nothing: a generator does not emit dead code"*, and check 14 counts only
reachable ones.

**What this engine does.** It refuses a `WHEN` that is a program root or that any
non-`CHOOSE` node reads, and accepts one nothing reads. The corpus fixture
`loader-stray-when-branch-022` makes it the root, which this refuses at check 16
as its name requires.

The choice is the narrower refusal, per priority 1: refusing a ruleset for a node
nothing evaluates would be refusing something no engine can observe.

**Proposed.** Add to check 16 whether the restriction is stated over reachable
nodes or over every node. Both readings agree on every ruleset an author would
write, which is exactly why it will be decided by accident otherwise.

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

## Settled upstream

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

Nothing found. All thirty-five `load_ruleset` fixtures of rules `2026.08.26` were
decoded byte for byte, and each is refused at exactly the check its name targets,
with the error kind the corpus expects. The two above are the only ones with a
second fault, and the order of the checks resolves them.

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

`ir.md` section 2 states both figures, and this engine measures both:
**3069** operation instances under the reachable-root reading, and **3204** under
the reading that sums every capture — the figure the document says two engines
reported. `PublishedRulesetTest` asserts both, so the correct reading cannot
regress into the other without a test saying so.

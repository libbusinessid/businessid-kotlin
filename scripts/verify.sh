#!/usr/bin/env bash
# Copyright The EntID Authors.
# SPDX-License-Identifier: Apache-2.0
#
# The single entry point of `engine.md` section 12.5.
#
# Runs the whole verification — lock digests, regeneration of the emitted code,
# compilation, tests, conformance against the runner from `spec`, lint, format,
# coverage and its thresholds, packaging — and holds to one contract:
#
#   * success prints ONE line, carrying the numbers that matter;
#   * failure prints the output of the failing step and only that, named;
#   * the exit code is non zero the moment a step fails, and is never swallowed.
#
# Two things this must not do, because this engine has been bitten by both.
#
# A Gradle task that is up to date replays a verdict it never recomputed: that is
# how detekt reported success on a commit CI refused, twice, and how a warm
# directory made Android lint pass a pin it should have failed. So every step
# that owns a verdict is forced to execute with `--rerun`, and afterwards each
# step must show evidence newer than this run. A step that did not run fails the
# whole thing rather than passing quietly.
#
# And a step that cannot run must say so rather than be skipped. The Android
# consumer needs an SDK; without one it is a failure here, not an omission.
#
# What is deliberately not here: the random fuzzing search. `test` already runs
# every Jazzer target over its committed seed corpus, which is a verdict and
# reproducible; the ten seconds of random search on top of it are a search, and a
# search that fails on a different commit each time turns the one command
# everyone runs into something they learn to re run. `ci.yml` keeps it in a job
# of its own and `scheduled.yml` runs it for ten minutes a target.

set -euo pipefail

cd "$(dirname "$0")/.."

readonly WORK="$(mktemp -d)"
readonly MARKER="$WORK/started"
trap 'rm -rf "$WORK"' EXIT
touch "$MARKER"

GRADLE=(./gradlew --console=plain --quiet)

# The two ends of the supported JDK range, read from the file that defines them,
# so the range is not restated here and in two workflows.
readonly CONSTANTS="buildSrc/src/main/kotlin/BuildConstants.kt"
PINNED_JDK="$(sed -n 's/.*TOOLCHAIN_JDK: Int = \([0-9]*\).*/\1/p' "$CONSTANTS")"
FAR_JDK="$(sed -n 's/.*TOOLCHAIN_JDK_MAX: Int = \([0-9]*\).*/\1/p' "$CONSTANTS")"
if [ -z "$PINNED_JDK" ] || [ -z "$FAR_JDK" ]; then
  printf 'verify: cannot read TOOLCHAIN_JDK and TOOLCHAIN_JDK_MAX from %s\n\n' "$CONSTANTS" >&2
  printf 'The supported range has one definition and this command could not find it.\n' >&2
  printf 'Read: pinned "%s", far end "%s".\n' "$PINNED_JDK" "$FAR_JDK" >&2
  exit 1
fi

fail() {
  local name="$1" log="$2" code="${3:-1}"
  printf 'verify: step "%s" failed\n\n' "$name" >&2
  cat "$log" >&2
  exit "$code"
}

# Runs one step with its output captured. Nothing reaches the terminal unless it
# fails, in which case its output is all that does.
step() {
  local name="$1"
  shift
  local log="$WORK/$(printf '%s' "$name" | tr ' /' '__').log"
  # The status is captured before anything else can overwrite it. Inside an
  # `if ! cmd` the `$?` a naive version would read is the status of the
  # negation, which is zero exactly when the command failed — so the failure
  # would be reported and then exited with success. This command exists to make
  # a verdict trustworthy; it does not get to swallow its own.
  local status=0
  "$@" >"$log" 2>&1 || status=$?
  if [ "$status" -ne 0 ]; then
    fail "$name" "$log" "$status"
  fi
}

# A step is not trusted to have run because it exited zero. It has to have left
# something behind that is newer than this invocation.
fresh() {
  local name="$1" pattern="$2"
  local found
  found="$(find $pattern -newer "$MARKER" -type f 2>/dev/null | head -1 || true)"
  if [ -z "$found" ]; then
    printf 'verify: step "%s" left no evidence newer than this run\n\n' "$name" >&2
    printf 'Nothing matching %s was written. The step was skipped, replayed from\n' "$pattern" >&2
    printf 'an earlier verdict, or writes somewhere this check does not look.\n' >&2
    exit 1
  fi
}

# -- the steps, cheapest first so a failure arrives early ---------------------

step "lock digests" ./scripts/verify-lock.sh

step "generated sources" "${GRADLE[@]}" checkGenerated --rerun

step "format" "${GRADLE[@]}" ktlintCheck --rerun

step "lint" "${GRADLE[@]}" detekt --rerun

step "public API" "${GRADLE[@]}" apiCheck --rerun

step "tests" "${GRADLE[@]}" test --rerun
fresh "tests" "entid/build/test-results generator/build/test-results testee/build/test-results"

step "coverage and thresholds" "${GRADLE[@]}" coverage
fresh "coverage and thresholds" "build/reports/coverage-summary.txt"

# Two traps here, both found by the freshness check below refusing to believe
# an exit code of zero.
#
# `assemble` is a lifecycle task with no actions of its own, and `--rerun`
# reaches the tasks named on the command line rather than their dependencies —
# so asking for `assemble` re-runs nothing, and the jar whose size this command
# prints would be whatever an earlier build happened to leave. `:entid:jar`
# is therefore named outright.
#
# And `--rerun` is a task option, not a build option: it binds to the task it
# follows. `a b c --rerun` forces `c` alone and says nothing about the other
# two. It has to be repeated after every task that must actually execute.
step "packaging" "${GRADLE[@]}" :entid:jar --rerun assemble auditPublishedDependencies --rerun
fresh "packaging" "entid/build/libs"

# The Android consumer needs an SDK. Absent, this is a failure and not a shrug:
# the line printed on success claims packaging was verified.
ANDROID_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
if [ ! -d "$ANDROID_SDK" ]; then
  printf 'verify: step "consumers" cannot run\n\n' >&2
  printf 'No Android SDK at %s. Set ANDROID_HOME, or install one: the published\n' "$ANDROID_SDK" >&2
  printf 'artefact is not verified for Android consumers without it, and this\n' >&2
  printf 'command does not report success for work it did not do.\n' >&2
  exit 1
fi
export ANDROID_HOME="$ANDROID_SDK"
step "consumers" "${GRADLE[@]}" consumerJvmTest consumerAndroidTest
fresh "consumers" "consumer/jvm/build/test-results consumer/android/build/test-results"

# Every declared dependency resolves. A moved or withdrawn artefact fails here
# rather than at a release, and this is where "packaging is verified" stops being
# a claim about the jar alone.
step "dependency resolution" "${GRADLE[@]}" resolveAllDependencies

# The runner comes from `spec` and from nowhere else, pinned to the commit
# rules.lock records under source_commit — the same commit as the corpus, so it
# is impossible to judge a corpus with another comparator.
#
# The module path is the one `go.mod` declares at *that commit*, not the one the
# repository answers to today: `go run` compares the two and refuses a path the
# module does not claim. The organisation was renamed to `entid-org`, so this
# says `libbusinessid` for as long as rules.lock pins a commit from before the
# rename, and moves with the next synchronization.
SOURCE_COMMIT="$(grep '^source_commit' rules.lock | cut -d'"' -f2)"
step "conformance testee" "${GRADLE[@]}" :testee:installDist
step "conformance" env GOTOOLCHAIN=auto go run \
  "github.com/libbusinessid/spec/cmd/conformance-runner@$SOURCE_COMMIT" \
  -corpus spec/entid-conformance.binpb \
  -- ./testee/build/install/entid-testee/bin/entid-testee

# The far end of the supported range, and the last step because it is the most
# expensive: a toolchain change recompiles everything into a build directory of
# its own.
#
# This used to be a CI job called `Toolchain 25`, and it never ran anything on
# JDK 25. It set `java-version: 25`, which moves the Gradle daemon; the tests
# followed `jvmToolchain`, which pins them to 17. Measured by asking the daemon
# on a JDK 26 what it launched the test executor with, and getting 17. It is
# here now because the entry point is meant to be the whole verification —
# a branch protection that requires this verdict and nothing else was letting
# every synchronization pull request merge without the range having spoken, and
# what it would have said was nothing anyway.
#
# The toolchain moves and the daemon does not, on purpose: detekt and ktlint
# each embed a Kotlin compiler that refuses a class file version newer than the
# release it was built against, and both run inside the daemon. They had their
# say above, on the JDK this project pins.
step "toolchain $FAR_JDK" "${GRADLE[@]}" test --rerun -Pentid.toolchain="$FAR_JDK"
fresh "toolchain $FAR_JDK" \
  "entid/build/jdk$FAR_JDK/test-results generator/build/jdk$FAR_JDK/test-results testee/build/jdk$FAR_JDK/test-results"

# -- the one line ------------------------------------------------------------
#
# Every number below is read from what this run produced, never from a constant
# repeated here.

RULES="$(grep '^rules_version' rules.lock | cut -d'"' -f2)"
CONFORMANCE="$(sed -n 's/.*: \([0-9]*\) cases, \([0-9]*\) matched, \([0-9]*\) differed.*/\2\/\1/p' "$WORK/conformance.log" | tail -1)"
DIFFERED="$(sed -n 's/.*matched, \([0-9]*\) differed.*/\1/p' "$WORK/conformance.log" | tail -1)"
if [ -z "$CONFORMANCE" ] || [ "${DIFFERED:-1}" != "0" ]; then
  fail "conformance" "$WORK/conformance.log"
fi

TESTS="$(find entid/build/test-results generator/build/test-results testee/build/test-results \
  consumer/jvm/build/test-results consumer/android/build/test-results \
  -name '*.xml' -type f 2>/dev/null |
  xargs sed -n 's/.*<testsuite [^>]*tests="\([0-9]*\)".*/\1/p' 2>/dev/null |
  awk '{n += $1} END {print n + 0}')"

COVERAGE="$(sed -n 's/^hand written *lines *\([0-9.]*\)% .* branches *\([0-9.]*\)%.*/\1%\/\2%/p' \
  build/reports/coverage-summary.txt)"

JAR="$(find entid/build/libs -name 'entid-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' -type f | head -1)"
JAR_BYTES="$(wc -c <"$JAR" | tr -d ' ')"

printf 'verify ok — rules %s · conformance %s · tests %s · toolchains %s+%s · coverage %s · jar %s B\n' \
  "$RULES" "$CONFORMANCE" "$TESTS" "$PINNED_JDK" "$FAR_JDK" "$COVERAGE" "$JAR_BYTES"

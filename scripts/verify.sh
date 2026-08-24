#!/usr/bin/env bash
# Copyright The LibBusinessID Authors.
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

set -euo pipefail

cd "$(dirname "$0")/.."

readonly WORK="$(mktemp -d)"
readonly MARKER="$WORK/started"
trap 'rm -rf "$WORK"' EXIT
touch "$MARKER"

GRADLE=(./gradlew --console=plain --quiet)

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
fresh "tests" "businessid/build/test-results generator/build/test-results testee/build/test-results"

step "coverage and thresholds" "${GRADLE[@]}" coverage
fresh "coverage and thresholds" "build/reports/coverage-summary.txt"

# Two traps here, both found by the freshness check below refusing to believe
# an exit code of zero.
#
# `assemble` is a lifecycle task with no actions of its own, and `--rerun`
# reaches the tasks named on the command line rather than their dependencies —
# so asking for `assemble` re-runs nothing, and the jar whose size this command
# prints would be whatever an earlier build happened to leave. `:businessid:jar`
# is therefore named outright.
#
# And `--rerun` is a task option, not a build option: it binds to the task it
# follows. `a b c --rerun` forces `c` alone and says nothing about the other
# two. It has to be repeated after every task that must actually execute.
step "packaging" "${GRADLE[@]}" :businessid:jar --rerun assemble auditPublishedDependencies --rerun
fresh "packaging" "businessid/build/libs"

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

# The runner comes from `spec` and from nowhere else, pinned to the commit
# rules.lock records under source_commit — the same commit as the corpus, so it
# is impossible to judge a corpus with another comparator.
SOURCE_COMMIT="$(grep '^source_commit' rules.lock | cut -d'"' -f2)"
step "conformance testee" "${GRADLE[@]}" :testee:installDist
step "conformance" env GOTOOLCHAIN=auto go run \
  "github.com/libbusinessid/spec/cmd/conformance-runner@$SOURCE_COMMIT" \
  -corpus spec/businessid-conformance.binpb \
  -- ./testee/build/install/businessid-testee/bin/businessid-testee

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

TESTS="$(find businessid/build/test-results generator/build/test-results testee/build/test-results \
  consumer/jvm/build/test-results consumer/android/build/test-results \
  -name '*.xml' -type f 2>/dev/null |
  xargs sed -n 's/.*<testsuite [^>]*tests="\([0-9]*\)".*/\1/p' 2>/dev/null |
  awk '{n += $1} END {print n + 0}')"

COVERAGE="$(sed -n 's/^hand written *lines *\([0-9.]*\)% .* branches *\([0-9.]*\)%.*/\1%\/\2%/p' \
  build/reports/coverage-summary.txt)"

JAR="$(find businessid/build/libs -name 'businessid-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' -type f | head -1)"
JAR_BYTES="$(wc -c <"$JAR" | tr -d ' ')"

printf 'verify ok — rules %s · conformance %s · tests %s · coverage %s · jar %s B\n' \
  "$RULES" "$CONFORMANCE" "$TESTS" "$COVERAGE" "$JAR_BYTES"

#!/usr/bin/env bash
# Copyright The EntID Authors.
# SPDX-License-Identifier: Apache-2.0
#
# Verifies the eight digests rules.lock declares against the files under spec/.
#
# A note on conformance_jsonl_sha256: it is taken on the decompressed JSONL, and
# it will sometimes stay put while conformance_sha256 moves. That looks exactly
# like the drift it exists to catch and is not: the JSONL is the reviewed source
# and carries no rules version, while the compiled corpus injects one into every
# expected report, so a version bump alone moves one and not the other.

set -euo pipefail

cd "$(dirname "$0")/.."

declare -a PAIRS=(
  "rules_sha256:entid-rules.binpb"
  "conformance_sha256:entid-conformance.binpb"
  "conformance_jsonl_sha256:entid-conformance.jsonl"
  "rules_proto_sha256:rules.proto"
  "conformance_proto_sha256:conformance.proto"
  "testee_proto_sha256:testee.proto"
  "ir_doc_sha256:ir.md"
  "features_doc_sha256:features.md"
)

if command -v sha256sum >/dev/null 2>&1; then
  digest() { sha256sum "$1" | cut -d' ' -f1; }
else
  digest() { shasum -a 256 "$1" | cut -d' ' -f1; }
fi

status=0
for pair in "${PAIRS[@]}"; do
  key="${pair%%:*}"
  file="spec/${pair##*:}"
  declared="$(grep -E "^${key}[[:space:]]*=" rules.lock | cut -d'"' -f2)"
  if [ -z "$declared" ]; then
    printf 'MISSING  %-26s rules.lock declares no %s\n' "$file" "$key"
    status=1
    continue
  fi
  actual="$(digest "$file")"
  if [ "$declared" = "$actual" ]; then
    printf 'ok       %-26s %s\n' "$file" "$actual"
  else
    printf 'MISMATCH %-26s declared %s, actual %s\n' "$file" "$declared" "$actual"
    status=1
  fi
done

if [ "$status" -ne 0 ]; then
  echo
  echo "spec/ and rules.lock disagree: do not generate from this ruleset."
fi
exit "$status"

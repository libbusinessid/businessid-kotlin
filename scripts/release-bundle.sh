#!/usr/bin/env bash
# Copyright The LibBusinessID Authors.
# SPDX-License-Identifier: Apache-2.0
#
# Builds the archive the Central Portal accepts, and refuses to hand over one it
# would reject.
#
#   ./scripts/release-bundle.sh 0.1.0
#
# It needs SIGNING_KEY (an ASCII armoured PGP private key) and SIGNING_PASSWORD
# in the environment: signing is not optional on Maven Central, and a release
# build fails rather than producing an unsigned artefact.
#
# Everything this writes is derived from the staging repository Gradle already
# produces — the same one `verify.sh` publishes into and the consumer projects
# build against. Nothing is assembled by hand.
#
# The check at the end is the point of the script. The Portal is otherwise the
# first thing to see the archive, and it sees it after the tag is pushed: a
# missing `.asc`, a `maven-metadata.xml` that belongs to a repository rather than
# to a deployment, a snapshot version, a POM missing a field Central requires —
# each is a failed deployment discovered at the one moment nothing can be taken
# back. So each is a failure here instead, named.

set -euo pipefail

cd "$(dirname "$0")/.."

VERSION="${1:-}"
if [ -z "$VERSION" ]; then
  printf 'usage: %s <version>\n\n' "$0" >&2
  printf 'The version to publish, without a leading "v". It is the tag, and the\n' >&2
  printf 'release workflow has already checked it against EngineVersion.VALUE.\n' >&2
  exit 2
fi

case "$VERSION" in
  *-SNAPSHOT | *SNAPSHOT*)
    printf 'release-bundle: "%s" is a snapshot\n\n' "$VERSION" >&2
    printf 'Maven Central holds releases only, and every version it holds is final.\n' >&2
    exit 1
    ;;
esac

for name in SIGNING_KEY SIGNING_PASSWORD; do
  if [ -z "${!name:-}" ]; then
    printf 'release-bundle: %s is not set\n\n' "$name" >&2
    printf 'Every file Maven Central accepts is signed. Without a key this build\n' >&2
    printf 'would fail at the signing task; it fails here instead, by name.\n' >&2
    exit 1
  fi
done

# The coordinates have one definition, and it is not this file.
GROUP="$(sed -n 's/^group=//p' gradle.properties)"
ARTIFACT="businessid"
if [ -z "$GROUP" ]; then
  printf 'release-bundle: no group in gradle.properties\n' >&2
  exit 1
fi
GROUP_PATH="${GROUP//.//}"

STAGING="businessid/build/staging-repository"
COMPONENT="$GROUP_PATH/$ARTIFACT/$VERSION"
BUNDLE="$PWD/build/central-bundle.zip"

rm -rf "$STAGING"
mkdir -p "$(dirname "$BUNDLE")"
rm -f "$BUNDLE"

./gradlew --console=plain --quiet -Pversion="$VERSION" \
  :businessid:publishMavenPublicationToLocalStagingRepository

# What goes in: the component directory and nothing above it. `maven-metadata.xml`
# sits one level up and describes a repository's view of every version there —
# Central maintains its own, and an uploaded one is a file the deployment cannot
# account for.
( cd "$STAGING" && find "$COMPONENT" -type f | sort | zip -q -X "$BUNDLE" -@ )

# What must be in it: every artefact, its signature, and the two checksums
# Central requires. Compared as a whole rather than checked one by one, so a file
# nobody expected fails too.
expected() {
  local base
  for base in "$ARTIFACT-$VERSION.jar" \
    "$ARTIFACT-$VERSION-sources.jar" \
    "$ARTIFACT-$VERSION-javadoc.jar" \
    "$ARTIFACT-$VERSION.pom" \
    "$ARTIFACT-$VERSION.module"; do
    # `.asc` files carry no checksum of their own, which is what Central asks
    # for. `.sha256` and `.sha512` are optional there and Gradle writes them.
    local suffix
    for suffix in "" .asc .md5 .sha1 .sha256 .sha512; do
      printf '%s/%s%s\n' "$COMPONENT" "$base" "$suffix"
    done
  done
}

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
expected | sort >"$WORK/expected"
unzip -Z1 "$BUNDLE" | sort >"$WORK/actual"

if ! diff -u "$WORK/expected" "$WORK/actual" >"$WORK/diff"; then
  printf 'release-bundle: the archive is not what the Central Portal accepts\n\n' >&2
  printf -- '-- expected, ++ present:\n\n' >&2
  tail -n +3 "$WORK/diff" >&2
  exit 1
fi

# A signature that is not one is worse than a missing file: Gradle wrote it, so
# the list above is satisfied, and only the Portal would notice.
#
# The whole entry is read rather than piped through `head`, which would close the
# pipe under `set -o pipefail` and fail the check on the size of the signature
# rather than on its content — invisibly, for as long as signatures stay small.
while read -r path; do
  case "$path" in *.asc) ;; *) continue ;; esac
  case "$(unzip -p "$BUNDLE" "$path")" in
    "-----BEGIN PGP SIGNATURE-----"*) ;;
    *)
      printf 'release-bundle: %s is not a PGP signature\n' "$path" >&2
      exit 1
      ;;
  esac
done <"$WORK/actual"

# The POM fields Central requires. Gradle writes them from the `pom` block, and
# a refactor that drops one is a rejected deployment rather than a build error.
POM="$STAGING/$COMPONENT/$ARTIFACT-$VERSION.pom"
for element in name description url licenses developers scm; do
  if ! grep -q "<$element>" "$POM"; then
    printf 'release-bundle: the POM declares no <%s>, which Maven Central requires\n' "$element" >&2
    exit 1
  fi
done
if ! grep -q "<groupId>$GROUP</groupId>" "$POM"; then
  printf 'release-bundle: the POM does not declare the %s namespace\n' "$GROUP" >&2
  exit 1
fi
if ! grep -q "<version>$VERSION</version>" "$POM"; then
  printf 'release-bundle: the POM does not declare version %s\n' "$VERSION" >&2
  exit 1
fi

FILES="$(wc -l <"$WORK/actual" | tr -d ' ')"
BYTES="$(wc -c <"$BUNDLE" | tr -d ' ')"
# Same fallback as verify-lock.sh: coreutils on a runner, the perl tool on macOS.
if command -v sha256sum >/dev/null 2>&1; then
  DIGEST="$(sha256sum "$BUNDLE" | cut -d' ' -f1)"
else
  DIGEST="$(shasum -a 256 "$BUNDLE" | cut -d' ' -f1)"
fi
printf 'bundle ok — %s:%s:%s · %s files · %s B · sha256 %s\n' \
  "$GROUP" "$ARTIFACT" "$VERSION" "$FILES" "$BYTES" "$DIGEST"

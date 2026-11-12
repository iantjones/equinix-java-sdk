#!/usr/bin/env bash
# Builds, verifies, tags and publishes an Equinix Java SDK release (POSIX twin of release.ps1).
#
# Runs the SAME gates as CI before anything is tagged or deployed:
#   1. Preconditions: clean git tree, tag not taken, pom version == release version,
#      and a "## [<version>]" section in CHANGELOG.md.
#   2. ./mvnw clean verify -Pcoverage   default + WireMock suites, JaCoCo gate, javadoc jar
#                                        (doclint), sources jar, shaded MCP server jar
#   3. ./mvnw javadoc:javadoc           javadoc reference gate
#   4. Shaded MCP server jar smoke test (-Dmcp.server.jar.required=true)
#   5. git tag + push, GitHub release with the jars      (unless --skip-github)
#   6. ./mvnw -Prelease deploy -DskipTests               (only with --deploy --gpg-key ID)
#   7. Next development version X.Y.Z+1-SNAPSHOT          (unless --skip-next-snapshot)
#
# Usage: scripts/release.sh [VERSION] [--deploy --gpg-key ID] [--skip-github]
#                           [--skip-next-snapshot] [--dry-run]
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

version="" gpg_key="" deploy=false skip_github=false skip_next=false dry_run=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --deploy) deploy=true ;;
    --gpg-key) gpg_key="${2:?--gpg-key needs a value}"; shift ;;
    --skip-github) skip_github=true ;;
    --skip-next-snapshot) skip_next=true ;;
    --dry-run) dry_run=true ;;
    -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
    -*) echo "Unknown option $1" >&2; exit 2 ;;
    *) version="$1" ;;
  esac
  shift
done

step() { printf '\n=== %s ===\n' "$1"; }
ok()   { printf '  [OK] %s\n' "$1"; }
die()  { printf '  [FAIL] %s\n' "$1" >&2; exit 1; }
mvnw() { echo "  > mvnw $*"; ./mvnw -B -ntp "$@" || die "mvnw $* failed"; }

step "Preconditions"
command -v java >/dev/null || die "java not found on PATH"
command -v git >/dev/null || die "git not found on PATH"
[[ -x ./mvnw ]] || die "Maven wrapper ./mvnw not found"

pom_version="$(./mvnw -q -B help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null)"
[[ -n "$version" ]] || version="${pom_version%-SNAPSHOT}"
[[ "$pom_version" == "$version" ]] || die "pom.xml version is $pom_version; set it to $version and commit first"
tag="v$version"
ok "Release version $version (tag $tag)"

[[ -z "$(git status --porcelain)" ]] || { git status --short; die "Working tree is not clean; commit or stash first"; }
ok "Working tree clean"
[[ -z "$(git tag -l "$tag")" ]] || die "Tag $tag already exists"
ok "Tag $tag is free"
grep -Eq "^## \[${version//./\\.}\]" CHANGELOG.md || die "CHANGELOG.md has no '## [$version]' section"
ok "CHANGELOG.md has a [$version] section"

if $deploy; then
  command -v gpg >/dev/null || die "gpg not found (needed for --deploy)"
  [[ -n "$gpg_key" ]] || die "--gpg-key is required with --deploy"
  # Maven runs in batch mode, where the gpg plugin cannot open a passphrase prompt. Sign once here,
  # before anything is tagged: it proves the key works and leaves the passphrase in gpg-agent.
  echo "release preflight" | gpg --clearsign --local-user "$gpg_key" >/dev/null \
    || die "gpg could not sign with key $gpg_key (passphrase prompt cancelled, or no pinentry)"
  ok "gpg can sign with $gpg_key"
fi
if ! $skip_github && ! command -v gh >/dev/null; then
  echo "  [WARN] gh not found; GitHub release skipped"; skip_github=true
fi

step "CI gates"
mvnw clean verify -Pcoverage
ok "Default + WireMock suites, coverage gate, javadoc/sources/server jars"
mvnw javadoc:javadoc
ok "Javadoc reference gate"
mvnw test -Dtest=ShadedJarSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dmcp.server.jar.required=true
ok "Shaded MCP server jar smoke test"

jars=("equinix-sdk-java-$version.jar" "equinix-sdk-java-$version-sources.jar"
      "equinix-sdk-java-$version-javadoc.jar" "equinix-sdk-java-$version-mcp-server.jar")
for j in "${jars[@]}"; do [[ -f "target/$j" ]] || die "Missing target/$j"; done
ok "Artifacts: ${jars[*]}"

if $dry_run; then step "Dry run: gates passed; nothing tagged, released or deployed"; exit 0; fi

step "Tag"
git tag -a "$tag" -m "Release $version"
git push origin "$tag"
ok "Pushed $tag"

if ! $skip_github; then
  step "GitHub release"
  gh release create "$tag" "${jars[@]/#/target/}" --title "Equinix Java SDK $version" \
    --notes "Equinix Java SDK $version. See CHANGELOG.md for the full list of changes."
  ok "GitHub release $tag"
fi

if $deploy; then
  step "Maven Central"
  echo "  Uses the 'central' server token from ~/.m2/settings.xml."
  mvnw -Prelease deploy -DskipTests "-Dgpg.keyname=$gpg_key"
  ok "Published com.eqixiac.equinix:equinix-sdk-java:$version"
fi

if ! $skip_next; then
  step "Next development version"
  IFS=. read -r major minor patch <<<"$version"
  next="$major.$minor.$((patch + 1))-SNAPSHOT"
  mvnw versions:set "-DnewVersion=$next" -DgenerateBackupPoms=false
  git add pom.xml
  git commit -m "Prepare next development version $next"
  git push
  ok "pom.xml now $next"
fi
step "Release $version complete"

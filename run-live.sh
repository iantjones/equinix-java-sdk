#!/usr/bin/env bash
# Runs a live integration tier with credentials from .env.local (gitignored).
#
# Usage: ./run-live.sh [TIER] [--confirm-destructive] [extra Maven args...]
#   readonly          GET / list / search only (default)
#   dryrun            + dryRun=true CREATES and validate calls (leave nothing behind)
#   dryrun-existing   dryRun=true UPDATES against pre-existing resources (no-op payloads)
#   full              full CRUD: creates and deletes REAL resources   (needs --confirm-destructive)
#   scenario          multi-step scenarios on REAL resources          (needs --confirm-destructive)
#
# Report: target/integration-report.json and target/site/integration-report.html
# (PASS / SKIPPED with reason / SERVER_ERROR / FAIL). run-live.ps1 is the PowerShell twin.
set -euo pipefail
cd "$(dirname "$0")"

tier="readonly"
if [[ $# -gt 0 && "$1" != -* ]]; then tier="$1"; shift; fi
confirm=false
extra=()
for a in "$@"; do
  if [[ "$a" == "--confirm-destructive" ]]; then confirm=true; else extra+=("$a"); fi
done

if [[ ! -f .env.local ]]; then
  echo "Missing .env.local - create it in the repo root with EQUINIX_ACCESS_KEY=... and EQUINIX_SECRET_KEY=... lines." >&2
  exit 1
fi
# Export only the live-tier keys; values are taken verbatim after the first '='.
while IFS= read -r line || [[ -n "$line" ]]; do
  case "$line" in
    EQUINIX_ACCESS_KEY=*|EQUINIX_SECRET_KEY=*|EQUINIX_ACCESS_POLICY_TOKEN=*) export "${line%%=*}=${line#*=}" ;;
  esac
done < .env.local
if [[ -z "${EQUINIX_ACCESS_KEY:-}" || -z "${EQUINIX_SECRET_KEY:-}" ]]; then
  echo "EQUINIX_ACCESS_KEY / EQUINIX_SECRET_KEY not set in .env.local" >&2
  exit 1
fi

case "$tier" in
  readonly|dryrun|dryrun-existing) args=("-Pintegration-$tier") ;;
  full|scenario)
    if ! $confirm; then
      echo "Tier '$tier' creates REAL resources; re-run with --confirm-destructive (sandbox accounts only)." >&2
      exit 1
    fi
    args=("-Pintegration-$tier" "-DconfirmDestructive=true") ;;
  *) echo "Unknown tier '$tier' (use readonly|dryrun|dryrun-existing|full|scenario)" >&2; exit 1 ;;
esac
# Credentials travel as environment variables, never -D properties.
exec ./mvnw -B test "${args[@]}" "${extra[@]+"${extra[@]}"}"

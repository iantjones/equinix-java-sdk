# Runs a live integration tier with credentials from .env.local (gitignored).
#
# Usage: .\run-live.ps1 [-Tier readonly|dryrun|dryrun-existing|full|scenario] [-ConfirmDestructive] [extra Maven args]
#   readonly          GET / list / search only (default)
#   dryrun            + dryRun=true CREATES and validate calls (leave nothing behind)
#   dryrun-existing   dryRun=true UPDATES against pre-existing resources (no-op payloads)
#   full              full CRUD: creates and deletes REAL resources   (needs -ConfirmDestructive)
#   scenario          multi-step scenarios on REAL resources          (needs -ConfirmDestructive)
#
# Report: target/integration-report.json and target/site/integration-report.html
# (PASS / SKIPPED with reason / SERVER_ERROR / FAIL). run-live.sh is the POSIX twin.
param(
    [ValidateSet("readonly", "dryrun", "dryrun-existing", "full", "scenario")]
    [string]$Tier = "readonly",
    [switch]$ConfirmDestructive,
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$MavenArgs = @()
)
Set-Location $PSScriptRoot
if (-not (Test-Path ".env.local")) { Write-Error "Missing .env.local - create it in the repo root with EQUINIX_ACCESS_KEY=... and EQUINIX_SECRET_KEY=... lines."; exit 1 }
# Export only the live-tier keys; values are taken verbatim after the first '='.
foreach ($line in Get-Content ".env.local") {
    if ($line -match '^(EQUINIX_ACCESS_KEY|EQUINIX_SECRET_KEY|EQUINIX_ACCESS_POLICY_TOKEN)=(.*)$') {
        Set-Item -Path "env:$($Matches[1])" -Value $Matches[2]
    }
}
if (-not $env:EQUINIX_ACCESS_KEY -or -not $env:EQUINIX_SECRET_KEY) {
    Write-Error "EQUINIX_ACCESS_KEY / EQUINIX_SECRET_KEY not set in .env.local"; exit 1
}
$profileArgs = @("-Pintegration-$Tier")
if ($Tier -in @("full", "scenario")) {
    if (-not $ConfirmDestructive) {
        Write-Error "Tier '$Tier' creates REAL resources; re-run with -ConfirmDestructive (sandbox accounts only)."; exit 1
    }
    $profileArgs += "-DconfirmDestructive=true"
}
$mvnw = if ($env:OS -eq "Windows_NT") { Join-Path $PSScriptRoot "mvnw.cmd" } else { Join-Path $PSScriptRoot "mvnw" }
# Credentials reach the test JVM through environment variables, never as -D system properties
# (which surefire can copy into its reports and which are visible in the process list).
& $mvnw -B test @profileArgs @MavenArgs
exit $LASTEXITCODE

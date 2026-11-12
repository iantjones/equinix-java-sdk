<#
.SYNOPSIS
    Builds, verifies, tags and publishes an Equinix Java SDK release.

.DESCRIPTION
    Runs the SAME gates as CI before anything is tagged or deployed:
      1. Preconditions: clean git tree, tag not taken, pom version == release version,
         and a "## [<version>]" section in CHANGELOG.md.
      2. ./mvnw clean verify -Pcoverage   default suite + WireMock suite + JaCoCo gate
                                          + javadoc jar (doclint) + sources jar + shaded MCP jar
      3. ./mvnw javadoc:javadoc           javadoc reference gate
      4. Shaded MCP server jar smoke test (-Dmcp.server.jar.required=true)
      5. git tag + push, GitHub release with the jars (optional)
      6. ./mvnw -Prelease deploy -DskipTests   GPG signing + Central Portal (optional)
      7. Next development version (X.Y.Z+1-SNAPSHOT), committed and pushed (optional)

    POSIX equivalent: scripts/release.sh. Commit and push everything BEFORE running.

.PARAMETER Version
    Release version (e.g. "4.0.0"). Defaults to the pom version without -SNAPSHOT.
.PARAMETER GpgKeyId
    GPG key id; required with -DeployMavenCentral.
.PARAMETER SkipGitHub
    Do not create the GitHub release.
.PARAMETER DeployMavenCentral
    Deploy to Maven Central (Sonatype Central Portal, autoPublish) with the release profile.
.PARAMETER SkipNextSnapshot
    Do not bump the pom to the next -SNAPSHOT version afterwards.
.PARAMETER DryRun
    Run the checks and the gates, but do not tag, release, deploy or bump.

.EXAMPLE
    .\scripts\release.ps1 -Version 4.0.0 -DryRun
.EXAMPLE
    .\scripts\release.ps1 -Version 4.0.0 -DeployMavenCentral -GpgKeyId BCD92FFA
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)][string]$Version,
    [string]$GpgKeyId,
    [switch]$SkipGitHub,
    [switch]$DeployMavenCentral,
    [switch]$SkipNextSnapshot,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$OnWindows = ($env:OS -eq "Windows_NT")
$Mvn = if ($OnWindows) { Join-Path $ProjectRoot "mvnw.cmd" } else { Join-Path $ProjectRoot "mvnw" }

function Step([string]$m) { Write-Host "`n=== $m ===" -ForegroundColor Cyan }
function Ok([string]$m) { Write-Host "  [OK] $m" -ForegroundColor Green }
function Die([string]$m) { Write-Host "  [FAIL] $m" -ForegroundColor Red; exit 1 }
function Mvn([string[]]$a) {
    Write-Host "  > mvnw $($a -join ' ')" -ForegroundColor DarkGray
    & $Mvn -B -ntp @a
    if ($LASTEXITCODE -ne 0) { Die "mvnw $($a -join ' ') failed" }
}

Push-Location $ProjectRoot
try {
    Step "Preconditions"
    foreach ($c in @("java", "git")) { if (-not (Get-Command $c -ErrorAction SilentlyContinue)) { Die "$c not found on PATH" } }
    if (-not (Test-Path $Mvn)) { Die "Maven wrapper $Mvn not found" }

    [xml]$pom = Get-Content (Join-Path $ProjectRoot "pom.xml")
    $pomVersion = $pom.project.version
    if (-not $Version) { $Version = $pomVersion -replace "-SNAPSHOT$", "" }
    if ($pomVersion -ne $Version) { Die "pom.xml version is $pomVersion; set it to $Version and commit first" }
    $TagName = "v$Version"
    Ok "Release version $Version (tag $TagName)"

    $status = & git status --porcelain
    if ($status) { Write-Host $status -ForegroundColor Yellow; Die "Working tree is not clean; commit or stash first" }
    Ok "Working tree clean"

    if (& git tag -l $TagName) { Die "Tag $TagName already exists" }
    Ok "Tag $TagName is free"

    $changelog = Get-Content (Join-Path $ProjectRoot "CHANGELOG.md") -Raw
    if ($changelog -notmatch "(?m)^## \[$([regex]::Escape($Version))\]") { Die "CHANGELOG.md has no '## [$Version]' section" }
    Ok "CHANGELOG.md has a [$Version] section"

    if ($DeployMavenCentral) {
        if (-not (Get-Command gpg -ErrorAction SilentlyContinue)) { Die "gpg not found (needed for -DeployMavenCentral)" }
        if (-not $GpgKeyId) { Die "-GpgKeyId is required with -DeployMavenCentral" }
    }
    $hasGh = [bool](Get-Command gh -ErrorAction SilentlyContinue)
    if (-not $hasGh -and -not $SkipGitHub) { Write-Host "  [WARN] gh not found; GitHub release skipped" -ForegroundColor Yellow; $SkipGitHub = $true }

    Step "CI gates"
    Mvn @("clean", "verify", "-Pcoverage")
    Ok "Default + WireMock suites, coverage gate, javadoc/sources/server jars"
    Mvn @("javadoc:javadoc")
    Ok "Javadoc reference gate"
    Mvn @("test", "-Dtest=ShadedJarSmokeTest", "-Dsurefire.failIfNoSpecifiedTests=false", "-Dmcp.server.jar.required=true")
    Ok "Shaded MCP server jar smoke test"

    $jars = @("equinix-sdk-java-$Version.jar", "equinix-sdk-java-$Version-sources.jar",
              "equinix-sdk-java-$Version-javadoc.jar", "equinix-sdk-java-$Version-mcp-server.jar")
    foreach ($j in $jars) { if (-not (Test-Path (Join-Path "target" $j))) { Die "Missing target/$j" } }
    Ok "Artifacts: $($jars -join ', ')"

    if ($DryRun) { Step "Dry run: gates passed; nothing tagged, released or deployed"; exit 0 }

    Step "Tag"
    & git tag -a $TagName -m "Release $Version"; if ($LASTEXITCODE -ne 0) { Die "git tag failed" }
    & git push origin $TagName; if ($LASTEXITCODE -ne 0) { Die "git push of $TagName failed" }
    Ok "Pushed $TagName"

    if (-not $SkipGitHub) {
        Step "GitHub release"
        $notes = "Equinix Java SDK $Version. See CHANGELOG.md for the full list of changes."
        & gh release create $TagName @($jars | ForEach-Object { Join-Path "target" $_ }) --title "Equinix Java SDK $Version" --notes $notes
        if ($LASTEXITCODE -ne 0) { Die "gh release create failed" }
        Ok "GitHub release $TagName"
    }

    if ($DeployMavenCentral) {
        Step "Maven Central"
        Write-Host "  Uses the 'central' server token from ~/.m2/settings.xml." -ForegroundColor White
        Mvn @("-Prelease", "deploy", "-DskipTests", "-Dgpg.keyname=$GpgKeyId")
        Ok "Published com.eqixiac.equinix:equinix-sdk-java:$Version"
    }

    if (-not $SkipNextSnapshot) {
        Step "Next development version"
        $p = $Version.Split(".")
        $next = "$($p[0]).$($p[1]).$([int]$p[2] + 1)-SNAPSHOT"
        Mvn @("versions:set", "-DnewVersion=$next", "-DgenerateBackupPoms=false")
        & git add pom.xml
        & git commit -m "Prepare next development version $next"
        & git push
        Ok "pom.xml now $next"
    }
    Step "Release $Version complete"
} finally {
    Pop-Location
}

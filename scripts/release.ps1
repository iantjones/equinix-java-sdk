<#
.SYNOPSIS
    Builds, tags, and publishes an Equinix Java SDK release.

.DESCRIPTION
    This script automates the full release workflow:
    1. Validates prerequisites (Java, Maven, Git, GitHub CLI)
    2. Runs unit tests
    3. Builds the SDK JARs (compiled, sources, javadoc)
    4. Creates a Git tag
    5. Creates a GitHub Release with attached JARs
    6. Optionally deploys to Maven Central via Sonatype OSSRH

.PARAMETER Version
    The release version (e.g., "1.1.0"). If not provided, reads from pom.xml.

.PARAMETER GpgKeyId
    The GPG key ID for Maven Central signing. Required only for Maven Central deploy.

.PARAMETER SkipTests
    Skip running tests before building. Default: false.

.PARAMETER SkipGitHub
    Skip creating the GitHub release. Default: false.

.PARAMETER DeployMavenCentral
    Deploy to Maven Central via Sonatype OSSRH. Default: false.

.PARAMETER DryRun
    Show what would be done without executing. Default: false.

.EXAMPLE
    # Build and create GitHub release (most common usage)
    .\scripts\release.ps1 -Version "1.1.0"

.EXAMPLE
    # Build, create GitHub release, AND deploy to Maven Central
    .\scripts\release.ps1 -Version "1.1.0" -DeployMavenCentral -GpgKeyId "ABCD1234"

.EXAMPLE
    # Preview what would happen without doing anything
    .\scripts\release.ps1 -Version "1.1.0" -DryRun
#>

[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$Version,

    [string]$GpgKeyId,

    [switch]$SkipTests,

    [switch]$SkipGitHub,

    [switch]$DeployMavenCentral,

    [switch]$DryRun
)

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent $PSScriptRoot

# ============================================================================
# Helper Functions
# ============================================================================

function Write-Step {
    param([string]$Message)
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "  [OK] $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "  [WARN] $Message" -ForegroundColor Yellow
}

function Write-Fail {
    param([string]$Message)
    Write-Host "  [FAIL] $Message" -ForegroundColor Red
}

function Test-Command {
    param([string]$Command)
    $null = Get-Command $Command -ErrorAction SilentlyContinue
    return $?
}

# ============================================================================
# Step 1: Validate Prerequisites
# ============================================================================

Write-Step "Validating Prerequisites"

# Java
if (Test-Command "java") {
    $javaVersion = & java -version 2>&1 | Out-String | ForEach-Object { $_.Trim().Split("`n")[0] }
    Write-Success "Java: $javaVersion"
} else {
    Write-Fail "Java not found. Install JDK 14+ and add to PATH."
    exit 1
}

# Maven
if (Test-Command "mvn") {
    $mvnVersion = & mvn --version 2>&1 | Out-String | ForEach-Object { $_.Trim().Split("`n")[0] }
    Write-Success "Maven: $mvnVersion"
} else {
    Write-Fail "Maven not found. Install Maven 3.6+ and add to PATH."
    exit 1
}

# Git
if (Test-Command "git") {
    $gitVersion = & git --version 2>&1 | Out-String | ForEach-Object { $_.Trim() }
    Write-Success "Git: $gitVersion"
} else {
    Write-Fail "Git not found. Install Git and add to PATH."
    exit 1
}

# GitHub CLI (optional but needed for GitHub releases)
$hasGhCli = Test-Command "gh"
if ($hasGhCli) {
    $ghVersion = & gh --version 2>&1 | Out-String | ForEach-Object { $_.Trim().Split("`n")[0] }
    Write-Success "GitHub CLI: $ghVersion"
} else {
    Write-Warning "GitHub CLI (gh) not found."
    Write-Host "         Install from: https://cli.github.com/" -ForegroundColor Yellow
    Write-Host "         Or: winget install --id GitHub.cli" -ForegroundColor Yellow
    Write-Host "         GitHub release step will be skipped." -ForegroundColor Yellow
    if (-not $SkipGitHub) {
        $SkipGitHub = $true
    }
}

# GPG (optional, needed for Maven Central)
if ($DeployMavenCentral) {
    if (Test-Command "gpg") {
        Write-Success "GPG: Available"
    } else {
        Write-Fail "GPG not found. Required for Maven Central signing."
        Write-Host "         Install from: https://gnupg.org/download/" -ForegroundColor Red
        exit 1
    }
    if (-not $GpgKeyId) {
        Write-Fail "-GpgKeyId is required for Maven Central deployment."
        Write-Host "         List keys with: gpg --list-keys" -ForegroundColor Red
        exit 1
    }
}

# ============================================================================
# Step 2: Determine Version
# ============================================================================

Write-Step "Determining Version"

Push-Location $ProjectRoot
try {
    if (-not $Version) {
        # Read version from pom.xml
        [xml]$pom = Get-Content "$ProjectRoot\pom.xml"
        $pomVersion = $pom.project.version
        if ($pomVersion -match "-SNAPSHOT$") {
            $Version = $pomVersion -replace "-SNAPSHOT$", ""
            Write-Host "  POM version: $pomVersion" -ForegroundColor White
            Write-Host "  Release version will be: $Version" -ForegroundColor White
        } else {
            $Version = $pomVersion
        }
    }

    $TagName = "v$Version"
    $ArtifactId = "equinix-sdk-java"
    $JarName = "$ArtifactId-$Version.jar"
    $SourcesJar = "$ArtifactId-$Version-sources.jar"
    $JavadocJar = "$ArtifactId-$Version-javadoc.jar"

    Write-Success "Release version: $Version"
    Write-Success "Git tag: $TagName"
    Write-Success "Artifacts: $JarName, $SourcesJar, $JavadocJar"

    # Check if tag already exists
    $existingTag = & git tag -l $TagName 2>&1
    if ($existingTag -eq $TagName) {
        Write-Fail "Tag $TagName already exists. Choose a different version or delete the existing tag."
        exit 1
    }

    # Check for uncommitted changes
    $gitStatus = & git status --porcelain 2>&1
    if ($gitStatus) {
        Write-Warning "Working directory has uncommitted changes:"
        Write-Host $gitStatus -ForegroundColor Yellow
        $proceed = Read-Host "  Continue anyway? (y/N)"
        if ($proceed -ne "y" -and $proceed -ne "Y") {
            Write-Host "  Aborted." -ForegroundColor Red
            exit 0
        }
    }

    if ($DryRun) {
        Write-Host "`n  === DRY RUN MODE === (no changes will be made)" -ForegroundColor Magenta
    }

    # ============================================================================
    # Step 3: Update POM Version (remove -SNAPSHOT)
    # ============================================================================

    Write-Step "Updating POM Version"

    [xml]$pom = Get-Content "$ProjectRoot\pom.xml"
    $currentVersion = $pom.project.version

    if ($currentVersion -ne $Version) {
        Write-Host "  Updating version: $currentVersion -> $Version" -ForegroundColor White
        if (-not $DryRun) {
            & mvn versions:set "-DnewVersion=$Version" -q 2>&1 | Out-Null
            # Clean up backup files created by versions plugin
            & mvn versions:commit -q 2>&1 | Out-Null
            Write-Success "POM version updated to $Version"
        } else {
            Write-Host "  [DRY RUN] Would update POM version to $Version" -ForegroundColor Magenta
        }
    } else {
        Write-Success "POM version already at $Version"
    }

    # ============================================================================
    # Step 4: Run Tests
    # ============================================================================

    if (-not $SkipTests) {
        Write-Step "Running Unit Tests"
        if (-not $DryRun) {
            & mvn test -q
            if ($LASTEXITCODE -ne 0) {
                Write-Fail "Tests failed. Fix test failures before releasing."
                exit 1
            }
            Write-Success "All tests passed"
        } else {
            Write-Host "  [DRY RUN] Would run: mvn test" -ForegroundColor Magenta
        }
    } else {
        Write-Host "`n  Skipping tests (-SkipTests)" -ForegroundColor Yellow
    }

    # ============================================================================
    # Step 5: Build JARs
    # ============================================================================

    Write-Step "Building Release JARs"

    if (-not $DryRun) {
        & mvn clean package -DskipTests -q
        if ($LASTEXITCODE -ne 0) {
            Write-Fail "Build failed."
            exit 1
        }

        # Verify all 3 JARs exist
        $jarPath = "$ProjectRoot\target\$JarName"
        $sourcesPath = "$ProjectRoot\target\$SourcesJar"
        $javadocPath = "$ProjectRoot\target\$JavadocJar"

        if (Test-Path $jarPath) { Write-Success "Built: $JarName ($('{0:N1} MB' -f ((Get-Item $jarPath).Length / 1MB)))" }
        else { Write-Fail "Missing: $jarPath"; exit 1 }

        if (Test-Path $sourcesPath) { Write-Success "Built: $SourcesJar ($('{0:N1} MB' -f ((Get-Item $sourcesPath).Length / 1MB)))" }
        else { Write-Fail "Missing: $sourcesPath"; exit 1 }

        if (Test-Path $javadocPath) { Write-Success "Built: $JavadocJar ($('{0:N1} MB' -f ((Get-Item $javadocPath).Length / 1MB)))" }
        else { Write-Fail "Missing: $javadocPath"; exit 1 }
    } else {
        Write-Host "  [DRY RUN] Would run: mvn clean package -DskipTests" -ForegroundColor Magenta
    }

    # ============================================================================
    # Step 6: Git Tag
    # ============================================================================

    Write-Step "Creating Git Tag"

    if (-not $DryRun) {
        & git add pom.xml
        & git commit -m "Release $Version" --allow-empty
        & git tag -a $TagName -m "Release $Version"
        Write-Success "Created tag: $TagName"

        Write-Host "  Pushing tag to remote..." -ForegroundColor White
        & git push origin $TagName
        & git push
        Write-Success "Pushed tag and commits to remote"
    } else {
        Write-Host "  [DRY RUN] Would create and push tag: $TagName" -ForegroundColor Magenta
    }

    # ============================================================================
    # Step 7: GitHub Release
    # ============================================================================

    if (-not $SkipGitHub) {
        Write-Step "Creating GitHub Release"

        $releaseNotes = @"
## Equinix Java SDK $Version

### Highlights
- 7 API domains: Fabric, Network Edge, Customer Portal, IBX SmartView, Internet Access, Projects, Messaging
- 58 resource types with 310+ API methods
- Cloud provider SDK interoperability (AWS Direct Connect, Azure ExpressRoute, Google Cloud Interconnect, Oracle FastConnect)
- Fluent builder pattern with Dry Run validation
- Automatic pagination, OAuth2 authentication, typed exception hierarchy

### Installation

**Maven:**
``````xml
<dependency>
    <groupId>com.eqixiac.equinix</groupId>
    <artifactId>equinix-sdk-java</artifactId>
    <version>$Version</version>
</dependency>
``````

**Manual:** Download the JAR files attached to this release.

### Artifacts
- ``$JarName`` - Compiled SDK
- ``$SourcesJar`` - Source code
- ``$JavadocJar`` - Javadoc documentation
"@

        if (-not $DryRun) {
            & gh release create $TagName `
                "$ProjectRoot\target\$JarName" `
                "$ProjectRoot\target\$SourcesJar" `
                "$ProjectRoot\target\$JavadocJar" `
                --title "Equinix Java SDK $Version" `
                --notes $releaseNotes

            if ($LASTEXITCODE -eq 0) {
                Write-Success "GitHub Release created: $TagName"
                $releaseUrl = & gh release view $TagName --json url -q .url 2>&1
                Write-Host "`n  Release URL: $releaseUrl" -ForegroundColor Green
            } else {
                Write-Fail "GitHub Release creation failed. You can create it manually at:"
                Write-Host "  https://github.com/iantjones/equinix-java-sdk/releases/new" -ForegroundColor Yellow
            }
        } else {
            Write-Host "  [DRY RUN] Would create GitHub release $TagName with 3 JAR attachments" -ForegroundColor Magenta
        }
    } else {
        Write-Host "`n  Skipping GitHub release" -ForegroundColor Yellow
    }

    # ============================================================================
    # Step 8: Maven Central Deploy (Optional)
    # ============================================================================

    if ($DeployMavenCentral) {
        Write-Step "Deploying to Maven Central"

        Write-Host "  This will deploy to Sonatype OSSRH staging repository." -ForegroundColor White
        Write-Host "  Ensure ~/.m2/settings.xml has your OSSRH credentials." -ForegroundColor White

        if (-not $DryRun) {
            & mvn clean deploy -DskipTests "-Dgpg.keyname=$GpgKeyId"
            if ($LASTEXITCODE -eq 0) {
                Write-Success "Deployed to Sonatype OSSRH staging"
                Write-Host ""
                Write-Host "  Next steps:" -ForegroundColor Yellow
                Write-Host "  1. Log in to https://s01.oss.sonatype.org/" -ForegroundColor Yellow
                Write-Host "  2. Find your staging repository under 'Staging Repositories'" -ForegroundColor Yellow
                Write-Host "  3. Select it -> Close -> Release" -ForegroundColor Yellow
                Write-Host "  4. Artifacts appear on Maven Central within ~30 minutes" -ForegroundColor Yellow
            } else {
                Write-Fail "Maven Central deployment failed. Check your OSSRH credentials and GPG key."
            }
        } else {
            Write-Host "  [DRY RUN] Would run: mvn clean deploy -DskipTests -Dgpg.keyname=$GpgKeyId" -ForegroundColor Magenta
        }
    }

    # ============================================================================
    # Step 9: Prepare Next Development Version
    # ============================================================================

    Write-Step "Preparing Next Development Version"

    # Increment patch version for next SNAPSHOT
    $versionParts = $Version.Split(".")
    if ($versionParts.Length -ge 3) {
        $versionParts[2] = [string]([int]$versionParts[2] + 1)
    } elseif ($versionParts.Length -eq 2) {
        $versionParts += "1"
    } else {
        $versionParts = @($Version, "0", "1")
    }
    $nextVersion = ($versionParts -join ".") + "-SNAPSHOT"

    Write-Host "  Next development version: $nextVersion" -ForegroundColor White

    if (-not $DryRun) {
        & mvn versions:set "-DnewVersion=$nextVersion" -q 2>&1 | Out-Null
        & mvn versions:commit -q 2>&1 | Out-Null
        & git add pom.xml
        & git commit -m "Prepare next development version $nextVersion"
        & git push
        Write-Success "POM updated to $nextVersion and pushed"
    } else {
        Write-Host "  [DRY RUN] Would update POM to $nextVersion" -ForegroundColor Magenta
    }

    # ============================================================================
    # Done
    # ============================================================================

    Write-Host "`n" -NoNewline
    Write-Step "Release Complete!"
    Write-Host ""
    Write-Host "  Version:    $Version" -ForegroundColor Green
    Write-Host "  Tag:        $TagName" -ForegroundColor Green
    if (-not $SkipGitHub -and $hasGhCli) {
        Write-Host "  GitHub:     https://github.com/iantjones/equinix-java-sdk/releases/tag/$TagName" -ForegroundColor Green
    }
    if ($DeployMavenCentral) {
        Write-Host "  Maven:      com.eqixiac.equinix:equinix-sdk-java:$Version" -ForegroundColor Green
    }
    Write-Host ""

} finally {
    Pop-Location
}

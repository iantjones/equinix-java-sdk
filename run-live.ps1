# Runs the live integration tiers using credentials from .env.local (gitignored).
# Usage: .\run-live.ps1 [readonly|dryrun]   (default: readonly)
param([string]$Tier = "readonly")
if (-not (Test-Path ".env.local")) { Write-Error "Missing .env.local - copy .env.local.example and fill it in."; exit 1 }
Get-Content ".env.local" | ForEach-Object {
    if ($_ -match '^\s*([A-Z_]+)\s*=\s*(.+)\s*$') { Set-Item -Path "env:$($Matches[1])" -Value $Matches[2] }
}
if (-not $env:EQUINIX_ACCESS_KEY) { Write-Error "EQUINIX_ACCESS_KEY not set in .env.local"; exit 1 }
$profileArgs = switch ($Tier) {
    "readonly" { @("-Pintegration-readonly") }
    "dryrun"   { @("-Pintegration-dryrun", "-DtestMode=dryrun") }
    default    { Write-Error "Unknown tier '$Tier' (use readonly|dryrun)"; exit 1 }
}
mvn test @profileArgs "-DaccessKey=$env:EQUINIX_ACCESS_KEY" "-DsecretKey=$env:EQUINIX_SECRET_KEY"

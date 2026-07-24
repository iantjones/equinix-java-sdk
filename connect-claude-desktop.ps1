# Registers the Equinix Intelligence MCP Server in Claude Desktop's config.
#
# Claude Desktop rewrites claude_desktop_config.json from memory when it exits,
# clobbering any edit made while it runs — so this script refuses to run until
# the app is fully quit (tray icon -> Quit).
#
# Usage:
#   1. Fill EQUINIX_ACCESS_KEY / EQUINIX_SECRET_KEY in .env.local (copy .env.local.example)
#   2. Quit Claude Desktop completely
#   3. .\connect-claude-desktop.ps1
#   4. Start Claude Desktop; the tools icon in the chat box should list "equinix" with 12 tools

$ErrorActionPreference = "Stop"

if (Get-Process -Name "Claude" -ErrorAction SilentlyContinue) {
    Write-Error "Claude Desktop is still running. Quit it completely (system tray icon -> Quit), then re-run this script. Edits made while it runs are wiped when it exits."
    exit 1
}

$envFile = Join-Path $PSScriptRoot ".env.local"
if (-not (Test-Path $envFile)) {
    Write-Error "Missing .env.local — copy .env.local.example to .env.local and fill in EQUINIX_ACCESS_KEY / EQUINIX_SECRET_KEY."
    exit 1
}
$vars = @{}
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([A-Z_]+)\s*=\s*(.+?)\s*$') { $vars[$Matches[1]] = $Matches[2] }
}
if (-not $vars["EQUINIX_ACCESS_KEY"] -or -not $vars["EQUINIX_SECRET_KEY"]) {
    Write-Error "EQUINIX_ACCESS_KEY / EQUINIX_SECRET_KEY are empty in .env.local — fill them in first."
    exit 1
}

$jar = Join-Path $PSScriptRoot "target\equinix-sdk-java-2.0.1-mcp-server.jar"
if (-not (Test-Path $jar)) {
    Write-Error "Server jar not found at $jar — build it first: mvn -q package -DskipTests"
    exit 1
}

$configPath = Join-Path $env:APPDATA "Claude\claude_desktop_config.json"
$config = if (Test-Path $configPath) { Get-Content $configPath -Raw | ConvertFrom-Json } else { [pscustomobject]@{} }
if (-not $config.PSObject.Properties["mcpServers"]) {
    $config | Add-Member -MemberType NoteProperty -Name "mcpServers" -Value ([pscustomobject]@{})
}
$entry = [pscustomobject]@{
    command = "java"
    args    = @("-jar", $jar)
    env     = [pscustomobject]@{
        EQUINIX_ACCESS_KEY = $vars["EQUINIX_ACCESS_KEY"]
        EQUINIX_SECRET_KEY = $vars["EQUINIX_SECRET_KEY"]
    }
}
if ($config.mcpServers.PSObject.Properties["equinix"]) {
    $config.mcpServers.equinix = $entry
} else {
    $config.mcpServers | Add-Member -MemberType NoteProperty -Name "equinix" -Value $entry
}
$config | ConvertTo-Json -Depth 10 | Set-Content $configPath -Encoding utf8

Write-Host "Registered 'equinix' MCP server in $configPath"
Write-Host "Jar: $jar"
Write-Host "Now start Claude Desktop and check the tools icon in the chat input for 'equinix' (12 tools)."

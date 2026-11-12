# Registers the Equinix Intelligence MCP Server in Claude Desktop's config (Windows).
# macOS / Linux: use connect-claude-desktop.sh, which behaves the same way.
#
# The merge itself is done by the server jar (ClaudeDesktopConfigMerger), so both scripts
# apply identical rules:
#   - an existing mcpServers.equinix entry is MERGED, not replaced: its env (EQUINIX_SANDBOX,
#     EQUINIX_MCP_TOOLSETS, ...), extra args (--toolsets, JVM options), a custom java command
#     and any other fields are kept; only the jar path is updated;
#   - the config is backed up first to claude_desktop_config.json.bak-<timestamp>;
#   - by DEFAULT NO SECRET IS WRITTEN: the entry gets cwd = this repository and
#     --env-file <repo>\.env.local, so the server reads the credentials from .env.local.
#     Credentials embedded by an older version of this script are removed from the config.
#     Pass -EmbedCredentials to copy them into the config's env block in plain text instead.
#
# Claude Desktop rewrites claude_desktop_config.json from its in-memory settings while
# running and on exit, clobbering any external edit - so this script refuses to run until
# the app is fully quit (tray icon -> Quit).
#
# Usage:
#   1. Fill EQUINIX_ACCESS_KEY / EQUINIX_SECRET_KEY in .env.local (a KEY=value file in the repo root; it is gitignored)
#   2. Build the server jar: mvn -q package -DskipTests
#   3. Quit Claude Desktop completely
#   4. .\connect-claude-desktop.ps1 [-EmbedCredentials]
#   5. Start Claude Desktop; the tools icon in the chat box should list 'equinix'
#
# NOTE: keep this file pure ASCII - Windows PowerShell 5.1 parses BOM-less
# scripts as ANSI, and UTF-8 punctuation decodes into string-breaking smart quotes.

param(
    [switch]$EmbedCredentials
)

$ErrorActionPreference = "Stop"

if (Get-Process -Name "Claude" -ErrorAction SilentlyContinue) {
    Write-Error "Claude Desktop is still running. Quit it completely (system tray icon -> Quit), then re-run this script. Edits made while it runs are wiped when it exits."
    exit 1
}

$envFile = Join-Path $PSScriptRoot ".env.local"
if (-not (Test-Path $envFile)) {
    Write-Error "Missing .env.local - create it in the repo root with EQUINIX_ACCESS_KEY=... and EQUINIX_SECRET_KEY=... lines."
    exit 1
}

# Resolve the current build's server jar regardless of version (newest wins).
$jarItem = Get-ChildItem (Join-Path $PSScriptRoot "target") -Filter "equinix-sdk-java-*-mcp-server.jar" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jarItem) {
    Write-Error "No mcp-server jar found in target\ - build one first: mvn -q package -DskipTests"
    exit 1
}
$jar = $jarItem.FullName

$java = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "java" }
$configPath = Join-Path $env:APPDATA "Claude\claude_desktop_config.json"

# The merger validates .env.local (export prefixes, quotes and inline comments are accepted;
# a malformed line fails with a message naming the line and key, never the value), backs up
# the config, merges and writes it as UTF-8 without a byte-order mark.
$mergeArgs = @("-cp", $jar, "com.eqixiac.equinix.mcp.server.ClaudeDesktopConfigMerger",
               "--config", $configPath, "--jar", $jar, "--repo", $PSScriptRoot)
if ($EmbedCredentials) { $mergeArgs += "--embed-credentials" }
& $java @mergeArgs
if ($LASTEXITCODE -ne 0) {
    Write-Error "The config was not changed (see the message above)."
    exit $LASTEXITCODE
}

Write-Host "Now start Claude Desktop and check the tools icon in the chat input for 'equinix'."

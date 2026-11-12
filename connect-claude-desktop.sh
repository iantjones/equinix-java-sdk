#!/bin/sh
# Registers the Equinix Intelligence MCP Server in Claude Desktop's config (macOS / Linux).
# Windows: use connect-claude-desktop.ps1, which behaves the same way.
#
# The merge itself is done by the server jar (ClaudeDesktopConfigMerger), so both scripts
# apply identical rules:
#   - an existing mcpServers.equinix entry is MERGED, not replaced: its env (EQUINIX_SANDBOX,
#     EQUINIX_MCP_TOOLSETS, ...), extra args (--toolsets, JVM options), a custom java command
#     and any other fields are kept; only the jar path is updated;
#   - the config is backed up first to claude_desktop_config.json.bak-<timestamp>;
#   - by DEFAULT NO SECRET IS WRITTEN: the entry gets cwd = this repository and
#     --env-file <repo>/.env.local, so the server reads the credentials from .env.local.
#     Credentials embedded by an older version of the script are removed from the config.
#     Pass --embed-credentials to copy them into the config's env block in plain text instead.
#
# Config location: macOS ~/Library/Application Support/Claude/claude_desktop_config.json;
# Linux ${XDG_CONFIG_HOME:-~/.config}/Claude/claude_desktop_config.json (Claude Desktop has no
# official Linux build; this is the path community builds use). Override with
# CLAUDE_DESKTOP_CONFIG=/path/to/claude_desktop_config.json.
#
# Usage:
#   1. Fill EQUINIX_ACCESS_KEY / EQUINIX_SECRET_KEY in .env.local (a KEY=value file in the repo root; it is gitignored)
#   2. Build the server jar: mvn -q package -DskipTests
#   3. Quit Claude Desktop completely (it rewrites its config on exit)
#   4. ./connect-claude-desktop.sh [--embed-credentials]
#   5. Start Claude Desktop; the tools icon in the chat box should list 'equinix'
set -eu

repo=$(cd "$(dirname "$0")" && pwd)
embed=""
for arg in "$@"; do
  case "$arg" in
    --embed-credentials) embed="--embed-credentials" ;;
    -h|--help) sed -n '2,26p' "$0"; exit 0 ;;
    *) echo "Unknown argument: $arg" >&2; exit 64 ;;
  esac
done

if pgrep -x Claude >/dev/null 2>&1 || pgrep -x claude-desktop >/dev/null 2>&1; then
  echo "Claude Desktop is still running. Quit it completely, then re-run this script." >&2
  echo "Edits made while it runs are wiped when it exits." >&2
  exit 1
fi

if [ ! -f "$repo/.env.local" ]; then
  echo "Missing .env.local - create it in the repo root with EQUINIX_ACCESS_KEY=... and EQUINIX_SECRET_KEY=... lines." >&2
  exit 1
fi

# Newest server jar, regardless of version.
jar=$(ls -t "$repo"/target/equinix-sdk-java-*-mcp-server.jar 2>/dev/null | head -n 1 || true)
if [ -z "$jar" ]; then
  echo "No mcp-server jar found in target/ - build one first: mvn -q package -DskipTests" >&2
  exit 1
fi

if [ -n "${CLAUDE_DESKTOP_CONFIG:-}" ]; then
  config="$CLAUDE_DESKTOP_CONFIG"
elif [ "$(uname -s)" = "Darwin" ]; then
  config="$HOME/Library/Application Support/Claude/claude_desktop_config.json"
else
  config="${XDG_CONFIG_HOME:-$HOME/.config}/Claude/claude_desktop_config.json"
fi

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  java="$JAVA_HOME/bin/java"
else
  java=java
fi

# The merger validates .env.local (export prefixes, quotes and inline comments are accepted; a
# malformed line fails with a message naming the line and key, never the value), backs up the
# config, merges and writes it.
if ! "$java" -cp "$jar" com.eqixiac.equinix.mcp.server.ClaudeDesktopConfigMerger \
    --config "$config" --jar "$jar" --repo "$repo" $embed; then
  echo "The config was not changed (see the message above)." >&2
  exit 1
fi

echo "Now start Claude Desktop and check the tools icon in the chat input for 'equinix'."

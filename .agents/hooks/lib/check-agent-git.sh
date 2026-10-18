#!/usr/bin/env sh
# Usage: check-agent-git.sh <action>
# Last line of defense for Rule 2. Blocks git actions started from a known AI agent shell.
# Best effort: it relies on env vars the agents set, which can change between versions.
. "$(dirname "$0")/common.sh"

for v in CLAUDECODE CLAUDE_CODE_ENTRYPOINT CODEX_SANDBOX CODEX_SANDBOX_NETWORK_DISABLED GEMINI_CLI CURSOR_AGENT AI_AGENT; do
  eval "val=\${$v:-}"
  [ -n "$val" ] && block "git $1 from an AI agent ($v is set). Only the user runs git."
done
exit 0

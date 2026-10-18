#!/usr/bin/env sh
# Usage: check-path.sh <path> <read|write>
# Exit 0 = allowed, exit 2 = blocked (reason on stderr).
. "$(dirname "$0")/common.sh"
REPO_ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"

path="$1"
mode="${2:-write}"
[ -z "$path" ] && exit 0

# Rule 5: stay inside the repo.
case "$path" in
  *..*) printf '%s' "$path" | grep -Eq '(^|/)\.\.(/|$)' && block "paths with .. are not allowed: $path" ;;
esac
case "$path" in
  "~"*) block "the home folder is outside the repo: $path" ;;
  /*) case "$path" in
        "$REPO_ROOT"|"$REPO_ROOT"/*) ;;
        *) block "path outside the repo is not allowed: $path" ;;
      esac ;;
esac

rel="${path#"$REPO_ROOT"/}"

# Secrets are never read.
case "$rel" in
  .env.example|*/.env.example) ;;
  .env|.env.*|*/.env|*/.env.*) block "env files hold secrets and are off limits: $rel" ;;
esac

# Rule 7: guardrail files are read-only for agents.
if [ "$mode" = "write" ]; then
  case "$rel" in
    .agents/*|.githooks/*|.claude/settings.json|.cursor/hooks.json|.gemini/settings.json|.codex/*)
      block "guardrail files can only be changed by the user: $rel" ;;
  esac
fi

exit 0

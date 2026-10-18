#!/usr/bin/env sh
# Shared pre-commit checks. Runs for every agent and human via git.
set -e
LIB="$(dirname "$0")/lib"

# Rule 2: agents never commit.
"$LIB/check-agent-git.sh" commit

# Block committing env files (secrets).
staged_env=$(git diff --cached --name-only | grep -E '(^|/)\.env($|\.)' | grep -v '\.env\.example$' || true)
if [ -n "$staged_env" ]; then
  echo "pre-commit: env files must not be committed:"
  echo "$staged_env"
  exit 1
fi

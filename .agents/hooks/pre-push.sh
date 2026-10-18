#!/usr/bin/env sh
# Shared pre-push check. Same agent check as pre-commit.
exec "$(dirname "$0")/lib/check-agent-git.sh" push

#!/usr/bin/env sh
# One-time setup after cloning. Points git at the shared hooks.
set -e
cd "$(git rev-parse --show-toplevel)"
git config core.hooksPath .githooks
chmod +x .githooks/* .agents/hooks/*.sh
echo "Shared hooks enabled."

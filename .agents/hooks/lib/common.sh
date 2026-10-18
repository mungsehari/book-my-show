#!/usr/bin/env sh
# Shared helpers for all guard scripts.

# Print a reason and exit with code 2 (the "block" code most agents understand).
block() {
  echo "BLOCKED by repo guardrails: $1" >&2
  echo "See AGENTS.md > Guardrails. Do not work around this. Stop and ask the user." >&2
  exit 2
}

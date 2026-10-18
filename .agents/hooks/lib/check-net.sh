#!/usr/bin/env sh
# Exit 0 if the user unlocked the network and the time limit has not passed. Otherwise block.
. "$(dirname "$0")/common.sh"

f="${GUARD_NET_FILE:-$(cd "$(dirname "$0")/../.." && pwd)/state/net-unlock-until}"
expiry="$(cat "$f" 2>/dev/null || true)"
case "$expiry" in ''|*[!0-9]*) expiry=0 ;; esac
[ "$(date +%s)" -lt "$expiry" ] && exit 0
block "network is locked. Ask the user to run: ./.agents/net-unlock.sh <minutes>"

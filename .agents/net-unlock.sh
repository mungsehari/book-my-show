#!/usr/bin/env sh
# User only. Unlocks network commands for agents for N minutes (default 15, max 120).
# Lock again early with: ./.agents/net-unlock.sh 0
set -e
mins="${1:-15}"
case "$mins" in ''|*[!0-9]*) echo "Usage: $0 <minutes>"; exit 1 ;; esac
[ "$mins" -gt 120 ] && mins=120
dir="$(cd "$(dirname "$0")" && pwd)/state"
mkdir -p "$dir"
expiry=$(( $(date +%s) + mins * 60 ))
echo "$expiry" > "$dir/net-unlock-until"
if [ "$mins" -eq 0 ]; then
  echo "Network locked."
else
  echo "Network unlocked for $mins minutes (until $(date -r "$expiry" '+%H:%M'))."
fi

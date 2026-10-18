#!/usr/bin/env sh
# Cursor hooks adapter. Reads hook JSON on stdin, replies with JSON.
# Blocked = "deny". Allowed shell and MCP calls = "ask", so the user approves every one (Rule 1).
LIB="$(cd "$(dirname "$0")" && pwd)/lib"
input="$(cat)"
event="$(printf '%s' "$input" | jq -r '.hook_event_name // empty')"
get() { printf '%s' "$input" | jq -r "$1 // empty"; }

reply() {
  jq -cn --arg p "$1" --arg m "$2" '{permission:$p, continue:($p != "deny"), userMessage:$m, agentMessage:$m}'
  exit 0
}
run() {
  reason="$("$@" 2>&1 >/dev/null)" || reply deny "$reason"
}

case "$event" in
  beforeShellExecution)
    run "$LIB/check-command.sh" "$(get '.command')"
    reply ask "Approve this command?" ;;
  beforeMCPExecution)
    run "$LIB/check-mcp.sh" "$(get '.tool_name')" "$(get '.tool_input | tostring')"
    reply ask "Approve this MCP call?" ;;
  beforeReadFile)
    run "$LIB/check-path.sh" "$(get '.file_path')" read
    reply allow "" ;;
esac
reply allow ""

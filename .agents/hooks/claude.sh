#!/usr/bin/env sh
# Claude Code PreToolUse adapter. Reads hook JSON on stdin.
# Exit 2 blocks the tool call and shows the reason to Claude.
LIB="$(cd "$(dirname "$0")" && pwd)/lib"
input="$(cat)"
tool="$(printf '%s' "$input" | jq -r '.tool_name // empty')"
get() { printf '%s' "$input" | jq -r "$1 // empty"; }

case "$tool" in
  Bash)
    exec "$LIB/check-command.sh" "$(get '.tool_input.command')" ;;
  Read|Glob|Grep|LS)
    exec "$LIB/check-path.sh" "$(get '.tool_input.file_path // .tool_input.path')" read ;;
  Write|Edit|MultiEdit|NotebookEdit)
    exec "$LIB/check-path.sh" "$(get '.tool_input.file_path // .tool_input.notebook_path')" write ;;
  WebFetch|WebSearch)
    exec "$LIB/check-net.sh" ;;
  mcp__*)
    exec "$LIB/check-mcp.sh" "$tool" "$(get '.tool_input | tostring')" ;;
esac
exit 0

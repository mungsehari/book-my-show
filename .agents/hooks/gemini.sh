#!/usr/bin/env sh
# Gemini CLI BeforeTool adapter. Reads hook JSON on stdin.
# Exit 2 blocks the tool call.
LIB="$(cd "$(dirname "$0")" && pwd)/lib"
input="$(cat)"
tool="$(printf '%s' "$input" | jq -r '.tool_name // empty')"
get() { printf '%s' "$input" | jq -r "$1 // empty"; }
path="$(get '.tool_input.file_path // .tool_input.absolute_path // .tool_input.path // .tool_input.dir_path')"

case "$tool" in
  run_shell_command)
    exec "$LIB/check-command.sh" "$(get '.tool_input.command')" ;;
  read_file|read_many_files|list_directory|glob|search_file_content)
    exec "$LIB/check-path.sh" "$path" read ;;
  write_file|replace|edit)
    exec "$LIB/check-path.sh" "$path" write ;;
  web_fetch|google_web_search)
    exec "$LIB/check-net.sh" ;;
  mcp_*|*__*)
    exec "$LIB/check-mcp.sh" "$tool" "$(get '.tool_input | tostring')" ;;
esac
exit 0

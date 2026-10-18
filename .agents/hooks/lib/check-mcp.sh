#!/usr/bin/env sh
# Usage: check-mcp.sh <tool name> [tool input as text]
# Blocks MCP tools that touch the database or delete data (Rules 3 and 4).
. "$(dirname "$0")/common.sh"

name="$1"
input="$2"

printf '%s' "$name" | grep -Eiq '(sql|database|_db|db_|table|record|row|delete|drop|truncate|remove|destroy|purge|migrat)' \
  && block "MCP tool '$name' can touch the database or delete data. Only the user runs it."
printf '%s' "$input" | grep -Eiq '(drop[[:space:]]+(table|database|schema|collection)|delete[[:space:]]+from|truncate[[:space:]]|deleteMany|deleteOne|findOneAndDelete|findByIdAndDelete|dropDatabase|dropCollection|flushall|flushdb|"method"[[:space:]]*:[[:space:]]*"delete")' \
  && block "delete operations in MCP tool input are not allowed."

exit 0

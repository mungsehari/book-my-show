"""Phase 0 guardrail patch. Run once from the repo root: python3 phase0-patch.py

Agents cannot edit guardrail files, so the user runs this.
Changes:
- Git: read-only commands allowed (status, diff, log, show). Everything else blocked.
- Database: more delete patterns blocked (ORM deletes, Redis flush, HTTP DELETE).
- Network: locked by default. User unlocks for a time limit with ./.agents/net-unlock.sh <minutes>.
- Fix: Rule 7 false positive (>/dev/null and words like "scp").
After running, delete this file yourself.
"""
from pathlib import Path
import os

FILES = {}

FILES[".agents/hooks/lib/check-command.sh"] = r'''#!/usr/bin/env sh
# Usage: check-command.sh "<shell command>"
# Exit 0 = allowed, exit 2 = blocked (reason on stderr).
LIB="$(cd "$(dirname "$0")" && pwd)"
. "$LIB/common.sh"
REPO_ROOT="$(cd "$LIB/../../.." && pwd)"

cmd="$1"
[ -z "$cmd" ] && exit 0

# Command start = beginning of string or after ; & | ( ` $( or whitespace.
S='(^|[;&|(`]|\$\(|[[:space:]])'
E='([[:space:]]|$|[;&|)])'

has() { printf '%s' "$cmd" | grep -Eiq -- "$1"; }

# Rule 2: git is read-only. GitHub CLI is blocked.
has "${S}(gh|hub)${E}" && block "GitHub CLI is not allowed. The user runs it."
has "core\.hooksPath|--no-verify" && block "changing or skipping git hooks is not allowed."
if has "${S}git${E}"; then
  subs="$(printf '%s' "$cmd" | grep -Eio -- "${S}git([[:space:]]+[^[:space:];&|)]*)?" | sed -E 's/.*[gG][iI][tT][[:space:]]*//')"
  for sub in $subs; do
    case "$sub" in
      status|diff|log|show|blame|ls-files|rev-parse|shortlog|describe) ;;
      *) block "git $sub is not allowed. Agents may only run read-only git (status, diff, log, show). The user runs the rest." ;;
    esac
  done
fi

# Rule 4: no deleting files.
has "${S}(rm|rmdir|unlink|shred|srm|trash|truncate)${E}" && block "deleting files is not allowed."
has "${S}find${E}.*(-delete|-exec[[:space:]]+(rm|unlink))" && block "deleting files with find is not allowed."
has "rmSync|unlinkSync|rmdirSync|fs\.(promises\.)?(rm|unlink|rmdir)|os\.(remove|unlink|rmdir)|shutil\.rmtree|\.unlink\(" \
  && block "deleting files from code is not allowed."

# Rule 3: no direct database access, no delete operations.
has "${S}(psql|mysql|mariadb|mongo|mongosh|sqlite3|redis-cli|pg_restore|pg_dump|cqlsh)${E}" \
  && block "direct database access is not allowed."
has "${S}(prisma|drizzle-kit|knex|sequelize|typeorm|supabase)${E}.*(reset|push|drop|execute|migrate|seed|db)" \
  && block "database migrations or resets are not allowed without the user."
has "(drop[[:space:]]+(table|database|schema|index|view|collection)|delete[[:space:]]+from|truncate[[:space:]]+(table[[:space:]]+)?[a-z_\"]|alter[[:space:]]+table)" \
  && block "destructive SQL is not allowed."
has "(deleteMany|deleteOne|findOneAndDelete|findByIdAndDelete|\.destroy\(|dropDatabase|dropCollection|dropTable|flushall|flushdb)" \
  && block "database delete operations are not allowed."
has "(-X|--request)[[:space:]]*['\"]?delete" && block "HTTP DELETE requests are not allowed."
has "${S}(node|bun|deno|python3?|ruby)${E}.*(-e|-c|-p|--eval)${E}" && has "\.delete\(" \
  && block "delete calls from inline code are not allowed."

# Rule 5: stay inside the repo.
has "${S}(sudo|su|doas)${E}" && block "running as another user is not allowed."
has "(^|[[:space:]/=\"'])\.\.(/|[[:space:]]|$|\"|')" && block "paths outside the repo (..) are not allowed."
has "(^|[[:space:]=\"'])(~|\\\$HOME|\\\$\{HOME\})" && block "the home folder is outside the repo."
for p in $(printf '%s' "$cmd" | grep -Eo "(^|[[:space:]=\"'(])/[^[:space:]\"';|&)]*" | sed -E "s/^[[:space:]=\"'(]//"); do
  case "$p" in
    /dev/null|/dev/stdout|/dev/stderr) ;;
    "$REPO_ROOT"|"$REPO_ROOT"/*) ;;
    *) block "path outside the repo is not allowed: $p" ;;
  esac
done

# Rule 8: network is locked unless the user unlocked it for a limited time.
if has "${S}(curl|wget|http|https|xh|nc|ncat|netcat|telnet|ssh|scp|sftp|rsync|ftp)${E}" \
  || has "${S}(npm|pnpm|yarn|bun)[[:space:]]+(install|i|add|update|upgrade|up|ci|dlx|create|x|publish)${E}" \
  || has "${S}(npx|bunx|pnpx)${E}" \
  || has "${S}(pip|pip3|pipx|uv)[[:space:]]+(install|add|sync|pip)" \
  || has "${S}(brew|docker)${E}"; then
  "$LIB/check-net.sh" || exit 2
fi

# Remote scripts piped into a shell.
has "(curl|wget).*\|[[:space:]]*(sh|bash|zsh|node|python)" && block "piping remote scripts into a shell is not allowed."

# Rule 7: guardrail files are read-only for agents.
has "net-unlock" && block "only the user can unlock the network."
# Harmless redirects (>/dev/null, 2>&1) are removed before checking for writes.
safe_cmd="$(printf '%s' "$cmd" | sed -E 's#[0-9]*>>?[[:space:]]*(/dev/(null|stdout|stderr)|&[0-9])##g')"
if has "(\.agents/|\.githooks|\.claude/settings\.json|\.cursor/hooks\.json|\.gemini/settings\.json|\.codex/)" \
  && printf '%s' "$safe_cmd" | grep -Eiq -- "(>|${S}(tee|mv|cp|chmod|chown|ln|touch|install|mkdir)${E}|sed[[:space:]]+(-[a-z]+[[:space:]]+)*-[a-z]*i|perl[[:space:]]+-[a-z]*i|writeFile|appendFile|createWriteStream|open\()"; then
  block "guardrail files can only be changed by the user."
fi

exit 0
'''

FILES[".agents/hooks/lib/check-net.sh"] = r'''#!/usr/bin/env sh
# Exit 0 if the user unlocked the network and the time limit has not passed. Otherwise block.
. "$(dirname "$0")/common.sh"

f="${GUARD_NET_FILE:-$(cd "$(dirname "$0")/../.." && pwd)/state/net-unlock-until}"
expiry="$(cat "$f" 2>/dev/null || true)"
case "$expiry" in ''|*[!0-9]*) expiry=0 ;; esac
[ "$(date +%s)" -lt "$expiry" ] && exit 0
block "network is locked. Ask the user to run: ./.agents/net-unlock.sh <minutes>"
'''

FILES[".agents/hooks/lib/check-mcp.sh"] = r'''#!/usr/bin/env sh
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
'''

FILES[".agents/hooks/claude.sh"] = r'''#!/usr/bin/env sh
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
'''

FILES[".agents/hooks/gemini.sh"] = r'''#!/usr/bin/env sh
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
'''

FILES[".agents/net-unlock.sh"] = r'''#!/usr/bin/env sh
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
'''

FILES[".agents/hooks/test.sh"] = r'''#!/usr/bin/env sh
# Self test for the guardrail hooks. Run: ./.agents/hooks/test.sh
H="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$H/../.." && pwd)"
pass=0; fail=0

# Network state for tests: locked by default, a temp file for the unlocked case.
LOCKED=/nonexistent/net-unlock-until
UNLOCKED="$(mktemp)"
trap 'rm -f "$UNLOCKED"' EXIT
echo $(( $(date +%s) + 600 )) > "$UNLOCKED"
export GUARD_NET_FILE="$LOCKED"
net_on()  { GUARD_NET_FILE="$UNLOCKED"; }
net_off() { GUARD_NET_FILE="$LOCKED"; }

expect() { # expect <block|allow> <description> <json>
  printf '%s' "$3" | "$H/claude.sh" >/dev/null 2>&1; code=$?
  if { [ "$1" = block ] && [ $code -eq 2 ]; } || { [ "$1" = allow ] && [ $code -eq 0 ]; }; then
    pass=$((pass+1))
  else
    fail=$((fail+1)); echo "FAIL ($1 expected, got exit $code): $2"
  fi
}
bash_json() { jq -cn --arg c "$1" '{tool_name:"Bash",tool_input:{command:$c}}'; }
file_json() { jq -cn --arg t "$1" --arg p "$2" '{tool_name:$t,tool_input:{file_path:$p}}'; }
WEB='{"tool_name":"WebFetch","tool_input":{"url":"https://example.com"}}'

# Rule 2: git is read-only, GitHub CLI blocked
expect allow "git status"         "$(bash_json 'git status')"
expect allow "git diff"           "$(bash_json 'git diff src')"
expect allow "git log"            "$(bash_json 'git log --oneline')"
expect block "git commit"         "$(bash_json 'git commit -m x')"
expect block "git push"           "$(bash_json 'git push')"
expect block "git add"            "$(bash_json 'git add .')"
expect block "status then push"   "$(bash_json 'git status && git push')"
expect block "push chained"       "$(bash_json 'npm test && git push')"
expect block "git -C push"        "$(bash_json 'git -C src push')"
expect block "no-verify"          "$(bash_json 'git commit --no-verify')"
expect block "gh pr"              "$(bash_json 'gh pr create')"
# Rule 3: database
expect block "psql"               "$(bash_json 'psql -c "select 1"')"
expect block "drop table"         "$(bash_json 'node run.js "DROP TABLE users"')"
expect block "delete from"        "$(bash_json 'echo "delete from bookings" | x')"
expect block "prisma reset"       "$(bash_json 'npx prisma migrate reset')"
expect block "deleteMany"         "$(bash_json 'node -e "db.users.deleteMany({})"')"
expect block "inline .delete("    "$(bash_json 'node -e "client.from(\"users\").delete()"')"
expect block "flushall"           "$(bash_json 'node -e "redis.flushall()"')"
net_on
expect block "http DELETE"        "$(bash_json 'curl -X DELETE https://api.example.com/users/1')"
net_off
expect block "mcp raw sql"        '{"tool_name":"mcp__insforge__run-raw-sql","tool_input":{"query":"select 1"}}'
expect block "mcp input delete"   '{"tool_name":"mcp__x__invoke","tool_input":{"code":"users.deleteMany()"}}'
# Rule 4: delete
expect block "rm"                 "$(bash_json 'rm -rf src')"
expect block "find delete"        "$(bash_json 'find . -name x -delete')"
expect block "node rmSync"        "$(bash_json 'node -e "fs.rmSync(\"a\")"')"
# Rule 5: outside the repo
expect block "cd parent"          "$(bash_json 'cd .. && ls')"
expect block "abs path outside"   "$(bash_json 'cat /etc/passwd')"
expect block "home"               "$(bash_json 'ls ~/Desktop')"
expect block "sudo"               "$(bash_json 'sudo ls')"
expect block "read outside"       "$(file_json Read /Users/someone/secret.txt)"
expect block "write outside"      "$(file_json Write /tmp/x.txt)"
expect block "read env"           "$(file_json Read "$REPO/.env")"
# Rule 7: guardrail files
expect block "edit hook"          "$(file_json Edit "$REPO/.agents/hooks/claude.sh")"
expect block "edit settings"      "$(file_json Write "$REPO/.claude/settings.json")"
expect block "write net state"    "$(file_json Write "$REPO/.agents/state/net-unlock-until")"
expect block "sed hook"           "$(bash_json 'sed -i "" s/a/b/ .agents/hooks/lib/common.sh')"
expect block "sed -E -i hook"     "$(bash_json 'sed -E -i "" s/a/b/ .agents/hooks/claude.sh')"
expect block "redirect into hook" "$(bash_json 'echo x > .agents/hooks/lib/common.sh')"
expect block "cp into hook"       "$(bash_json 'cp a.sh .agents/hooks/claude.sh')"
expect block "node write state"   "$(bash_json 'node -e "fs.writeFileSync(\".agents/state/net-unlock-until\",\"9999999999\")"')"
expect block "agent unlocks net"  "$(bash_json './.agents/net-unlock.sh 60')"
# Rule 8: network locked
expect block "curl locked"        "$(bash_json 'curl -s https://example.com/api')"
expect block "npm install locked" "$(bash_json 'npm install')"
expect block "npx locked"         "$(bash_json 'npx create-next-app app')"
expect block "webfetch locked"    "$WEB"
net_on
expect allow "curl unlocked"      "$(bash_json 'curl -s https://example.com/api')"
expect allow "npm install unlocked" "$(bash_json 'npm install')"
expect allow "webfetch unlocked"  "$WEB"
net_off
# Allowed
expect allow "npm run dev"        "$(bash_json 'npm run dev')"
expect allow "ls repo"            "$(bash_json 'ls -la src')"
expect allow "abs path inside"    "$(bash_json "cat $REPO/README.md")"
expect allow "edit src"           "$(file_json Edit "$REPO/src/app.ts")"
expect allow "read env example"   "$(file_json Read "$REPO/.env.example")"
expect allow "read hook"          "$(file_json Read "$REPO/.agents/hooks/claude.sh")"
expect allow "devnull redirect"   "$(bash_json 'jq . .claude/settings.json >/dev/null 2>&1')"
expect allow "word contains cp"   "$(bash_json 'cat .agents/README.md | grep scp')"

echo "passed: $pass, failed: $fail"
[ $fail -eq 0 ]
'''

FILES[".claude/settings.json"] = r'''{
  "permissions": {
    "defaultMode": "plan",
    "disableBypassPermissionsMode": "disable",
    "ask": [
      "Bash",
      "Write",
      "Edit",
      "MultiEdit",
      "NotebookEdit",
      "WebFetch",
      "WebSearch"
    ],
    "deny": [
      "Bash(gh *)",
      "Bash(rm *)",
      "Bash(rmdir *)",
      "Bash(sudo *)",
      "Read(./.env)",
      "Read(./.env.*)",
      "Edit(/.agents/**)",
      "Edit(/.githooks/**)",
      "Edit(/.claude/settings.json)",
      "Edit(/.cursor/hooks.json)",
      "Edit(/.gemini/settings.json)",
      "Edit(/.codex/**)"
    ]
  },
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "*",
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR\"/.agents/hooks/claude.sh"
          }
        ]
      }
    ]
  }
}
'''

root = Path(__file__).resolve().parent
for rel, content in FILES.items():
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content)
    if rel.endswith(".sh"):
        os.chmod(p, 0o755)
    print("wrote", rel)

gi = root / ".gitignore"
text = gi.read_text()
if ".agents/state/" not in text:
    gi.write_text(text.rstrip("\n") + "\n\n# Guardrail runtime state (network unlock timer)\n.agents/state/\n")
    print("updated .gitignore")

print("Done. Now run: ./.agents/hooks/test.sh")

#!/usr/bin/env sh
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
# Build tools and scripts
expect block "mvnw locked"        "$(bash_json 'cd backend && ./mvnw test')"
expect allow "mvnw offline"       "$(bash_json 'cd backend && ./mvnw -o test')"
net_on
expect allow "mvnw unlocked"      "$(bash_json 'cd backend && ./mvnw test')"
net_off
expect block "db script"          "$(bash_json './scripts/db/reset.sh')"
expect allow "dev script"         "$(bash_json './scripts/dev/status.sh')"
# Allowed
expect allow "npm run dev"        "$(bash_json 'npm run dev')"
expect allow "ls repo"            "$(bash_json 'ls -la src')"
expect allow "abs path inside"    "$(bash_json "cat $REPO/README.md")"
expect allow "edit src"           "$(file_json Edit "$REPO/src/app.ts")"
expect allow "read env example"   "$(file_json Read "$REPO/.env.example")"
expect allow "read hook"          "$(file_json Read "$REPO/.agents/hooks/claude.sh")"
expect allow "devnull redirect"   "$(bash_json 'jq . .claude/settings.json >/dev/null 2>&1')"
expect allow "word contains cp"   "$(bash_json 'cat .agents/README.md | grep escape')"

echo "passed: $pass, failed: $fail"
[ $fail -eq 0 ]

#!/usr/bin/env sh
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

# Database scripts are for the user only.
has "scripts/db/" && block "database scripts can only be run by the user."

# Rule 8: network is locked unless the user unlocked it for a limited time.
if has "${S}(curl|wget|http|https|xh|nc|ncat|netcat|telnet|ssh|scp|sftp|rsync|ftp)${E}" \
  || has "${S}(npm|pnpm|yarn|bun)[[:space:]]+(install|i|add|update|upgrade|up|ci|dlx|create|x|publish)${E}" \
  || has "${S}(npx|bunx|pnpx)${E}" \
  || has "${S}(pip|pip3|pipx|uv)[[:space:]]+(install|add|sync|pip)" \
  || has "${S}(brew|docker)${E}" \
  || { has "${S}(\./)?(mvn|mvnw|gradle|gradlew)${E}" && ! has "${S}(-o|--offline)${E}"; }; then
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

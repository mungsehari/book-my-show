"""Guardrail patch 2. Run once from the repo root: python3 guard-patch-2.py

Agents cannot edit guardrail files, so the user runs this.
Changes:
- Network: mvn, ./mvnw, gradle, ./gradlew need a network unlock, unless run offline (-o / --offline).
- Scripts: anything in scripts/db/ can only be run by the user.
- Test fix: the "word contains cp" test no longer uses "scp" (a network command).
After running, delete this file yourself.
"""
from pathlib import Path

root = Path(__file__).resolve().parent


def patch(rel, old, new):
    p = root / rel
    s = p.read_text()
    if new in s:
        print("already patched:", rel)
        return
    assert old in s, f"expected text not found in {rel}"
    p.write_text(s.replace(old, new, 1))
    print("patched:", rel)


cc = ".agents/hooks/lib/check-command.sh"

patch(cc,
r'''  || has "${S}(brew|docker)${E}"; then
  "$LIB/check-net.sh" || exit 2
fi
''',
r'''  || has "${S}(brew|docker)${E}" \
  || { has "${S}(\./)?(mvn|mvnw|gradle|gradlew)${E}" && ! has "${S}(-o|--offline)${E}"; }; then
  "$LIB/check-net.sh" || exit 2
fi
''')

patch(cc,
r'''# Rule 8: network is locked unless the user unlocked it for a limited time.
''',
r'''# Database scripts are for the user only.
has "scripts/db/" && block "database scripts can only be run by the user."

# Rule 8: network is locked unless the user unlocked it for a limited time.
''')

t = ".agents/hooks/test.sh"

s = (root / t).read_text()
if "grep scp" in s:
    (root / t).write_text(s.replace("grep scp", "grep escape"))
    print("patched:", t, "(scp test)")

patch(t,
r'''# Allowed
''',
r'''# Build tools and scripts
expect block "mvnw locked"        "$(bash_json 'cd backend && ./mvnw test')"
expect allow "mvnw offline"       "$(bash_json 'cd backend && ./mvnw -o test')"
net_on
expect allow "mvnw unlocked"      "$(bash_json 'cd backend && ./mvnw test')"
net_off
expect block "db script"          "$(bash_json './scripts/db/reset.sh')"
expect allow "dev script"         "$(bash_json './scripts/dev/status.sh')"
# Allowed
''')

print("Done. Now run: ./.agents/hooks/test.sh")

# .agents

Shared guardrails for all AI coding agents. Agents must never edit this folder. Only the user does.

| Path | Purpose |
|---|---|
| `hooks/lib/check-command.sh` | Blocks dangerous shell commands (git, delete, database, outside repo) |
| `hooks/lib/check-path.sh` | Blocks file access outside the repo, env files, and writes to guardrail files |
| `hooks/lib/check-mcp.sh` | Blocks MCP tools that touch the database or delete data |
| `hooks/lib/check-agent-git.sh` | Blocks git actions started from an AI agent shell |
| `hooks/claude.sh` | Claude Code adapter |
| `hooks/cursor.sh` | Cursor adapter |
| `hooks/gemini.sh` | Gemini CLI adapter |
| `hooks/pre-commit.sh`, `hooks/pre-push.sh` | Git hook logic |
| `hooks/test.sh` | Self test. Run after any change to the hooks. |
| `setup.sh` | Run once after cloning. Enables the git hooks. |

All logic lives in `hooks/lib/`. Adapters only translate each agent's input format.

Rules live in `/AGENTS.md`. Git hook wrappers live in `/.githooks/`.

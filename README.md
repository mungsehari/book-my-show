# Book My Show

A movie and event ticket booking app.

## Status

Backend boilerplate with read-only list APIs.

| API | Returns |
|---|---|
| `GET /api/v1/cities` | All cities |
| `GET /api/v1/cities/{cityId}/cinemas` | Cinemas in a city |
| `GET /api/v1/cinemas/{cinemaId}/shows?date=yyyy-MM-dd` | Shows at a cinema on a date (default today) |

Stack: Java 21, Spring Boot 4, PostgreSQL, Flyway, Docker Compose. See `AGENTS.md` for structure and rules.

## Planned features

- Browse movies and events by city
- View showtimes and venues
- Pick seats and book tickets
- User accounts and booking history

## Getting started

After cloning, enable the shared git hooks:

```bash
./.agents/setup.sh
```

Create your env file and set `DB_PASSWORD`:

```bash
cp .env.example .env
```

Start Postgres:

```bash
docker compose up -d
```

Run the backend with sample data:

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Then open http://localhost:8080/swagger-ui.html

Run format check and tests:

```bash
./scripts/ci/check.sh
```

## AI agent setup

Works the same with Claude Code, Cursor, Codex, Antigravity, Gemini, Grok, and others.

| File | Read by | Role |
|---|---|---|
| `AGENTS.md` | Codex, Cursor, Grok, most agents | Source of truth for all rules |
| `CLAUDE.md` | Claude Code | Imports `AGENTS.md` |
| `GEMINI.md` | Gemini CLI, Antigravity | Imports `AGENTS.md` |
| `.cursor/rules/agents.mdc` | Cursor | Points to `AGENTS.md` |
| `.agents/hooks/` | All agents | Shared guardrail hook logic |
| `.claude/settings.json` | Claude Code | Plan mode, ask before actions, hooks |
| `.cursor/hooks.json` | Cursor | Hooks |
| `.gemini/settings.json` | Gemini CLI | Hooks, no YOLO mode |
| `.codex/config.toml` | Codex | Ask before commands, repo-only sandbox |
| `.githooks/` | Git | Blocks agent commits and pushes |

Guardrails are listed in `AGENTS.md`. Test the hooks with `./.agents/hooks/test.sh`.

Network is locked for agents by default. To unlock it for a limited time (max 120 minutes):

```bash
./.agents/net-unlock.sh 15
```

Lock it again early with `./.agents/net-unlock.sh 0`.

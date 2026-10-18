# Book My Show

A movie and event ticket booking app.

## Status

Backend boilerplate with read-only list APIs.

| API | Returns |
|---|---|
| `GET /api/v1/cities` | All cities |
| `GET /api/v1/cities/{cityId}/cinemas` | Cinemas in a city |
| `GET /api/v1/cinemas/{cinemaId}/shows?date=yyyy-MM-dd` | Shows at a cinema on a date (default today) |
| `GET /api/v1/cities/{cityId}/movies?date=yyyy-MM-dd` | Movies playing in a city on a date (default next 7 days) |
| `GET /api/v1/movies?language=Hindi` | All movies, or movies in one language |
| `GET /api/v1/movies/{movieId}` | One movie |

Stack: Java 21, Spring Boot 4, PostgreSQL, Flyway, Docker Compose. See `AGENTS.md` for structure and rules.

## Planned features

Full list with phases and technical notes: [docs/ROADMAP.md](docs/ROADMAP.md).

| Phase | Focus |
|---|---|
| 1 | Core booking: movies, seat map, seat lock, payment, ticket, login |
| 2 | Group booking: poll, split payment, seats together, waitlist |
| 3 | Trust and resale: verified reviews, face-value resale |
| 4 | Smart pricing: last-minute deals, price alerts, crowd meter |
| 5 | Experience: seat view preview, quiet shows, accessibility |
| 6 | AI: mood search, weekend planner |

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

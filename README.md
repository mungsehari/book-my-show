# Book My Show

A movie and event ticket booking app.

## Status

Backend boilerplate with read-only list APIs.

| API | Returns |
|---|---|
| `GET /api/v1/cities` | All cities |
| `GET /api/v1/cities/{cityId}/cinemas` | Cinemas in a city |
| `GET /api/v1/cinemas/{cinemaId}/shows?date=yyyy-MM-dd` | Shows at a cinema on a date (default today). Filters: `quiet`, `sensoryFriendly`, `subtitles`, `wheelchair` (each `true` to apply). |
| `GET /api/v1/cinemas/{cinemaId}/screens` | Screens in a cinema, with seat counts |
| `GET /api/v1/screens/{screenId}/layout` | Seat layout of a screen, grouped by row |
| `GET /api/v1/shows/{showId}/seats` | Seat map of a show: price, status, and wheelchair flag of every seat |
| `GET /api/v1/shows/{showId}/crowd` | Crowd meter: sold, held, available, percent full, and level (LOW, MEDIUM, HIGH, HOUSEFULL) |
| `GET /api/v1/movies/{movieId}/reviews` | Verified reviews, newest first, with the average rating |
| `GET /api/v1/cities/{cityId}/movies?date=yyyy-MM-dd` | Movies playing in a city on a date (default next 7 days) |
| `GET /api/v1/movies?language=Hindi` | All movies, or movies in one language |
| `GET /api/v1/movies/{movieId}` | One movie |

Auth (no token needed, except `me`):

| API | Does |
|---|---|
| `POST /api/v1/auth/register` | Sign up with name, email, password. Returns an access token. |
| `POST /api/v1/auth/login` | Log in with email and password |
| `POST /api/v1/auth/otp/request` | Send a 6-digit code to a phone (`+91...`). In dev the code is in the backend log. |
| `POST /api/v1/auth/otp/verify` | Log in with the phone code. A new phone gets a new account. |
| `POST /api/v1/auth/google` | Log in with a Google ID token from the frontend |
| `GET /api/v1/auth/me` | The logged-in user |

Admin (send `Authorization: Bearer <token>`):

| API | Who |
|---|---|
| `POST /api/v1/admin/movies` | ADMIN |
| `POST /api/v1/admin/cinemas/{cinemaId}/admins` | ADMIN. Makes the user a theatre admin of that cinema. |
| `POST /api/v1/admin/cinemas/{cinemaId}/screens` | ADMIN, or a theatre admin of that cinema. Each row can list `wheelchairSeats` (seat numbers). |
| `POST /api/v1/admin/screens/{screenId}/shows` | ADMIN, or a theatre admin of that cinema. Needs a price for every seat category on the screen, for example `"prices": {"REGULAR": 200, "PREMIUM": 300}`. Optional `quiet`, `sensoryFriendly`, `subtitles`. Rejects overlapping shows. |

The first admin: put your email in `ADMIN_EMAILS` in `.env`, then sign up with it.

Seat locks (any logged-in user):

| API | Does |
|---|---|
| `POST /api/v1/shows/{showId}/locks` | Hold up to 10 seats for 10 minutes, for example `{"seatIds": [12, 13]}`. All or none. One lock per user per show. Returns `lockId` and the total price. |
| `POST /api/v1/locks/{lockId}/release` | Give the seats back early |

Held seats show as `LOCKED` on the seat map for everyone.

Bookings and payment (Razorpay test mode):

| API | Does |
|---|---|
| `POST /api/v1/bookings` | Start payment for a lock, `{"lockId": "..."}`. Returns the Razorpay order ID, amount in paise, and key ID for checkout. Calling it again returns the same order. |
| `POST /api/v1/bookings/{bookingId}/confirm` | Send what Razorpay checkout returned (`razorpayOrderId`, `razorpayPaymentId`, `razorpaySignature`). Checks the signature and sells the seats. |
| `GET /api/v1/bookings/{bookingId}` | Your booking |
| `GET /api/v1/bookings/me?when=upcoming&page=0&size=20` | Your bookings. `upcoming` (default, soonest first) or `past` (newest first). Unpaid checkouts are left out. Max 50 per page. |
| `GET /api/v1/bookings/{bookingId}/ticket` | Ticket for a confirmed booking. The frontend shows `ticketCode` as a QR code. |
| `POST /api/v1/admin/tickets/check-in` | Entry scan, `{"ticketCode": "..."}`. ADMIN, or a theatre admin of that cinema. Works once per ticket, only on the show day. |
| `POST /api/v1/payments/razorpay/webhook` | Called by Razorpay. Checks `X-Razorpay-Signature`. Confirms the booking even if the user closed the page. |

Reviews: `POST /api/v1/movies/{movieId}/reviews` with `{"rating": 1-5, "text": "..."}`. Only users with a paid booking whose show has started (or who were checked in) can post, once per movie.

Sold seats show as `BOOKED`. A seat can be sold only once (a unique key in the database). If a payment arrives after the seats were sold to someone else, the booking becomes `NEEDS_REFUND`.

Set the Razorpay test keys in `.env`. For the webhook on your laptop, use a tunnel (for example `ngrok http 8080`) and add its URL in the Razorpay dashboard with the events `payment.captured` and `payment.failed`.

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

Create your env file and set `DB_PASSWORD`, `REDIS_PASSWORD`, and `JWT_SECRET`:

```bash
cp .env.example .env
```

Start Postgres and Redis:

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

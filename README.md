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
| `GET /api/v1/screens/{screenId}/views` | Seat view photos: what the screen looks like from each block of rows |
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

Cancel, refund, resale, waitlist:

| API | Does |
|---|---|
| `POST /api/v1/bookings/{bookingId}/cancel` | Cancel with a full refund, up to 2 hours before the show. Freed seats go to the waitlist first. |
| `POST /api/v1/admin/bookings/{bookingId}/refund` | ADMIN. Refund a `NEEDS_REFUND` booking. |
| `GET /api/v1/shows/{showId}/resale` | Resale tickets for a show, at face value |
| `POST /api/v1/bookings/{bookingId}/resale` | Offer your whole ticket for resale (and `/resale/cancel` to stop) |
| `POST /api/v1/resale/{bookingId}/checkout` | Buy a resale ticket at the price the owner paid. Confirm with `/bookings/{id}/confirm`. The owner is refunded in full. |
| `POST /api/v1/shows/{showId}/waitlist` | Join the waitlist of a full show, `{"seats": 2}` |
| `GET /api/v1/waitlist/me?showId=` | Your place in the queue, or the `lockId` of seats held for you |
| `POST /api/v1/waitlist/{entryId}/leave` | Leave the waitlist |

Last-minute deals: in the last hour before a show, if it is less than 70% full, every seat costs 20% less (rounded to whole rupees). The seat map shows `deal` and each category's `originalPrice`. Admins can send `"lastMinuteDeals": false` when creating a show to keep it out.

Price alerts (login needed): `POST /api/v1/price-alerts` with `{"movieId", "cityId", "maxPrice"}`, `GET /api/v1/price-alerts/me`, `POST /api/v1/price-alerts/{id}/cancel`. A job checks every 5 minutes and tells you once, about the soonest show in the next 7 days that is cheap enough. Up to 10 active alerts.

Seat view photos (admin): `POST /api/v1/admin/screens/{screenId}/views` with `{"fromRow": "A", "toRow": "C", "imageUrl": "https://...", "caption"}`. Blocks on a screen may not overlap. Images are hosted elsewhere.

Social (login needed):

| API | Does |
|---|---|
| `POST /api/v1/friends/requests` | Send a friend request by exact email. If they already asked you, you become friends. |
| `GET /api/v1/friends/requests` | Requests sent to you. Accept or decline with `/{id}/accept`, `/{id}/decline`. |
| `GET /api/v1/friends` | Your friends. Remove one with `POST /api/v1/friends/{userId}/remove`. |
| `GET /api/v1/friends/activity?movieId=` | Friends who watched the movie, and friends going to a show of it |
| `GET/POST /api/v1/me/privacy` | `{"shareActivity": false}` hides your activity from friends |
| `POST /api/v1/bookings/{bookingId}/gift` | Give your ticket to another user by email, up to 2 hours before the show. No money moves. |
| `POST /api/v1/polls` | Ask friends to vote: `{"title", "showIds": [2-5 shows], "friendIds": [...]}` |
| `GET /api/v1/polls/mine`, `GET /api/v1/polls/{id}` | Your polls, with vote counts |
| `POST /api/v1/polls/{id}/vote`, `POST /api/v1/polls/{id}/close` | Vote (changeable). The creator closes: most votes wins, the earlier show wins a tie. |

Split payment (login needed):

| API | Does |
|---|---|
| `POST /api/v1/bookings/split` | Split locked seats with 1 to 9 friends: `{"lockId", "friendIds": [...]}`. Everyone gets an equal share (the organizer also pays any leftover paise) and their own Razorpay order. The seat lock is extended to 15 minutes. |
| `GET /api/v1/payment-shares/me` | Your unpaid shares, with the Razorpay order to pay |
| `POST /api/v1/payment-shares/{shareId}/confirm` | Confirm your share with what Razorpay checkout returned. The webhook does the same. |

When the last share is paid the seats are sold and the booking is confirmed. If 15 minutes pass first, or the seats were lost, everyone who paid is refunded. The organizer owns the ticket; friends can see the booking and ticket. Cancel, resale, and gift are not available for split bookings yet.

AI and planning:

| API | Does |
|---|---|
| `GET /api/v1/movies/search?q=kuch halka funny, 2 ghante se kam&cityId=1` | Mood search (public). Claude reads the request into filters; the movies always come from our catalog. Falls back to keyword rules without a key, on errors, or past 20 AI calls a minute per client. Answers are cached for a day. `source` says which was used. |
| `POST /api/v1/planner` | Weekend planner (login needed): `{"cityId", "date", "mood", "people", "budgetPerPerson", "time": "MORNING|AFTERNOON|EVENING"}`. Up to 3 bookable plans: show, group price, nearest food and parking. |
| `GET /api/v1/cinemas/{cinemaId}/places` | Food and parking near a cinema. Admins add them with `POST /api/v1/admin/cinemas/{cinemaId}/places`. |

Set `ANTHROPIC_API_KEY` in `.env` to turn on Claude (`CLAUDE_MODEL` defaults to `claude-opus-5-5`).

Jobs run every minute: unpaid checkouts expire after 15 minutes, and waitlist offers nobody took in 10 minutes go to the next person. Notifications (waitlist offers, refunds) are written to the backend log for now.

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

The whole schema is one migration, `V1__init_schema.sql`. It works only on an empty database. If your local database was made by the older V1 to V15 migrations, reset it first (the `postgres-data` Docker volume), or Flyway will fail on a checksum mismatch. New schema changes go in `V2__...` and later.

Run the backend with sample data:

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Then open http://localhost:8080/swagger-ui.html

Run format check and tests:

```bash
./scripts/ci/check.sh
```

Run the frontend (Next.js, needs the backend running):

```bash
cd frontend && cp .env.example .env.local && npm install && npm run dev
```

Then open http://localhost:3000. Add `API_URL=http://localhost:8081` to `frontend/.env.local` for login.

| Stage | What works |
|---|---|
| F1 | Home (city picker, now showing), movie page (showtimes by cinema), dark mode |
| F2 | Login (email, phone OTP, register), account menu, My bookings (upcoming and past) |
| F3 | Seat map (live, refreshed every 5 seconds), seat selection, 10 minute seat hold with countdown |
| F4 | Razorpay test payment, booking confirm, QR ticket page, cancel |
| F9b | Split payment: "Split with friends" on the seat hold, pay your share, `/shares` page for shares friends sent you |
| F9a | Weekend planner (`/planner`), gift a ticket, resell at face value, buy resale tickets on the seat page |
| F8 | Group polls: create with 2 to 5 shows and friends, live vote bars, share link, close and book the winner |
| F7 | Friends page (add by email, accept or decline, remove), activity privacy switch, "friends watched this" on the movie page |
| F6 | Waitlist on housefull shows (join, position, pay when seats are offered), price alerts (set on the movie page, list at `/alerts`) |
| F5 | Mood search on the home page (AI when the Claude key is set, keyword fallback), movie reviews with star rating |

For F4 set the Razorpay test keys in the root `.env` (backend). In test mode use card `4111 1111 1111 1111`, any future expiry and CVV, OTP `1234`. The webhook needs a tunnel, so on a laptop the confirm call from the browser is the main path.

Phone OTP codes are printed in the backend console.

Google sign-in (optional):

- In Google Cloud Console, create an OAuth client ID of type Web application. Add `http://localhost:3000` under Authorized JavaScript origins.
- Put the same client ID in two places: `GOOGLE_CLIENT_ID` in the root `.env` (backend) and `NEXT_PUBLIC_GOOGLE_CLIENT_ID` in `frontend/.env.local`.
- Without the client ID the Google button is hidden and the other logins still work.

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

# Roadmap

Features planned for Book My Show, grouped in phases.

- Build phases in order. Each phase needs its own plan and approval.
- Each feature lists what it does and the main technical challenge.

Status key: Done, Partial, Planned.

## Phase 1: Core booking

The base flow. Everything else builds on it.

| Feature | What it does | Technical challenge | Status |
|---|---|---|---|
| City select | User picks a city and sees what plays there | Simple list API | Done (`GET /api/v1/cities`) |
| Cinemas and shows | Cinemas in a city, shows by date | Date filters, indexes | Done (list APIs) |
| Movies and events | Poster, rating, language, genre, duration | Catalog model shared by movies and events | Partial (movies APIs done, events planned) |
| Seat map | Seat layout per screen, price per seat category | Layout model per screen, per-show availability | Planned |
| Seat lock | Hold seats for 5 to 10 minutes during checkout | Two users clicking the same seat. Redis lock with a TTL | Planned |
| Payment | Pay in test mode (Razorpay or Stripe) | Webhooks, idempotency, release seats on failure | Planned |
| Ticket | Booking confirmation with a QR code | Signed QR payload to stop fake tickets | Planned |
| Login | OTP or Google sign-in | JWT, refresh tokens, rate limit on OTP | Planned |
| Booking history | Past and upcoming bookings | Pagination | Planned |
| Theatre admin panel | Theatre owners add screens, seat layouts, and shows | Role-based access, no overlapping shows on a screen | Planned |
| All-inclusive price | Final price on the first screen, no fees added at checkout | One price calculator used by listing and checkout | Planned |

### Phase 1 build order

Each step is one small change with its own plan and approval.

| Step | Work | Needs | Status |
|---|---|---|---|
| 1.1 | Link shows to movies (`shows.movie_id`) | Nothing | Done (nullable for now) |
| 1.2 | Movies playing in a city API | Nothing | Done |
| 1.3 | Screens and seat layout | Nothing | Done (read APIs, seed data) |
| 1.4 | Theatre admin panel APIs | 1.3, 1.5 | Done (create movie, screen, show; assign theatre admin) |
| 1.5 | Users and login (JWT) | Network unlock | Done (email, phone OTP with console SMS, Google) |
| 1.6 | Seat map and availability per show | 1.3 | Planned |
| 1.7 | Seat lock with Redis | Network unlock, Redis compose file | Planned |
| 1.8 | Booking and test payment with webhook | Network unlock, test keys in `.env` | Planned |
| 1.9 | Ticket with signed QR | 1.8 | Planned |
| 1.10 | Booking history | 1.8 | Planned |
| 1.11 | Next.js frontend | Network unlock | Planned |

Clean-up after 1.1: once every show has a movie, make `movie_id` NOT NULL and drop `movie_name`. The user approves and runs that migration.

## Phase 2: Group booking

| Feature | What it does | Technical challenge |
|---|---|---|
| Group poll | Friends vote on a movie and a show | Shared poll link, live vote count |
| Split payment | One person locks seats, each friend pays their share | Longer seat lock, partial payments, refund if the group does not finish |
| Seats together | Show only shows where the whole group can sit side by side | Search for N adjacent free seats |
| Waitlist | When a housefull show gets a cancellation, offer the seat to the next person | Queue per show, offer timeout, fair order |
| Virtual waiting room | Queue users fairly when a big release opens for booking | Admission tokens, rate limits, protect the seat lock service |
| Gift ticket | Book for someone else and send them a link | Transfer the ticket to the receiver's account |

## Phase 3: Trust and resale

| Feature | What it does | Technical challenge |
|---|---|---|
| Verified reviews | Only people who booked and watched can review | Link each review to a used ticket |
| Ticket resale | Resell a ticket at face value only | Ownership transfer, new QR, block price markup |
| Friends activity | "3 friends watched this movie" | Friend graph, privacy settings |

## Phase 4: Smart pricing

| Feature | What it does | Technical challenge |
|---|---|---|
| Last-minute deals | Empty seats get cheaper close to show time | Price rules engine, scheduled jobs |
| Price alerts | Notify when a ticket drops below a set price | Alert matching, notifications |
| Crowd meter | Show how full a show is, live | Cached seat counts (Redis) |
| Movie pass | Monthly subscription, for example 4 movies for a fixed price | Recurring billing, usage limits |
| Cancellation protection | Pay a small fee for a full refund up to 2 hours before the show | Refund rules, release seats back to sale |

## Phase 5: Experience

| Feature | What it does | Technical challenge |
|---|---|---|
| Seat view preview | See the screen from a chosen seat | Image per seat zone, CDN |
| Quiet show tag | Shows with fewer kids or smaller crowds | Tags on shows, filters |
| Accessibility filters | Wheelchair seats, subtitles, sensory-friendly shows | Seat and show attributes, filters |
| Food pre-order | Order snacks with the ticket, delivered to the seat at the interval | Order per booking, kitchen queue per cinema |
| Wallet pass | Add the ticket to Google Wallet or Apple Wallet | Signed pass files per provider |

## Phase 6: AI

| Feature | What it does | Technical challenge |
|---|---|---|
| Mood search | "Something light and funny, under 2 hours" returns movies | LLM call through `integrations/`, search on genre and length |
| Weekend planner | Plan movie, food, and parking together | Third-party APIs, combined itinerary |

## Tech added per phase

| Phase | New tech |
|---|---|
| 1 | Redis (seat lock), payment provider (test mode), JWT, Next.js frontend |
| 2 | Scheduled jobs, notifications |
| 3 | No new infra expected |
| 4 | Rules engine, scheduled jobs |
| 5 | Object storage and CDN for images |
| 6 | LLM provider, Elasticsearch (search) |

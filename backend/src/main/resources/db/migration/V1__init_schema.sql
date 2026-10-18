-- Full schema in one migration. It is the end result of the earlier V1 to V15 migrations, written directly:
-- no ALTER, no DROP. Use it on an empty database. Sample data lives in db/seed, not here.

-- Browse -------------------------------------------------------------------------------------------------------------

CREATE TABLE cities (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    CONSTRAINT uq_cities_name_state UNIQUE (name, state)
);

CREATE TABLE cinemas (
    id      BIGSERIAL PRIMARY KEY,
    city_id BIGINT       NOT NULL REFERENCES cities (id),
    name    VARCHAR(150) NOT NULL,
    address VARCHAR(255) NOT NULL,
    CONSTRAINT uq_cinemas_city_name UNIQUE (city_id, name)
);

CREATE INDEX idx_cinemas_city_id ON cinemas (city_id);

CREATE TABLE movies (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(200)  NOT NULL,
    description      VARCHAR(1000) NOT NULL,
    language         VARCHAR(50)   NOT NULL,
    genre            VARCHAR(50)   NOT NULL,
    duration_minutes INTEGER       NOT NULL CHECK (duration_minutes > 0),
    certificate      VARCHAR(5)    NOT NULL CHECK (certificate IN ('U', 'UA', 'A')),
    release_date     DATE          NOT NULL,
    poster_url       VARCHAR(500),
    CONSTRAINT uq_movies_title_language UNIQUE (title, language)
);

CREATE INDEX idx_movies_language ON movies (language);

-- Screens and seats --------------------------------------------------------------------------------------------------

CREATE TABLE screens (
    id        BIGSERIAL PRIMARY KEY,
    cinema_id BIGINT      NOT NULL REFERENCES cinemas (id),
    name      VARCHAR(50) NOT NULL,
    CONSTRAINT uq_screens_cinema_name UNIQUE (cinema_id, name)
);

CREATE INDEX idx_screens_cinema_id ON screens (cinema_id);

CREATE TABLE seats (
    id          BIGSERIAL PRIMARY KEY,
    screen_id   BIGINT      NOT NULL REFERENCES screens (id),
    row_label   VARCHAR(1)  NOT NULL CHECK (row_label ~ '^[A-Z]$'),
    seat_number INTEGER     NOT NULL CHECK (seat_number > 0),
    category    VARCHAR(20) NOT NULL CHECK (category IN ('REGULAR', 'PREMIUM', 'RECLINER')),
    wheelchair  BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_seats_position UNIQUE (screen_id, row_label, seat_number)
);

CREATE TABLE screen_views (
    id         BIGSERIAL PRIMARY KEY,
    screen_id  BIGINT       NOT NULL REFERENCES screens (id),
    from_row   VARCHAR(1)   NOT NULL CHECK (from_row ~ '^[A-Z]$'),
    to_row     VARCHAR(1)   NOT NULL CHECK (to_row ~ '^[A-Z]$'),
    image_url  VARCHAR(500) NOT NULL,
    caption    VARCHAR(200),
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_screen_views_rows CHECK (from_row <= to_row)
);

CREATE INDEX idx_screen_views_screen ON screen_views (screen_id, from_row);

-- Shows --------------------------------------------------------------------------------------------------------------

-- movie_id and screen_id are nullable: older shows may have neither. movie_name is kept as a copy of the title.
CREATE TABLE shows (
    id               BIGSERIAL PRIMARY KEY,
    cinema_id        BIGINT         NOT NULL REFERENCES cinemas (id),
    movie_id         BIGINT REFERENCES movies (id),
    screen_id        BIGINT REFERENCES screens (id),
    movie_name       VARCHAR(200)   NOT NULL,
    show_date        DATE           NOT NULL,
    start_time       TIME           NOT NULL,
    price            NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    quiet            BOOLEAN        NOT NULL DEFAULT FALSE,
    sensory_friendly BOOLEAN        NOT NULL DEFAULT FALSE,
    subtitles        BOOLEAN        NOT NULL DEFAULT FALSE,
    deals_enabled    BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_shows_screen_slot UNIQUE (screen_id, show_date, start_time)
);

CREATE INDEX idx_shows_cinema_date ON shows (cinema_id, show_date);
CREATE INDEX idx_shows_movie_id ON shows (movie_id);
CREATE INDEX idx_shows_screen_date ON shows (screen_id, show_date);

-- Price per seat category for each show. shows.price keeps the lowest price.
CREATE TABLE show_prices (
    id       BIGSERIAL PRIMARY KEY,
    show_id  BIGINT         NOT NULL REFERENCES shows (id),
    category VARCHAR(20)    NOT NULL CHECK (category IN ('REGULAR', 'PREMIUM', 'RECLINER')),
    price    NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    CONSTRAINT uq_show_prices_show_category UNIQUE (show_id, category)
);

-- Users and login ----------------------------------------------------------------------------------------------------

-- A user signs in with email and password, phone OTP, or Google. At least one identity is required.
CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(255),
    phone          VARCHAR(16),
    password_hash  VARCHAR(100),
    google_subject VARCHAR(255),
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('USER', 'THEATRE_ADMIN', 'ADMIN')),
    created_at     TIMESTAMPTZ  NOT NULL,
    share_activity BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT uq_users_google_subject UNIQUE (google_subject),
    CONSTRAINT chk_users_identity CHECK (email IS NOT NULL OR phone IS NOT NULL OR google_subject IS NOT NULL)
);

-- Only a hash of each code is stored. The newest code per phone is the one that counts.
CREATE TABLE otp_codes (
    id         BIGSERIAL PRIMARY KEY,
    phone      VARCHAR(16)  NOT NULL,
    code_hash  VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    attempts   INTEGER      NOT NULL CHECK (attempts >= 0),
    used       BOOLEAN      NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_otp_codes_phone_created ON otp_codes (phone, created_at DESC);

-- Which theatre admins manage which cinema.
CREATE TABLE cinema_admins (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    cinema_id  BIGINT      NOT NULL REFERENCES cinemas (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_cinema_admins_user_cinema UNIQUE (user_id, cinema_id)
);

CREATE INDEX idx_cinema_admins_cinema_id ON cinema_admins (cinema_id);

-- Food and parking near each cinema, entered by the theatre.
CREATE TABLE cinema_places (
    id           BIGSERIAL PRIMARY KEY,
    cinema_id    BIGINT       NOT NULL REFERENCES cinemas (id),
    type         VARCHAR(20)  NOT NULL CHECK (type IN ('FOOD', 'PARKING')),
    name         VARCHAR(150) NOT NULL,
    walk_minutes INTEGER      NOT NULL CHECK (walk_minutes BETWEEN 0 AND 60),
    notes        VARCHAR(300),
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_cinema_places_cinema ON cinema_places (cinema_id, type, walk_minutes);

-- Bookings and sold seats --------------------------------------------------------------------------------------------

-- Show details (date, movie, cinema, screen, seat labels) are copied here at checkout, so tickets and history keep
-- what was bought. seat_ids and seat_labels are comma-separated.
CREATE TABLE bookings (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT         NOT NULL REFERENCES users (id),
    show_id               BIGINT         NOT NULL REFERENCES shows (id),
    status                VARCHAR(20)    NOT NULL CHECK (status IN (
        'PENDING_PAYMENT', 'CONFIRMED', 'FAILED', 'NEEDS_REFUND', 'EXPIRED', 'CANCELLED', 'REFUNDED', 'RESOLD')),
    seat_ids              VARCHAR(250)   NOT NULL,
    amount                NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    currency              VARCHAR(3)     NOT NULL,
    lock_id               VARCHAR(36)    NOT NULL,
    razorpay_order_id     VARCHAR(50),
    razorpay_payment_id   VARCHAR(50),
    razorpay_refund_id    VARCHAR(50),
    created_at            TIMESTAMPTZ    NOT NULL,
    confirmed_at          TIMESTAMPTZ,
    cancelled_at          TIMESTAMPTZ,
    refunded_at           TIMESTAMPTZ,
    show_date             DATE           NOT NULL,
    start_time            TIME           NOT NULL,
    movie_name            VARCHAR(200)   NOT NULL,
    movie_id              BIGINT REFERENCES movies (id),
    cinema_id             BIGINT         NOT NULL REFERENCES cinemas (id),
    screen_name           VARCHAR(50)    NOT NULL,
    seat_labels           VARCHAR(250)   NOT NULL,
    checked_in_at         TIMESTAMPTZ,
    checked_in_by         BIGINT REFERENCES users (id),
    resale_listed_at      TIMESTAMPTZ,
    resale_of_booking_id  BIGINT REFERENCES bookings (id),
    gifted_from_user_id   BIGINT REFERENCES users (id),
    split                 BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_bookings_lock_id UNIQUE (lock_id),
    CONSTRAINT uq_bookings_razorpay_order_id UNIQUE (razorpay_order_id)
);

CREATE INDEX idx_bookings_user_created ON bookings (user_id, created_at DESC);
CREATE INDEX idx_bookings_user_show_date ON bookings (user_id, show_date);
CREATE INDEX idx_bookings_user_movie ON bookings (user_id, movie_id);
CREATE INDEX idx_bookings_status_created ON bookings (status, created_at);
CREATE INDEX idx_bookings_resale_of ON bookings (resale_of_booking_id);
CREATE INDEX idx_bookings_split_status ON bookings (split, status, created_at);

-- One row per sold seat. A cancel sets released_at, and the seat can be sold again. Only one active row (released_at
-- null) can exist per show and seat, so a seat is never sold twice.
CREATE TABLE show_booked_seats (
    id          BIGSERIAL PRIMARY KEY,
    show_id     BIGINT      NOT NULL REFERENCES shows (id),
    seat_id     BIGINT      NOT NULL REFERENCES seats (id),
    booking_id  BIGINT      NOT NULL REFERENCES bookings (id),
    created_at  TIMESTAMPTZ NOT NULL,
    released_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_show_booked_seats_active ON show_booked_seats (show_id, seat_id) WHERE released_at IS NULL;
CREATE INDEX idx_show_booked_seats_booking ON show_booked_seats (booking_id);

-- Split payment: each person pays a share of one booking. VOID if the booking expired before they paid.
CREATE TABLE payment_shares (
    id                  BIGSERIAL PRIMARY KEY,
    booking_id          BIGINT         NOT NULL REFERENCES bookings (id),
    user_id             BIGINT         NOT NULL REFERENCES users (id),
    amount              NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    currency            VARCHAR(3)     NOT NULL,
    status              VARCHAR(20)    NOT NULL CHECK (status IN ('PENDING', 'PAID', 'REFUNDED', 'VOID')),
    razorpay_order_id   VARCHAR(50),
    razorpay_payment_id VARCHAR(50),
    razorpay_refund_id  VARCHAR(50),
    created_at          TIMESTAMPTZ    NOT NULL,
    paid_at             TIMESTAMPTZ,
    refunded_at         TIMESTAMPTZ,
    CONSTRAINT uq_payment_shares_booking_user UNIQUE (booking_id, user_id),
    CONSTRAINT uq_payment_shares_order UNIQUE (razorpay_order_id)
);

CREATE INDEX idx_payment_shares_user_status ON payment_shares (user_id, status);

-- Friends in a split booking. They can see the booking and its ticket.
CREATE TABLE booking_members (
    id         BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings (id),
    user_id    BIGINT NOT NULL REFERENCES users (id),
    CONSTRAINT uq_booking_members UNIQUE (booking_id, user_id)
);

CREATE INDEX idx_booking_members_user ON booking_members (user_id);

-- Waitlist, reviews, alerts ------------------------------------------------------------------------------------------

CREATE TABLE waitlist_entries (
    id               BIGSERIAL PRIMARY KEY,
    show_id          BIGINT      NOT NULL REFERENCES shows (id),
    user_id          BIGINT      NOT NULL REFERENCES users (id),
    seats_wanted     INTEGER     NOT NULL CHECK (seats_wanted BETWEEN 1 AND 10),
    status           VARCHAR(20) NOT NULL CHECK (status IN ('WAITING', 'OFFERED', 'BOOKED', 'EXPIRED', 'LEFT')),
    lock_id          VARCHAR(36),
    offered_seat_ids VARCHAR(250),
    offered_at       TIMESTAMPTZ,
    offer_expires_at TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL
);

-- One active entry per user per show.
CREATE UNIQUE INDEX uq_waitlist_active ON waitlist_entries (show_id, user_id) WHERE status IN ('WAITING', 'OFFERED');
CREATE INDEX idx_waitlist_show_status ON waitlist_entries (show_id, status, created_at);
CREATE INDEX idx_waitlist_lock ON waitlist_entries (lock_id);

-- One review per user per movie, tied to the booking that proves they watched it.
CREATE TABLE reviews (
    id         BIGSERIAL PRIMARY KEY,
    movie_id   BIGINT        NOT NULL REFERENCES movies (id),
    user_id    BIGINT        NOT NULL REFERENCES users (id),
    booking_id BIGINT        NOT NULL REFERENCES bookings (id),
    rating     INTEGER       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    text       VARCHAR(1000),
    created_at TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_reviews_movie_user UNIQUE (movie_id, user_id)
);

CREATE INDEX idx_reviews_movie_created ON reviews (movie_id, created_at DESC);

-- "Tell me when a ticket for this movie in this city is at or below this price." One message, then TRIGGERED.
CREATE TABLE price_alerts (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT         NOT NULL REFERENCES users (id),
    movie_id          BIGINT         NOT NULL REFERENCES movies (id),
    city_id           BIGINT         NOT NULL REFERENCES cities (id),
    max_price         NUMERIC(10, 2) NOT NULL CHECK (max_price > 0),
    status            VARCHAR(20)    NOT NULL CHECK (status IN ('ACTIVE', 'TRIGGERED', 'CANCELLED')),
    created_at        TIMESTAMPTZ    NOT NULL,
    triggered_at      TIMESTAMPTZ,
    triggered_show_id BIGINT REFERENCES shows (id),
    triggered_price   NUMERIC(10, 2)
);

CREATE INDEX idx_price_alerts_status ON price_alerts (status, id);
CREATE INDEX idx_price_alerts_user ON price_alerts (user_id, status);

-- Friends and group polls --------------------------------------------------------------------------------------------

-- One row per pair of users, whoever asked first. A declined or removed pair is reused for a new request.
CREATE TABLE friendships (
    id           BIGSERIAL PRIMARY KEY,
    requester_id BIGINT      NOT NULL REFERENCES users (id),
    addressee_id BIGINT      NOT NULL REFERENCES users (id),
    status       VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'REMOVED')),
    created_at   TIMESTAMPTZ NOT NULL,
    responded_at TIMESTAMPTZ,
    CONSTRAINT chk_friendships_not_self CHECK (requester_id <> addressee_id)
);

CREATE UNIQUE INDEX uq_friendships_pair
    ON friendships (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));
CREATE INDEX idx_friendships_addressee ON friendships (addressee_id, status);
CREATE INDEX idx_friendships_requester ON friendships (requester_id, status);

CREATE TABLE polls (
    id              BIGSERIAL PRIMARY KEY,
    creator_id      BIGINT       NOT NULL REFERENCES users (id),
    title           VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    winning_show_id BIGINT REFERENCES shows (id),
    created_at      TIMESTAMPTZ  NOT NULL,
    closed_at       TIMESTAMPTZ
);

CREATE TABLE poll_options (
    id      BIGSERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL REFERENCES polls (id),
    show_id BIGINT NOT NULL REFERENCES shows (id),
    CONSTRAINT uq_poll_options UNIQUE (poll_id, show_id)
);

CREATE TABLE poll_members (
    id      BIGSERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL REFERENCES polls (id),
    user_id BIGINT NOT NULL REFERENCES users (id),
    CONSTRAINT uq_poll_members UNIQUE (poll_id, user_id)
);

CREATE INDEX idx_poll_members_user ON poll_members (user_id);

-- One vote per member per poll. Changing a vote updates the row.
CREATE TABLE poll_votes (
    id       BIGSERIAL PRIMARY KEY,
    poll_id  BIGINT      NOT NULL REFERENCES polls (id),
    user_id  BIGINT      NOT NULL REFERENCES users (id),
    show_id  BIGINT      NOT NULL REFERENCES shows (id),
    voted_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_poll_votes UNIQUE (poll_id, user_id)
);

-- Batch 2: expiry, cancel and refund, face-value resale, waitlist.
-- Approved by the user: this migration replaces three constraints. No rows are deleted.

-- 1. New booking states.
ALTER TABLE bookings DROP CONSTRAINT bookings_status_check;
ALTER TABLE bookings ADD CONSTRAINT chk_bookings_status CHECK (status IN (
    'PENDING_PAYMENT', 'CONFIRMED', 'FAILED', 'NEEDS_REFUND', 'EXPIRED', 'CANCELLED', 'REFUNDED', 'RESOLD'));

ALTER TABLE bookings
    ADD COLUMN razorpay_refund_id   VARCHAR(50),
    ADD COLUMN cancelled_at         TIMESTAMPTZ,
    ADD COLUMN refunded_at          TIMESTAMPTZ,
    ADD COLUMN resale_listed_at     TIMESTAMPTZ,
    ADD COLUMN resale_of_booking_id BIGINT REFERENCES bookings (id);

CREATE INDEX idx_bookings_status_created ON bookings (status, created_at);
CREATE INDEX idx_bookings_resale_of ON bookings (resale_of_booking_id);

-- 2. A sold seat can be freed (cancel) and sold again. Old rows stay as history with released_at set.
ALTER TABLE show_booked_seats ADD COLUMN released_at TIMESTAMPTZ;
ALTER TABLE show_booked_seats DROP CONSTRAINT uq_show_booked_seats_show_seat;
CREATE UNIQUE INDEX uq_show_booked_seats_active ON show_booked_seats (show_id, seat_id) WHERE released_at IS NULL;
CREATE INDEX idx_show_booked_seats_booking ON show_booked_seats (booking_id);

-- 3. The same movie may run at the same time on two screens. One show per screen slot instead.
ALTER TABLE shows DROP CONSTRAINT uq_shows_slot;
ALTER TABLE shows ADD CONSTRAINT uq_shows_screen_slot UNIQUE (screen_id, show_date, start_time);

-- 4. Waitlist for full shows. One active entry per user per show.
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

CREATE UNIQUE INDEX uq_waitlist_active ON waitlist_entries (show_id, user_id) WHERE status IN ('WAITING', 'OFFERED');
CREATE INDEX idx_waitlist_show_status ON waitlist_entries (show_id, status, created_at);
CREATE INDEX idx_waitlist_lock ON waitlist_entries (lock_id);

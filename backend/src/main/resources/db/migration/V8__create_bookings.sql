-- Bookings and sold seats.
-- A booking starts as PENDING_PAYMENT when the Razorpay order is made, and becomes CONFIRMED once the payment is
-- verified. show_booked_seats is the final guard: one row per sold seat, unique per show, so a seat can never be
-- sold twice even if a Redis lock expired.

CREATE TABLE bookings (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT         NOT NULL REFERENCES users (id),
    show_id             BIGINT         NOT NULL REFERENCES shows (id),
    status              VARCHAR(20)    NOT NULL
        CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'FAILED', 'NEEDS_REFUND')),
    seat_ids            VARCHAR(250)   NOT NULL,
    amount              NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    currency            VARCHAR(3)     NOT NULL,
    lock_id             VARCHAR(36)    NOT NULL,
    razorpay_order_id   VARCHAR(50),
    razorpay_payment_id VARCHAR(50),
    created_at          TIMESTAMPTZ    NOT NULL,
    confirmed_at        TIMESTAMPTZ,
    CONSTRAINT uq_bookings_lock_id UNIQUE (lock_id),
    CONSTRAINT uq_bookings_razorpay_order_id UNIQUE (razorpay_order_id)
);

CREATE INDEX idx_bookings_user_created ON bookings (user_id, created_at DESC);

CREATE TABLE show_booked_seats (
    id         BIGSERIAL PRIMARY KEY,
    show_id    BIGINT      NOT NULL REFERENCES shows (id),
    seat_id    BIGINT      NOT NULL REFERENCES seats (id),
    booking_id BIGINT      NOT NULL REFERENCES bookings (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_show_booked_seats_show_seat UNIQUE (show_id, seat_id)
);

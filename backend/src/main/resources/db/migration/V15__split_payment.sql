-- Batch 4b: split payment. Friends each pay their share of one booking. Only adds.

ALTER TABLE bookings ADD COLUMN split BOOLEAN NOT NULL DEFAULT FALSE;

-- One share per person. PENDING until paid; VOID if the booking expired before they paid.
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

-- Friends in a split booking. They can see the booking and its ticket. The organizer is the booking's owner.
CREATE TABLE booking_members (
    id         BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings (id),
    user_id    BIGINT NOT NULL REFERENCES users (id),
    CONSTRAINT uq_booking_members UNIQUE (booking_id, user_id)
);

CREATE INDEX idx_booking_members_user ON booking_members (user_id);
CREATE INDEX idx_bookings_split_status ON bookings (split, status, created_at);

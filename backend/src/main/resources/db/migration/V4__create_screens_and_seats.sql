-- Screens in a cinema and the fixed seat layout of each screen.
-- Shows are linked to screens, and prices per category are set, in a later migration.

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
    CONSTRAINT uq_seats_position UNIQUE (screen_id, row_label, seat_number)
);

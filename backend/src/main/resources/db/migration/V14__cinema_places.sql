-- Weekend planner: food and parking near each cinema, entered by the theatre. Only adds.

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

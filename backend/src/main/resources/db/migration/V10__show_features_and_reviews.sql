-- Show tags for filters, wheelchair seats, the movie on each booking, and verified reviews. Only adds.

ALTER TABLE shows
    ADD COLUMN quiet            BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sensory_friendly BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN subtitles        BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE seats ADD COLUMN wheelchair BOOLEAN NOT NULL DEFAULT FALSE;

-- The movie on each booking, so reviews can check who watched what.
ALTER TABLE bookings ADD COLUMN movie_id BIGINT REFERENCES movies (id);

UPDATE bookings b
SET movie_id = s.movie_id
FROM shows s
WHERE s.id = b.show_id;

CREATE INDEX idx_bookings_user_movie ON bookings (user_id, movie_id);

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

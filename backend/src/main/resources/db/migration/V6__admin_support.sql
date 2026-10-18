-- Admin support: which theatre admins manage which cinema, and which screen a show runs on.
-- shows.screen_id stays nullable, because shows created before screens existed have no screen.

CREATE TABLE cinema_admins (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    cinema_id  BIGINT      NOT NULL REFERENCES cinemas (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_cinema_admins_user_cinema UNIQUE (user_id, cinema_id)
);

CREATE INDEX idx_cinema_admins_cinema_id ON cinema_admins (cinema_id);

ALTER TABLE shows ADD COLUMN screen_id BIGINT REFERENCES screens (id);

CREATE INDEX idx_shows_screen_date ON shows (screen_id, show_date);

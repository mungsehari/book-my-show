-- Batch 3: last-minute deals, price alerts, seat view preview. Only adds.

-- Theatres can keep a show out of last-minute deals.
ALTER TABLE shows ADD COLUMN deals_enabled BOOLEAN NOT NULL DEFAULT TRUE;

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

-- A photo of the screen as seen from a block of rows (fromRow to toRow, inclusive). Images are hosted elsewhere.
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

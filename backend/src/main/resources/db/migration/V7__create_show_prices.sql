-- Price per seat category for each show. shows.price keeps the lowest price, shown as "from" in listings.

CREATE TABLE show_prices (
    id       BIGSERIAL PRIMARY KEY,
    show_id  BIGINT         NOT NULL REFERENCES shows (id),
    category VARCHAR(20)    NOT NULL CHECK (category IN ('REGULAR', 'PREMIUM', 'RECLINER')),
    price    NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    CONSTRAINT uq_show_prices_show_category UNIQUE (show_id, category)
);

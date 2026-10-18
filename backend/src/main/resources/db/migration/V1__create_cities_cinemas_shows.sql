-- Core browse tables: cities, cinemas, shows.

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

CREATE TABLE shows (
    id         BIGSERIAL PRIMARY KEY,
    cinema_id  BIGINT         NOT NULL REFERENCES cinemas (id),
    movie_name VARCHAR(200)   NOT NULL,
    show_date  DATE           NOT NULL,
    start_time TIME           NOT NULL,
    price      NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    CONSTRAINT uq_shows_slot UNIQUE (cinema_id, show_date, start_time, movie_name)
);

CREATE INDEX idx_shows_cinema_date ON shows (cinema_id, show_date);

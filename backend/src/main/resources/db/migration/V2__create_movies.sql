-- Movie catalog. Shows still store movie_name. Linking shows to movies comes in a later migration.

CREATE TABLE movies (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(200)  NOT NULL,
    description      VARCHAR(1000) NOT NULL,
    language         VARCHAR(50)   NOT NULL,
    genre            VARCHAR(50)   NOT NULL,
    duration_minutes INTEGER       NOT NULL CHECK (duration_minutes > 0),
    certificate      VARCHAR(5)    NOT NULL CHECK (certificate IN ('U', 'UA', 'A')),
    release_date     DATE          NOT NULL,
    poster_url       VARCHAR(500),
    CONSTRAINT uq_movies_title_language UNIQUE (title, language)
);

CREATE INDEX idx_movies_language ON movies (language);

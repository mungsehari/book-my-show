-- Link shows to the movie catalog.
-- movie_id stays nullable for now. Existing local data may have shows whose movie is not in the catalog yet.
-- A later migration makes it NOT NULL and drops movie_name.

ALTER TABLE shows ADD COLUMN movie_id BIGINT REFERENCES movies (id);

-- Backfill only when the title matches exactly one movie, so a title in two languages is never guessed.
UPDATE shows s
SET movie_id = (SELECT m.id FROM movies m WHERE m.title = s.movie_name)
WHERE s.movie_id IS NULL
  AND (SELECT COUNT(*) FROM movies m WHERE m.title = s.movie_name) = 1;

CREATE INDEX idx_shows_movie_id ON shows (movie_id);

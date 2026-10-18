-- Ticket details on each booking, copied at checkout, so tickets and history read one table and keep what was
-- bought. Also the check-in record for the entry scan. Only adds columns.

ALTER TABLE bookings
    ADD COLUMN show_date     DATE,
    ADD COLUMN start_time    TIME,
    ADD COLUMN movie_name    VARCHAR(200),
    ADD COLUMN cinema_id     BIGINT REFERENCES cinemas (id),
    ADD COLUMN screen_name   VARCHAR(50),
    ADD COLUMN seat_labels   VARCHAR(250),
    ADD COLUMN checked_in_at TIMESTAMPTZ,
    ADD COLUMN checked_in_by BIGINT REFERENCES users (id);

-- Fill in bookings made before this migration.
UPDATE bookings b
SET show_date  = s.show_date,
    start_time = s.start_time,
    movie_name = s.movie_name,
    cinema_id  = s.cinema_id
FROM shows s
WHERE s.id = b.show_id;

UPDATE bookings b
SET screen_name = sc.name
FROM shows s
JOIN screens sc ON sc.id = s.screen_id
WHERE s.id = b.show_id;

UPDATE bookings b
SET seat_labels = (SELECT string_agg(se.row_label || se.seat_number, ',' ORDER BY se.row_label, se.seat_number)
                   FROM seats se
                   WHERE se.id = ANY (string_to_array(b.seat_ids, ',')::BIGINT[]));

ALTER TABLE bookings
    ALTER COLUMN show_date SET NOT NULL,
    ALTER COLUMN start_time SET NOT NULL,
    ALTER COLUMN movie_name SET NOT NULL,
    ALTER COLUMN cinema_id SET NOT NULL,
    ALTER COLUMN screen_name SET NOT NULL,
    ALTER COLUMN seat_labels SET NOT NULL;

CREATE INDEX idx_bookings_user_show_date ON bookings (user_id, show_date);

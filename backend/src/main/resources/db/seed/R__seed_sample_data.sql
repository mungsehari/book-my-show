-- Sample data for local development. Loaded only with the local profile.
-- Safe to run more than once. Names are made up.
-- Show dates are today and tomorrow at the time this file runs. Flyway reruns it when the file changes.

INSERT INTO cities (name, state)
VALUES ('Mumbai', 'Maharashtra'),
       ('Pune', 'Maharashtra'),
       ('Bengaluru', 'Karnataka'),
       ('Delhi', 'Delhi')
ON CONFLICT (name, state) DO NOTHING;

INSERT INTO cinemas (city_id, name, address)
SELECT c.id, v.name, v.address
FROM (VALUES ('Mumbai', 'Maharashtra', 'City Cineplex Andheri', 'Andheri West, Mumbai'),
             ('Mumbai', 'Maharashtra', 'Starlight Cinemas Bandra', 'Linking Road, Bandra, Mumbai'),
             ('Pune', 'Maharashtra', 'Galaxy Screens Kothrud', 'Karve Road, Kothrud, Pune'),
             ('Bengaluru', 'Karnataka', 'Lakeview Multiplex Indiranagar', '100 Feet Road, Indiranagar, Bengaluru'),
             ('Delhi', 'Delhi', 'Capital Cinema Saket', 'Press Enclave Road, Saket, Delhi'))
         AS v (city, state, name, address)
JOIN cities c ON c.name = v.city AND c.state = v.state
ON CONFLICT (city_id, name) DO NOTHING;

-- Two screens per cinema.
INSERT INTO screens (cinema_id, name)
SELECT ci.id, v.name
FROM cinemas ci
CROSS JOIN (VALUES ('Screen 1'), ('Screen 2')) AS v (name)
ON CONFLICT (cinema_id, name) DO NOTHING;

-- Same layout on every screen: rows A-F regular (12 seats), G-I premium (12 seats), J recliner (8 seats).
INSERT INTO seats (screen_id, row_label, seat_number, category)
SELECT sc.id, r.row_label, n.seat_number, r.category
FROM screens sc
CROSS JOIN (VALUES ('A', 'REGULAR', 12), ('B', 'REGULAR', 12), ('C', 'REGULAR', 12),
                   ('D', 'REGULAR', 12), ('E', 'REGULAR', 12), ('F', 'REGULAR', 12),
                   ('G', 'PREMIUM', 12), ('H', 'PREMIUM', 12), ('I', 'PREMIUM', 12),
                   ('J', 'RECLINER', 8))
         AS r (row_label, category, seat_count)
CROSS JOIN LATERAL generate_series(1, r.seat_count) AS n (seat_number)
ON CONFLICT (screen_id, row_label, seat_number) DO NOTHING;

INSERT INTO movies (title, description, language, genre, duration_minutes, certificate, release_date)
VALUES ('The Last Monsoon', 'A fishing family waits out one last storm season.', 'Hindi', 'Drama', 142, 'UA', DATE '2026-09-25'),
       ('Sky Riders', 'Two rival pilots race across the Himalayas.', 'English', 'Action', 128, 'UA', DATE '2026-10-02'),
       ('Code Red Mumbai', 'A cyber cop has one night to stop a city-wide blackout.', 'Hindi', 'Thriller', 135, 'A', DATE '2026-10-09')
ON CONFLICT (title, language) DO NOTHING;

INSERT INTO shows (cinema_id, movie_name, show_date, start_time, price)
SELECT ci.id, v.movie, CURRENT_DATE + v.day_offset, v.start_time::TIME, v.price
FROM (VALUES ('City Cineplex Andheri', 'The Last Monsoon', 0, '10:00', 250.00),
             ('City Cineplex Andheri', 'The Last Monsoon', 0, '18:30', 320.00),
             ('City Cineplex Andheri', 'Sky Riders', 1, '14:00', 280.00),
             ('Starlight Cinemas Bandra', 'Code Red Mumbai', 0, '21:00', 350.00),
             ('Galaxy Screens Kothrud', 'Sky Riders', 0, '12:15', 220.00),
             ('Lakeview Multiplex Indiranagar', 'The Last Monsoon', 0, '19:45', 300.00),
             ('Capital Cinema Saket', 'Code Red Mumbai', 1, '16:30', 270.00))
         AS v (cinema, movie, day_offset, start_time, price)
JOIN cinemas ci ON ci.name = v.cinema
ON CONFLICT (cinema_id, show_date, start_time, movie_name) DO NOTHING;

-- Link sample shows to their movie. Same rule as V3: only when the title matches exactly one movie.
UPDATE shows s
SET movie_id = (SELECT m.id FROM movies m WHERE m.title = s.movie_name)
WHERE s.movie_id IS NULL
  AND (SELECT COUNT(*) FROM movies m WHERE m.title = s.movie_name) = 1;

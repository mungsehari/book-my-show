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

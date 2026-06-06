-- Populate sensible default seat prices per seat type (ONLY for seats with price = 0 or NULL)
-- Adjust values to your business rules before running in production.

-- Regular seats
UPDATE seats
SET price = 150.00
WHERE (price IS NULL OR price = 0) AND seat_type = 'REGULAR';

-- Premium seats
UPDATE seats
SET price = 250.00
WHERE (price IS NULL OR price = 0) AND seat_type = 'PREMIUM';

-- VIP seats
UPDATE seats
SET price = 400.00
WHERE (price IS NULL OR price = 0) AND seat_type = 'VIP';

-- Recliner seats
UPDATE seats
SET price = 700.00
WHERE (price IS NULL OR price = 0) AND seat_type = 'RECLINER';

-- Accessible / others: set to REGULAR price if needed
UPDATE seats
SET price = 150.00
WHERE (price IS NULL OR price = 0) AND seat_type NOT IN ('REGULAR','PREMIUM','VIP','RECLINER');

-- Ensure show_seats prices reflect the physical seat prices where missing
UPDATE show_seats ss
JOIN seats s ON ss.seat_id = s.id
SET ss.price = s.price
WHERE ss.price IS NULL OR ss.price = 0;

-- Optional: log count (MySQL client only)
SELECT 'Seat prices populated' as msg;
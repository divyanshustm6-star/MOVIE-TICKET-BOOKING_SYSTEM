# Movie Booking System Database Schema

The MySQL schema is in `src/main/resources/db/schema.sql`. It is designed for a Java Spring Boot application using JPA Hibernate and MySQL 8+.

## Table Purposes

| Table | Purpose |
| --- | --- |
| `roles` | Stores application roles such as `ROLE_ADMIN` and `ROLE_CUSTOMER`. |
| `users` | Stores authenticated user accounts, password hashes, profile fields, and account state. |
| `user_roles` | Maps users to one or more roles. This keeps authorization flexible for admins and customers. |
| `refresh_tokens` | Stores hashed refresh/session tokens for stateless authentication flows. |
| `movies` | Stores movie metadata such as title, language, duration, release date, certification, media URLs, and lifecycle status. |
| `movie_genres` | Stores one or more genres per movie without duplicating movie rows. |
| `theaters` | Stores cinema/theater locations, address data, contact data, and operating status. |
| `screens` | Stores individual screens/auditoriums inside a theater. |
| `seats` | Stores the physical seat layout for each screen, including row, seat number, type, and seat condition. |
| `movie_shows` | Stores scheduled movie showtimes for a movie on a specific screen. |
| `show_seats` | Stores per-show seat inventory, price, availability, lock owner, and lock expiry. |
| `bookings` | Stores customer bookings, booking reference, totals, booking status, and timestamps. |
| `booking_seats` | Stores the exact seats reserved by a booking. A unique constraint prevents the same show seat from being booked twice. |
| `payments` | Stores payment attempts and successful transactions linked to bookings. |
| `booking_status_history` | Stores booking lifecycle changes for booking history and audit trails. |

## ER Relationship Explanation

`users` and `roles` have a many-to-many relationship through `user_roles`. This supports customer and admin access while allowing a user to hold multiple roles if required.

`theaters` have many `screens`, and each `screen` has many physical `seats`. A `movie` can be scheduled in many `movie_shows`, and each `movie_show` belongs to exactly one `screen`.

`show_seats` is the inventory table for booking. It connects a `movie_show` to the physical `seat` records for that screen and stores the per-show state: available, locked, booked, or blocked.

`users` create `bookings` for a specific `movie_show`. Each `booking` has one or more `booking_seats`. The schema enforces that every booked seat belongs to the same show as the booking.

`payments` belong to `bookings`, allowing one or more payment attempts per booking. `booking_status_history` records changes such as pending to confirmed, confirmed to cancelled, or confirmed to refunded.

## Main Cardinalities

| Relationship | Cardinality |
| --- | --- |
| `users` -> `user_roles` -> `roles` | Many-to-many |
| `users` -> `refresh_tokens` | One-to-many |
| `movies` -> `movie_genres` | One-to-many |
| `theaters` -> `screens` | One-to-many |
| `screens` -> `seats` | One-to-many |
| `movies` -> `movie_shows` | One-to-many |
| `screens` -> `movie_shows` | One-to-many |
| `movie_shows` -> `show_seats` | One-to-many |
| `seats` -> `show_seats` | One-to-many over time |
| `users` -> `bookings` | One-to-many |
| `movie_shows` -> `bookings` | One-to-many |
| `bookings` -> `booking_seats` | One-to-many |
| `show_seats` -> `booking_seats` | One-to-zero-or-one successful booking |
| `bookings` -> `payments` | One-to-many |
| `bookings` -> `booking_status_history` | One-to-many |

## Production Notes

- Passwords and refresh tokens are stored as hashes, not plain text.
- Foreign keys use restrictive deletes for historical records such as bookings and payments.
- Audit columns are included on transactional and master tables.
- Unique constraints protect email, phone, booking reference, payment reference, screen seat positions, and duplicate booked show seats.
- Indexes are included for common lookup paths: city/status theater search, movie date/status search, showtime search, booking history, payment status, and seat availability.
- Seat locking is modeled with `locked_by_user_id` and `locked_until`, which supports short-lived reservation windows before payment confirmation.

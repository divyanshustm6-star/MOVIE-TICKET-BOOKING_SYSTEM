-- Movie Booking System - MySQL schema for Spring Boot, JPA Hibernate, and MySQL 8+
-- Recommended engine/charset for production tables.
-- CREATE DATABASE moviebooking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE moviebooking;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS booking_status_history;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS booking_seats;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS show_seats;
DROP TABLE IF EXISTS movie_shows;
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS screens;
DROP TABLE IF EXISTS theaters;
DROP TABLE IF EXISTS movie_genres;
DROP TABLE IF EXISTS movies;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT chk_roles_name CHECK (name IN ('ROLE_ADMIN', 'ROLE_USER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL,
    phone VARCHAR(25),
    password_hash VARCHAR(255) NOT NULL,
    account_status ENUM('ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone UNIQUE (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    INDEX idx_refresh_tokens_user_id (user_id),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE movies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    language VARCHAR(80) NOT NULL,
    duration_minutes SMALLINT UNSIGNED NOT NULL,
    release_date DATE,
    certification VARCHAR(20),
    poster_url VARCHAR(500),
    trailer_url VARCHAR(500),
    status ENUM('UPCOMING', 'NOW_SHOWING', 'ARCHIVED') NOT NULL DEFAULT 'UPCOMING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_movies_status_release_date (status, release_date),
    INDEX idx_movies_title (title),
    CONSTRAINT chk_movies_duration CHECK (duration_minutes > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE movie_genres (
    movie_id BIGINT NOT NULL,
    genre VARCHAR(80) NOT NULL,
    PRIMARY KEY (movie_id, genre),
    CONSTRAINT fk_movie_genres_movie
        FOREIGN KEY (movie_id) REFERENCES movies (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE theaters (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(180) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(80) NOT NULL DEFAULT 'India',
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    contact_phone VARCHAR(25),
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_theaters_city_status (city, status),
    CONSTRAINT uk_theaters_name_location UNIQUE (name, address_line1, city, postal_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE screens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    theater_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    screen_type ENUM('STANDARD', 'IMAX', 'DOLBY', 'THREE_D', 'FOUR_DX') NOT NULL DEFAULT 'STANDARD',
    total_seats SMALLINT UNSIGNED NOT NULL,
    status ENUM('ACTIVE', 'MAINTENANCE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_screens_theater_name UNIQUE (theater_id, name),
    INDEX idx_screens_theater_id (theater_id),
    CONSTRAINT fk_screens_theater
        FOREIGN KEY (theater_id) REFERENCES theaters (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_screens_total_seats CHECK (total_seats > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE seats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    screen_id BIGINT NOT NULL,
    row_label VARCHAR(10) NOT NULL,
    seat_number SMALLINT UNSIGNED NOT NULL,
    seat_type ENUM('REGULAR', 'PREMIUM', 'RECLINER', 'ACCESSIBLE') NOT NULL DEFAULT 'REGULAR',
    status ENUM('ACTIVE', 'BROKEN', 'BLOCKED') NOT NULL DEFAULT 'ACTIVE',
price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
CONSTRAINT uk_seats_screen_row_number UNIQUE (screen_id, row_label, seat_number),
INDEX idx_seats_screen_id (screen_id),
CONSTRAINT fk_seats_screen
    FOREIGN KEY (screen_id) REFERENCES screens (id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE movie_shows (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
movie_id BIGINT NOT NULL,
screen_id BIGINT NOT NULL,
show_date DATE NOT NULL,
starts_at DATETIME NOT NULL,
ends_at DATETIME NOT NULL,
status ENUM('SCHEDULED', 'BOOKING_OPEN', 'SOLD_OUT', 'CANCELLED', 'COMPLETED') NOT NULL DEFAULT 'SCHEDULED',
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
INDEX idx_movie_shows_movie_id (movie_id),
INDEX idx_movie_shows_screen_time (screen_id, starts_at, ends_at),
INDEX idx_movie_shows_date_status (show_date, status),
CONSTRAINT fk_movie_shows_movie
    FOREIGN KEY (movie_id) REFERENCES movies (id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
CONSTRAINT fk_movie_shows_screen
    FOREIGN KEY (screen_id) REFERENCES screens (id)
    ON DELETE CASCADE ON UPDATE CASCADE,
CONSTRAINT chk_movie_shows_time CHECK (ends_at > starts_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE show_seats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    show_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    seat_status ENUM('AVAILABLE', 'LOCKED', 'BOOKED', 'BLOCKED') NOT NULL DEFAULT 'AVAILABLE',
    locked_by_user_id BIGINT NULL,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_show_seats_show_seat UNIQUE (show_id, seat_id),
    CONSTRAINT uk_show_seats_id_show UNIQUE (id, show_id),
    INDEX idx_show_seats_show_status (show_id, seat_status),
    INDEX idx_show_seats_locked_until (locked_until),
    CONSTRAINT fk_show_seats_show
        FOREIGN KEY (show_id) REFERENCES movie_shows (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_show_seats_seat
        FOREIGN KEY (seat_id) REFERENCES seats (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_show_seats_locked_by_user
        FOREIGN KEY (locked_by_user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_show_seats_price CHECK (price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_reference VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    show_id BIGINT NOT NULL,
    booking_status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    seats_count SMALLINT UNSIGNED NOT NULL,
    subtotal_amount DECIMAL(10, 2) NOT NULL,
    tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL,
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_bookings_reference UNIQUE (booking_reference),
    CONSTRAINT uk_bookings_id_show UNIQUE (id, show_id),
    INDEX idx_bookings_user_booked_at (user_id, booked_at),
    INDEX idx_bookings_show_status (show_id, booking_status),
    CONSTRAINT fk_bookings_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_bookings_show
        FOREIGN KEY (show_id) REFERENCES movie_shows (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_bookings_seats_count CHECK (seats_count > 0),
    CONSTRAINT chk_bookings_amounts CHECK (
        subtotal_amount >= 0
        AND tax_amount >= 0
        AND discount_amount >= 0
        AND total_amount >= 0
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE booking_seats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    show_id BIGINT NOT NULL,
    show_seat_id BIGINT NOT NULL,
    seat_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_booking_seats_show_seat UNIQUE (show_seat_id),
    CONSTRAINT uk_booking_seats_booking_show_seat UNIQUE (booking_id, show_seat_id),
    INDEX idx_booking_seats_booking_id (booking_id),
    CONSTRAINT fk_booking_seats_booking_show
        FOREIGN KEY (booking_id, show_id) REFERENCES bookings (id, show_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_booking_seats_show_seat_show
        FOREIGN KEY (show_seat_id, show_id) REFERENCES show_seats (id, show_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_booking_seats_price CHECK (seat_price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    payment_reference VARCHAR(80) NOT NULL,
    provider VARCHAR(80) NOT NULL,
    provider_transaction_id VARCHAR(120),
    payment_method ENUM('CARD', 'UPI', 'NET_BANKING', 'WALLET', 'CASH') NOT NULL,
    payment_status ENUM('INITIATED', 'SUCCESS', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED') NOT NULL DEFAULT 'INITIATED',
    amount DECIMAL(10, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    paid_at TIMESTAMP NULL,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_payments_reference UNIQUE (payment_reference),
    CONSTRAINT uk_payments_provider_transaction UNIQUE (provider, provider_transaction_id),
    INDEX idx_payments_booking_id (booking_id),
    INDEX idx_payments_status_created_at (payment_status, created_at),
    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_payments_amount CHECK (amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE booking_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    old_status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'REFUNDED') NULL,
    new_status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'REFUNDED') NOT NULL,
    changed_by_user_id BIGINT NULL,
    reason VARCHAR(500),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_booking_status_history_booking_id (booking_id),
    INDEX idx_booking_status_history_changed_at (changed_at),
    CONSTRAINT fk_booking_status_history_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_booking_status_history_changed_by_user
        FOREIGN KEY (changed_by_user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN', 'Can manage movies, theaters, screens, shows, bookings, and reports'),
('ROLE_USER', 'Can browse shows, book seats, pay, cancel eligible bookings, and view booking history');

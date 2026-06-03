package com.movie.moviebooking.repository;

import com.movie.moviebooking.entity.Booking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByUserEmailOrderByBookedAtDesc(String email);

    List<Booking> findByShowId(Long showId);
}

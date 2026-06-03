package com.movie.moviebooking.repository;

import com.movie.moviebooking.entity.BookingSeat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    List<BookingSeat> findByBookingId(Long bookingId);

    List<BookingSeat> findByShowSeatId(Long showSeatId);
}

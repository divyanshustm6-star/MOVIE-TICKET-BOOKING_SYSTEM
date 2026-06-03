package com.movie.moviebooking.repository;

import com.movie.moviebooking.entity.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScreenId(Long screenId);

    List<Seat> findByScreenIdOrderByRowLabelAscSeatNumberAsc(Long screenId);

    long countByScreenId(Long screenId);

    boolean existsByScreenIdAndRowLabelAndSeatNumber(Long screenId, String rowLabel, Integer seatNumber);
}

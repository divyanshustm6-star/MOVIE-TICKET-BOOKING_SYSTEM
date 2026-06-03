package com.movie.moviebooking.repository;

import com.movie.moviebooking.entity.Theater;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheaterRepository extends JpaRepository<Theater, Long> {
    List<Theater> findByCityIgnoreCase(String city);
}

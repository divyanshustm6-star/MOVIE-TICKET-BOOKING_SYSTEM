package com.movie.moviebooking.repository;

import com.movie.moviebooking.entity.Movie;
import com.movie.moviebooking.entity.MovieStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByStatus(MovieStatus status);
}

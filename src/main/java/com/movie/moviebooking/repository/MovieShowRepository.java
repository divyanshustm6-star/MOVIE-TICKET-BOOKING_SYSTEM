package com.movie.moviebooking.repository;

import com.movie.moviebooking.entity.MovieShow;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieShowRepository extends JpaRepository<MovieShow, Long> {
    @Query("select s from MovieShow s join fetch s.movie m join fetch s.screen sc join fetch sc.theater t where m.id = :movieId")
    List<MovieShow> findByMovieIdFetch(@Param("movieId") Long movieId);

    @Query("select s from MovieShow s join fetch s.movie m join fetch s.screen sc join fetch sc.theater t where sc.id = :screenId")
    List<MovieShow> findByScreenIdFetch(@Param("screenId") Long screenId);

    // keep derived query for other services that expect it
    List<MovieShow> findByScreenId(Long screenId);

    @Query("select s from MovieShow s join fetch s.movie m join fetch s.screen sc join fetch sc.theater t where s.showDate = :showDate")
    List<MovieShow> findByShowDateFetch(@Param("showDate") LocalDate showDate);

    @Query("select s from MovieShow s join fetch s.movie m join fetch s.screen sc join fetch sc.theater t")
    List<MovieShow> findAllFetch();

    @Query("select s from MovieShow s join fetch s.movie m join fetch s.screen sc join fetch sc.theater t where s.id = :id")
    MovieShow findByIdFetch(@Param("id") Long id);
}

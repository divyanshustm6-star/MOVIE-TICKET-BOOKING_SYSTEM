package com.movie.moviebooking.repository;

import com.movie.moviebooking.entity.ShowSeat;
import com.movie.moviebooking.entity.ShowSeatStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {
    List<ShowSeat> findByShowId(Long showId);

    @Query("select ss from ShowSeat ss join fetch ss.seat seat join fetch ss.show sh join fetch sh.movie m join fetch sh.screen sc join fetch sc.theater t where sh.id = :showId")
    List<ShowSeat> findByShowIdFetch(@Param("showId") Long showId);

    List<ShowSeat> findBySeatId(Long seatId);

    List<ShowSeat> findByShowIdAndSeatStatus(Long showId, ShowSeatStatus status);

    boolean existsByShowIdAndSeatId(Long showId, Long seatId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowSeat ss where ss.id = :id and ss.show.id = :showId")
    Optional<ShowSeat> findByIdAndShowIdForUpdate(@Param("id") Long id, @Param("showId") Long showId);
}

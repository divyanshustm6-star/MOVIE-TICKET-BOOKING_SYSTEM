package com.movie.moviebooking.repository;

import com.movie.moviebooking.entity.Booking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByShowId(Long showId);

    @Query("""
            select distinct b
            from Booking b
            left join fetch b.user
            left join fetch b.show s
            left join fetch s.movie
            left join fetch s.screen sc
            left join fetch sc.theater
            where b.id = :id
            """)
    Optional<Booking> findByIdWithRelations(@Param("id") Long id);

    @Query("""
            select distinct b
            from Booking b
            left join fetch b.user
            left join fetch b.show s
            left join fetch s.movie
            left join fetch s.screen sc
            left join fetch sc.theater
            """)
    List<Booking> findAllWithRelations();

    @Query("""
            select distinct b
            from Booking b
            left join fetch b.user
            left join fetch b.show s
            left join fetch s.movie
            left join fetch s.screen sc
            left join fetch sc.theater
            where b.user.email = :email
            order by b.bookedAt desc
            """)
    List<Booking> findHistoryWithRelations(@Param("email") String email);
}

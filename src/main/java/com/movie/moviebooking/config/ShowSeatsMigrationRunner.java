package com.movie.moviebooking.config;

import com.movie.moviebooking.entity.MovieShow;
import com.movie.moviebooking.entity.Seat;
import com.movie.moviebooking.entity.ShowSeat;
import com.movie.moviebooking.entity.ShowSeatStatus;
import com.movie.moviebooking.repository.MovieShowRepository;
import com.movie.moviebooking.repository.SeatRepository;
import com.movie.moviebooking.repository.ShowSeatRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(200)
public class ShowSeatsMigrationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ShowSeatsMigrationRunner.class);

    private final MovieShowRepository movieShowRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;

    public ShowSeatsMigrationRunner(
            MovieShowRepository movieShowRepository,
            SeatRepository seatRepository,
            ShowSeatRepository showSeatRepository) {
        this.movieShowRepository = movieShowRepository;
        this.seatRepository = seatRepository;
        this.showSeatRepository = showSeatRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        List<MovieShow> shows = movieShowRepository.findAll();
        int totalCreated = 0;
        for (MovieShow show : shows) {
            Long showId = show.getId();
            Long screenId = show.getScreen().getId();
            List<Seat> seats = seatRepository.findByScreenId(screenId);
            List<ShowSeat> toCreate = new ArrayList<>();
            for (Seat seat : seats) {
                if (!showSeatRepository.existsByShowIdAndSeatId(showId, seat.getId())) {
                    ShowSeat ss = new ShowSeat();
                    ss.setShow(show);
                    ss.setSeat(seat);
                    // migrate price from seat if available; otherwise fall back to 0
                    ss.setPrice(seat.getPrice() == null ? BigDecimal.ZERO : seat.getPrice());
                    ss.setSeatStatus(ShowSeatStatus.AVAILABLE);
                    toCreate.add(ss);
                }
            }
            if (!toCreate.isEmpty()) {
                showSeatRepository.saveAll(toCreate);
                totalCreated += toCreate.size();
                log.info("ShowSeatsMigration: created {} seats for show {}", toCreate.size(), showId);
            }
        }
        if (totalCreated > 0) {
            log.info("ShowSeatsMigration: total show seats created: {}", totalCreated);
        } else {
            log.info("ShowSeatsMigration: no missing show seats found");
        }
    }
}

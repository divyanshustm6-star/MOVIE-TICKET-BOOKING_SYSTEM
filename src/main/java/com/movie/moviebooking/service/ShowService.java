package com.movie.moviebooking.service;

import com.movie.moviebooking.dto.ApiDtos.ShowRequest;
import com.movie.moviebooking.dto.ApiDtos.ShowResponse;
import com.movie.moviebooking.dto.ApiDtos.ShowSeatResponse;
import com.movie.moviebooking.entity.MovieShow;
import com.movie.moviebooking.entity.Screen;
import com.movie.moviebooking.entity.SeatStatus;
import com.movie.moviebooking.entity.ShowSeat;
import com.movie.moviebooking.exception.BadRequestException;
import com.movie.moviebooking.exception.ResourceNotFoundException;
import com.movie.moviebooking.repository.BookingRepository;
import com.movie.moviebooking.repository.BookingSeatRepository;
import com.movie.moviebooking.repository.MovieShowRepository;
import com.movie.moviebooking.repository.PaymentRepository;
import com.movie.moviebooking.repository.SeatRepository;
import com.movie.moviebooking.repository.ShowSeatRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShowService {
    private static final Logger log = LoggerFactory.getLogger(ShowService.class);
    private final MovieShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;
    private final MovieService movieService;
    private final ScreenService screenService;

    public ShowService(
            MovieShowRepository showRepository,
            ShowSeatRepository showSeatRepository,
            SeatRepository seatRepository,
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            MovieService movieService,
            ScreenService screenService) {
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.paymentRepository = paymentRepository;
        this.movieService = movieService;
        this.screenService = screenService;
    }

    public List<ShowResponse> findAll(Long movieId, LocalDate date) {
        List<MovieShow> shows;
        if (movieId != null) {
            shows = showRepository.findByMovieIdFetch(movieId);
        } else if (date != null) {
            shows = showRepository.findByShowDateFetch(date);
        } else {
            shows = showRepository.findAllFetch();
        }
        return shows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ShowResponse findById(Long id) {
        return toResponse(getShow(id));
    }

    @Transactional
    public ShowResponse create(ShowRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("Show end time must be after start time");
        }
        // conflict detection: overlapping shows on same screen
        Screen screen = screenService.getScreen(request.screenId());
        log.debug("Checking conflicts for screenId={}, showDate={}, startsAt={}, endsAt={}", screen.getId(), request.showDate(), request.startsAt(), request.endsAt());
        java.util.List<MovieShow> existing = showRepository.findByScreenId(screen.getId());
        for (MovieShow es : existing) {
            if (es.getStartsAt() == null || es.getEndsAt() == null) continue;
            log.debug("Comparing with existing show id={} startsAt={} endsAt={}", es.getId(), es.getStartsAt(), es.getEndsAt());
            if (es.getStartsAt().isBefore(request.endsAt()) && es.getEndsAt().isAfter(request.startsAt())) {
                throw new BadRequestException(String.format("Screen already has a show (id=%d, movie=%s) from %s to %s",
                        es.getId(), es.getMovie() != null ? es.getMovie().getTitle() : "-", es.getStartsAt().toString(), es.getEndsAt().toString()));
            }
        }

        MovieShow show = new MovieShow();
        show.setMovie(movieService.getMovie(request.movieId()));
        show.setScreen(screen);
        show.setShowDate(request.showDate());
        show.setStartsAt(request.startsAt());
        show.setEndsAt(request.endsAt());
        if (request.status() != null) {
            show.setStatus(request.status());
        }
        MovieShow saved = showRepository.save(show);
        createShowSeats(saved);
        return toResponse(saved);
    }

    @Transactional
    public ShowResponse update(Long id, ShowRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("Show end time must be after start time");
        }
        MovieShow show = getShow(id);
        // conflict detection for update: ignore the same show id
        java.util.List<MovieShow> existing = showRepository.findByScreenId(show.getScreen().getId());
        log.debug("Checking conflicts for update showId={} screenId={} showDate={} startsAt={} endsAt={}", id, show.getScreen().getId(), request.showDate(), request.startsAt(), request.endsAt());
        for (MovieShow es : existing) {
            if (es.getId().equals(id)) continue;
            if (es.getStartsAt() == null || es.getEndsAt() == null) continue;
            log.debug("Comparing with existing show id={} startsAt={} endsAt={}", es.getId(), es.getStartsAt(), es.getEndsAt());
            if (es.getStartsAt().isBefore(request.endsAt()) && es.getEndsAt().isAfter(request.startsAt())) {
                throw new BadRequestException(String.format("Screen already has a show (id=%d, movie=%s) from %s to %s",
                        es.getId(), es.getMovie() != null ? es.getMovie().getTitle() : "-", es.getStartsAt().toString(), es.getEndsAt().toString()));
            }
        }
        show.setMovie(movieService.getMovie(request.movieId()));
        show.setScreen(screenService.getScreen(request.screenId()));
        show.setShowDate(request.showDate());
        show.setStartsAt(request.startsAt());
        show.setEndsAt(request.endsAt());
        if (request.status() != null) {
            show.setStatus(request.status());
        }
        show.setUpdatedAt(Instant.now());
        return toResponse(showRepository.save(show));
    }

    @Transactional
    public void delete(Long id) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ShowService.class);
        log.info("Deleting Show ID={}", id);
        MovieShow show = getShow(id);
        var bookings = bookingRepository.findByShowId(show.getId());
        log.info("Deleting Bookings Count={} for Show ID={}", bookings.size(), show.getId());
        for (var booking : bookings) {
            paymentRepository.deleteAll(paymentRepository.findByBookingId(booking.getId()));
            bookingSeatRepository.deleteAll(bookingSeatRepository.findByBookingId(booking.getId()));
            bookingRepository.delete(booking);
        }
        var ss = showSeatRepository.findByShowId(show.getId());
        log.info("Deleting ShowSeats Count={} for Show ID={}", ss.size(), show.getId());
        showSeatRepository.deleteAll(ss);
        showRepository.delete(show);
        boolean exists = showRepository.existsById(id);
        log.info("Show ID={} exists after delete? {}", id, exists);
    }

    @Transactional(readOnly = true)
    public List<ShowSeatResponse> seats(Long showId) {
        return showSeatRepository.findByShowIdFetch(showId).stream().map(this::toSeatResponse).toList();
    }

    @Transactional(readOnly = true)
    public MovieShow getShow(Long id) {
        MovieShow show = showRepository.findByIdFetch(id);
        if (show == null) {
            throw new ResourceNotFoundException("Show not found");
        }
        return show;
    }

    @org.springframework.beans.factory.annotation.Value("${app.enforce-seat-prices:false}")
    private boolean enforceSeatPrices;

    private void createShowSeats(MovieShow show) {
        List<ShowSeat> showSeats = seatRepository.findByScreenId(show.getScreen().getId()).stream()
                .filter(seat -> seat.getStatus() == SeatStatus.ACTIVE)
                .map(seat -> {
                    ShowSeat showSeat = new ShowSeat();
                    showSeat.setShow(show);
                    showSeat.setSeat(seat);
                    // use price from seat record (seat-based pricing)
                    if (seat.getPrice() == null) {
                        log.warn("ShowService.createShowSeats: seat id={} has null price; setting showSeat.price=0. Enable app.enforce-seat-prices=true to fail.", seat.getId());
                        if (enforceSeatPrices) {
                            throw new com.movie.moviebooking.exception.BadRequestException("Missing price for seat id=" + seat.getId());
                        }
                        showSeat.setPrice(java.math.BigDecimal.ZERO);
                    } else {
                        showSeat.setPrice(seat.getPrice());
                    }
                    return showSeat;
                })
                .toList();
        showSeatRepository.saveAll(showSeats);
    }

    public ShowResponse toResponse(MovieShow show) {
        int available = showSeatRepository.findByShowIdAndSeatStatus(show.getId(), com.movie.moviebooking.entity.ShowSeatStatus.AVAILABLE).size();
        // compute minimum price among show seats as show price preview
        java.util.List<com.movie.moviebooking.entity.ShowSeat> seats = showSeatRepository.findByShowIdFetch(show.getId());
        java.math.BigDecimal price = null;
        for (com.movie.moviebooking.entity.ShowSeat ss : seats) {
            if (ss.getPrice() != null) {
                if (price == null || ss.getPrice().compareTo(price) < 0) price = ss.getPrice();
            }
        }
        return new ShowResponse(
                show.getId(),
                show.getMovie().getId(),
                show.getMovie().getTitle(),
                show.getScreen().getTheater().getName(),
                show.getScreen().getName(),
                show.getShowDate(),
                show.getStartsAt(),
                price,
                available);
    }

    public ShowSeatResponse toSeatResponse(ShowSeat showSeat) {
        return new ShowSeatResponse(
                showSeat.getId(),
                showSeat.getShow().getId(),
                showSeat.getSeat().getId(),
                showSeat.getSeat().getRowLabel(),
                showSeat.getSeat().getSeatNumber(),
                showSeat.getSeat().getSeatType(),
                showSeat.getPrice(),
                showSeat.getSeatStatus());
    }
}

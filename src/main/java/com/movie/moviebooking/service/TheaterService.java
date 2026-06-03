package com.movie.moviebooking.service;

import com.movie.moviebooking.dto.ApiDtos.TheaterRequest;
import com.movie.moviebooking.dto.ApiDtos.TheaterResponse;
import com.movie.moviebooking.entity.Theater;
import com.movie.moviebooking.exception.BadRequestException;
import com.movie.moviebooking.exception.ResourceNotFoundException;
import com.movie.moviebooking.repository.BookingRepository;
import com.movie.moviebooking.repository.BookingSeatRepository;
import com.movie.moviebooking.repository.MovieShowRepository;
import com.movie.moviebooking.repository.PaymentRepository;
import com.movie.moviebooking.repository.SeatRepository;
import com.movie.moviebooking.repository.ScreenRepository;
import com.movie.moviebooking.repository.ShowSeatRepository;
import com.movie.moviebooking.repository.TheaterRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TheaterService {
    private static final Logger log = LoggerFactory.getLogger(TheaterService.class);

    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final MovieShowRepository showRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatRepository seatRepository;

    public TheaterService(
            TheaterRepository theaterRepository,
            ScreenRepository screenRepository,
            MovieShowRepository showRepository,
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            ShowSeatRepository showSeatRepository,
            SeatRepository seatRepository) {
        this.theaterRepository = theaterRepository;
        this.screenRepository = screenRepository;
        this.showRepository = showRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.paymentRepository = paymentRepository;
        this.showSeatRepository = showSeatRepository;
        this.seatRepository = seatRepository;
    }

    public List<TheaterResponse> findAll(String city) {
        List<Theater> theaters = city == null || city.isBlank()
                ? theaterRepository.findAll()
                : theaterRepository.findByCityIgnoreCase(city);
        return theaters.stream().map(this::toResponse).toList();
    }

    public TheaterResponse findById(Long id) {
        return toResponse(getTheater(id));
    }

    @Transactional
    public TheaterResponse create(TheaterRequest request) {
        return toResponse(theaterRepository.save(apply(new Theater(), request)));
    }

    @Transactional
    public TheaterResponse update(Long id, TheaterRequest request) {
        Theater theater = getTheater(id);
        theater.setUpdatedAt(Instant.now());
        return toResponse(theaterRepository.save(apply(theater, request)));
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting Theater ID={}", id);
        Theater theater = getTheater(id);
        List<com.movie.moviebooking.entity.Screen> screens = screenRepository.findByTheaterId(theater.getId());

        // check for active bookings
        long activeBookings = 0;
        for (var screen : screens) {
            List<com.movie.moviebooking.entity.MovieShow> shows = showRepository.findByScreenId(screen.getId());
            for (var show : shows) {
                var bookings = bookingRepository.findByShowId(show.getId());
                for (var b : bookings) {
                    if (b.getBookingStatus() == com.movie.moviebooking.entity.BookingStatus.PENDING || b.getBookingStatus() == com.movie.moviebooking.entity.BookingStatus.CONFIRMED) {
                        activeBookings++;
                    }
                }
            }
        }
        if (activeBookings > 0) {
            throw new BadRequestException("Cannot delete theater: " + activeBookings + " active bookings exist");
        }

        // delete related data
        for (var screen : screens) {
            log.info("Deleting Screen ID={} for Theater ID={}", screen.getId(), id);
            List<com.movie.moviebooking.entity.MovieShow> shows = showRepository.findByScreenId(screen.getId());
            for (var show : shows) {
                log.info("Deleting Show ID={} for Screen ID={}", show.getId(), screen.getId());
                // delete bookings and related payments/booking seats
                var bookings = bookingRepository.findByShowId(show.getId());
                for (var booking : bookings) {
                    log.info("Deleting Booking ID={} for Show ID={}", booking.getId(), show.getId());
                    paymentRepository.deleteAll(paymentRepository.findByBookingId(booking.getId()));
                    bookingSeatRepository.deleteAll(bookingSeatRepository.findByBookingId(booking.getId()));
                    bookingRepository.delete(booking);
                }
                // delete show seats
                var ss = showSeatRepository.findByShowId(show.getId());
                log.info("Deleting ShowSeats Count={} for Show ID={}", ss.size(), show.getId());
                showSeatRepository.deleteAll(ss);
                // delete show
                showRepository.delete(show);
            }
            // delete seats and screen
            var seats = seatRepository.findByScreenId(screen.getId());
            log.info("Deleting Seats Count={} for Screen ID={}", seats.size(), screen.getId());
            seatRepository.deleteAll(seats);
            screenRepository.delete(screen);
        }

        theaterRepository.delete(theater);
        boolean exists = theaterRepository.existsById(id);
        log.info("Theater ID={} exists after delete? {}", id, exists);
    }

    public Theater getTheater(Long id) {
        return theaterRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Theater not found"));
    }

    private Theater apply(Theater theater, TheaterRequest request) {
        theater.setName(request.name());
        theater.setAddressLine1(request.addressLine1());
        theater.setAddressLine2(request.addressLine2());
        theater.setCity(request.city());
        theater.setState(request.state());
        theater.setPostalCode(request.postalCode());
        theater.setCountry(request.country() == null || request.country().isBlank() ? "India" : request.country());
        theater.setLatitude(request.latitude());
        theater.setLongitude(request.longitude());
        theater.setContactPhone(request.contactPhone());
        if (request.status() != null) {
            theater.setStatus(request.status());
        }
        return theater;
    }

    public TheaterResponse toResponse(Theater theater) {
        return new TheaterResponse(
                theater.getId(),
                theater.getName(),
                theater.getAddressLine1(),
                theater.getAddressLine2(),
                theater.getCity(),
                theater.getState(),
                theater.getPostalCode(),
                theater.getCountry(),
                theater.getLatitude(),
                theater.getLongitude(),
                theater.getContactPhone(),
                theater.getStatus());
    }
}

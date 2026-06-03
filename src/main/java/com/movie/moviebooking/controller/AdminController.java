package com.movie.moviebooking.controller;

import com.movie.moviebooking.repository.BookingRepository;
import com.movie.moviebooking.repository.BookingSeatRepository;
import com.movie.moviebooking.repository.MovieShowRepository;
import com.movie.moviebooking.repository.PaymentRepository;
import com.movie.moviebooking.repository.SeatRepository;
import com.movie.moviebooking.repository.ScreenRepository;
import com.movie.moviebooking.repository.ShowSeatRepository;
import com.movie.moviebooking.repository.TheaterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final MovieShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;

    public AdminController(TheaterRepository theaterRepository,
                           ScreenRepository screenRepository,
                           SeatRepository seatRepository,
                           MovieShowRepository showRepository,
                           ShowSeatRepository showSeatRepository,
                           BookingRepository bookingRepository,
                           BookingSeatRepository bookingSeatRepository,
                           PaymentRepository paymentRepository) {
        this.theaterRepository = theaterRepository;
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.paymentRepository = paymentRepository;
    }

    @DeleteMapping("/reset-theaters")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void resetTheaters() {
        log.warn("Admin reset: deleting all theaters, screens, seats, shows and related data");
        // remove payments, booking seats, bookings
        paymentRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
        // remove show seats and shows
        showSeatRepository.deleteAll();
        showRepository.deleteAll();
        // remove seats and screens and theaters
        seatRepository.deleteAll();
        screenRepository.deleteAll();
        theaterRepository.deleteAll();
        log.warn("Admin reset complete");
    }
}

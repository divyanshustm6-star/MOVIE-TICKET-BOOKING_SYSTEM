package com.movie.moviebooking.service;

import com.movie.moviebooking.dto.ApiDtos.BookingRequest;
import com.movie.moviebooking.dto.ApiDtos.BookingResponse;
import com.movie.moviebooking.dto.ApiDtos.BookingUpdateRequest;
import com.movie.moviebooking.dto.ApiDtos.ShowSeatResponse;
import com.movie.moviebooking.entity.Booking;
import com.movie.moviebooking.entity.BookingSeat;
import com.movie.moviebooking.entity.BookingStatus;
import com.movie.moviebooking.entity.MovieShow;
import com.movie.moviebooking.entity.ShowSeat;
import com.movie.moviebooking.entity.ShowSeatStatus;
import com.movie.moviebooking.entity.User;
import com.movie.moviebooking.exception.BadRequestException;
import com.movie.moviebooking.exception.ResourceNotFoundException;
import com.movie.moviebooking.repository.BookingRepository;
import com.movie.moviebooking.repository.BookingSeatRepository;
import com.movie.moviebooking.repository.PaymentRepository;
import com.movie.moviebooking.repository.ShowSeatRepository;
import com.movie.moviebooking.repository.UserRepository;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final AuthService authService;
    private final ShowService showService;

    public BookingService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            ShowSeatRepository showSeatRepository,
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            AuthService authService,
            ShowService showService) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.showSeatRepository = showSeatRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.authService = authService;
        this.showService = showService;
    }

    @Transactional
    public BookingResponse book(BookingRequest request, Principal principal) {
        User user = resolveBookingUser(request, principal);
        MovieShow show = showService.getShow(request.showId());
        List<ShowSeat> selectedSeats = request.showSeatIds().stream()
                .map(id -> showSeatRepository.findByIdAndShowIdForUpdate(id, request.showId())
                        .orElseThrow(() -> new ResourceNotFoundException("Show seat not found: " + id)))
                .toList();

        if (selectedSeats.stream().anyMatch(seat -> seat.getSeatStatus() != ShowSeatStatus.AVAILABLE)) {
            throw new BadRequestException("One or more selected seats are not available");
        }

        BigDecimal subtotal = selectedSeats.stream()
                .map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.18"));
        BigDecimal total = subtotal.add(tax);

        Booking booking = new Booking();
        booking.setBookingReference("BK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        booking.setUser(user);
        booking.setShow(show);
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setSeatsCount(selectedSeats.size());
        booking.setSubtotalAmount(subtotal);
        booking.setTaxAmount(tax);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTotalAmount(total);
        Booking savedBooking = bookingRepository.save(booking);

        selectedSeats.forEach(showSeat -> {
            showSeat.setSeatStatus(ShowSeatStatus.LOCKED);
            showSeat.setLockedByUser(user);
            BookingSeat bookingSeat = new BookingSeat();
            bookingSeat.setBooking(savedBooking);
            bookingSeat.setShow(show);
            bookingSeat.setShowSeat(showSeat);
            bookingSeat.setSeatPrice(showSeat.getPrice());
            bookingSeatRepository.save(bookingSeat);
        });

        return toResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> history(Principal principal) {
        if (principal == null) {
            return findAll();
        }
        return bookingRepository.findByUserEmailOrderByBookedAtDesc(principal.getName()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findAll() {
        return bookingRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id) {
        return toResponse(getBooking(id));
    }

    @Transactional
    public BookingResponse update(Long id, BookingUpdateRequest request) {
        Booking booking = getBooking(id);
        booking.setBookingStatus(request.bookingStatus());
        booking.setUpdatedAt(Instant.now());
        if (request.bookingStatus() == BookingStatus.CANCELLED) {
            booking.setCancelledAt(Instant.now());
            bookingSeatRepository.findByBookingId(booking.getId()).forEach(bookingSeat -> {
                bookingSeat.getShowSeat().setSeatStatus(ShowSeatStatus.AVAILABLE);
                bookingSeat.getShowSeat().setLockedByUser(null);
                bookingSeat.getShowSeat().setLockedUntil(null);
            });
        }
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public void delete(Long id) {
        Booking booking = getBooking(id);
        paymentRepository.deleteAll(paymentRepository.findByBookingId(booking.getId()));
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
        bookingSeats.forEach(bookingSeat -> {
            bookingSeat.getShowSeat().setSeatStatus(ShowSeatStatus.AVAILABLE);
            bookingSeat.getShowSeat().setLockedByUser(null);
            bookingSeat.getShowSeat().setLockedUntil(null);
        });
        bookingSeatRepository.deleteAll(bookingSeats);
        bookingRepository.delete(booking);
    }

    public Booking getBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private User resolveBookingUser(BookingRequest request, Principal principal) {
        if (principal != null) {
            return authService.currentUser(principal.getName());
        }
        if (request.userId() == null) {
            throw new BadRequestException("userId is required when no authenticated user is present");
        }
        return userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public BookingResponse toResponse(Booking booking) {
        List<ShowSeatResponse> seats = bookingSeatRepository.findByBookingId(booking.getId()).stream()
                .map(BookingSeat::getShowSeat)
                .map(showService::toSeatResponse)
                .toList();
        return new BookingResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getShow().getId(),
                booking.getShow().getMovie().getTitle(),
                booking.getBookingStatus(),
                booking.getSeatsCount(),
                booking.getTotalAmount(),
                seats);
    }
}

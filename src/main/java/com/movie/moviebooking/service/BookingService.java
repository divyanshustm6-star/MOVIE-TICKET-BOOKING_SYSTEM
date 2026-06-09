package com.movie.moviebooking.service;

import com.movie.moviebooking.dto.ApiDtos.BookingRequest;
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
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookingService.class);

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
    public com.movie.moviebooking.dto.ApiDtos.BookingResponseDto book(BookingRequest request, Principal principal) {
        User user = resolveBookingUser(request, principal);
        MovieShow show = showService.getShow(request.showId());
        List<ShowSeat> selectedSeats = request.showSeatIds().stream()
                .map(id -> showSeatRepository.findByIdAndShowIdForUpdate(id, request.showId())
                        .orElseThrow(() -> new ResourceNotFoundException("Show seat not found: " + id)))
                .toList();

        if (selectedSeats.stream().anyMatch(seat -> seat.getSeatStatus() != ShowSeatStatus.AVAILABLE)) {
            throw new BadRequestException("One or more selected seats are not available");
        }

        // compute subtotal using show seat price; if missing, fall back to physical seat price
        BigDecimal subtotal = selectedSeats.stream()
                .map(ss -> ss.getPrice() != null ? ss.getPrice() : (ss.getSeat() != null && ss.getSeat().getPrice() != null ? ss.getSeat().getPrice() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.18"));
        BigDecimal total = subtotal.add(tax);

        log.debug("Computed booking amounts: subtotal={}, tax={}, total={}", subtotal, tax, total);

        // Prevent accidental zero-total bookings unless all selected seats truly have zero price
        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
            boolean allZero = selectedSeats.stream()
                    .allMatch(ss -> {
                        java.math.BigDecimal p1 = ss.getPrice();
                        java.math.BigDecimal p2 = (ss.getSeat() != null ? ss.getSeat().getPrice() : null);
                        return (p1 == null || p1.compareTo(BigDecimal.ZERO) == 0) && (p2 == null || p2.compareTo(BigDecimal.ZERO) == 0);
                    });
            if (!allZero) {
                log.error("Booking subtotal computed as 0 but some underlying seat prices are non-zero or missing. showId={}, seatIds={}", request.showId(), request.showSeatIds());
                throw new BadRequestException("Booking total is zero due to missing seat prices. Contact admin.");
            }
        }

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
            // persist the effective seat price: prefer show-seat price, else fallback to seat.price
            java.math.BigDecimal effectivePrice = showSeat.getPrice() != null ? showSeat.getPrice() : (showSeat.getSeat() != null ? showSeat.getSeat().getPrice() : java.math.BigDecimal.ZERO);
            bookingSeat.setSeatPrice(effectivePrice);
            bookingSeatRepository.save(bookingSeat);
        });

        return toResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<com.movie.moviebooking.dto.ApiDtos.BookingResponseDto> history(Principal principal) {
        if (principal == null) {
            return findAll();
        }
        return bookingRepository.findHistoryWithRelations(principal.getName()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.movie.moviebooking.dto.ApiDtos.BookingResponseDto> findAll() {
        return bookingRepository.findAllWithRelations().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public com.movie.moviebooking.dto.ApiDtos.BookingResponseDto findById(Long id) {
        return toResponse(getBooking(id));
    }

    @Transactional
    public com.movie.moviebooking.dto.ApiDtos.BookingResponseDto update(Long id, BookingUpdateRequest request) {
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
        log.info("[Booking Service] Retrieving booking by ID: {}", id);
        Booking booking = bookingRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (booking.getUser() != null) {
            log.info("[Booking Service] Booking retrieved successfully. User Email: {}, User Name: {}", 
                     booking.getUser().getEmail(), booking.getUser().getFullName());
        } else {
            log.warn("[Booking Service] Booking retrieved successfully but User is NULL!");
        }
        return booking;
    }

    @Transactional
    public void updateTicketPath(Long bookingId, String path) {
        Booking booking = getBooking(bookingId);
        booking.setTicketPath(path);
        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);
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

    public com.movie.moviebooking.dto.ApiDtos.BookingResponseDto toResponse(Booking booking) {
        try {
            // map seats to human-readable seat labels like A1
            List<String> seatNumbers = bookingSeatRepository.findByBookingId(booking.getId()).stream()
                    .map(BookingSeat::getShowSeat)
                    .map(ss -> {
                        // ss.getSeat() may be lazy but this method is always used inside @Transactional methods
                        if (ss == null) return "";
                        if (ss.getSeat() != null) {
                            return ss.getSeat().getRowLabel() + ss.getSeat().getSeatNumber();
                        }
                        // No Seat linked: fallback to using showSeat id label to avoid NPE and provide traceable value
                        return "showSeat-" + ss.getId();
                    })
                    .filter(s -> s != null && !s.isBlank())
                    .toList();

            String theaterName = null;
            String screenName = null;
            java.time.LocalDateTime startsAt = null;
            Long showId = null;
            String posterUrl = null;
            if (booking.getShow() != null) {
                MovieShow s = booking.getShow();
                showId = s.getId();
                startsAt = s.getStartsAt();
                if (s.getScreen() != null) {
                    screenName = s.getScreen().getName();
                    if (s.getScreen().getTheater() != null) theaterName = s.getScreen().getTheater().getName();
                }
                if (s.getMovie() != null) {
                    posterUrl = s.getMovie().getPosterUrl();
                }
            }

            String userEmail = booking.getUser() != null ? booking.getUser().getEmail() : null;

            return new com.movie.moviebooking.dto.ApiDtos.BookingResponseDto(
                    booking.getId(),
                    booking.getBookingReference(),
                    showId,
                    booking.getShow() != null && booking.getShow().getMovie() != null ? booking.getShow().getMovie().getTitle() : null,
                    theaterName,
                    screenName,
                    posterUrl,
                    startsAt,
                    booking.getBookingStatus(),
                    booking.getSeatsCount(),
                    seatNumbers,
                    booking.getTotalAmount(),
                    userEmail
            );
        } catch (Exception e) {
            // log full stack trace so Spring console shows root cause
            e.printStackTrace();
            throw e;
        }
    }
}

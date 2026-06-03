package com.movie.moviebooking.dto;

import com.movie.moviebooking.entity.BookingStatus;
import com.movie.moviebooking.entity.MovieStatus;
import com.movie.moviebooking.entity.PaymentMethod;
import com.movie.moviebooking.entity.PaymentStatus;
import com.movie.moviebooking.entity.ScreenStatus;
import com.movie.moviebooking.entity.ScreenType;
import com.movie.moviebooking.entity.SeatStatus;
import com.movie.moviebooking.entity.SeatType;
import com.movie.moviebooking.entity.ShowSeatStatus;
import com.movie.moviebooking.entity.ShowStatus;
import com.movie.moviebooking.entity.TheaterStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record RegisterRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            String phone,
            @NotBlank @Size(min = 6, message = "must be at least 6 characters") String password) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 6, message = "must be at least 6 characters") String password) {
    }

    public record AuthResponse(String token, String tokenType, Long expiresInSeconds, Long userId, String email, String fullName, String phone, Set<String> roles) {
    }

    public record UserResponse(Long id, String fullName, String email, String phone, Set<String> roles) {
    }

    public record UserRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            String phone,
            String password,
            Set<String> roles) {
    }

    public record MovieRequest(
            @NotBlank String title,
            String description,
            @NotBlank String language,
            @Min(1) Integer durationMinutes,
            LocalDate releaseDate,
            String certification,
            String posterUrl,
            String trailerUrl,
            MovieStatus status,
            Set<String> genres) {
    }

    public record MovieResponse(
            Long id,
            String title,
            String description,
            String language,
            Integer durationMinutes,
            LocalDate releaseDate,
            String certification,
            String posterUrl,
            String trailerUrl,
            MovieStatus status,
            Set<String> genres) {
    }

    public record TheaterRequest(
            @NotBlank String name,
            @NotBlank String addressLine1,
            String addressLine2,
            @NotBlank String city,
            @NotBlank String state,
            @NotBlank String postalCode,
            String country,
            BigDecimal latitude,
            BigDecimal longitude,
            String contactPhone,
            TheaterStatus status) {
    }

    public record TheaterResponse(
            Long id,
            String name,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            BigDecimal latitude,
            BigDecimal longitude,
            String contactPhone,
            TheaterStatus status) {
    }

    public record ScreenRequest(
            @NotNull Long theaterId,
            @NotBlank String name,
            ScreenType screenType,
            @Min(1) Integer totalSeats,
            ScreenStatus status) {
    }

    public record ScreenResponse(Long id, Long theaterId, String name, ScreenType screenType, Integer totalSeats, ScreenStatus status) {
    }

    public record SeatRequest(
            @NotNull Long screenId,
            @NotBlank String rowLabel,
            @Min(1) Integer seatNumber,
            SeatType seatType,
            SeatStatus status,
            @Positive java.math.BigDecimal price) {
    }

    public record SeatResponse(Long id, Long screenId, String rowLabel, Integer seatNumber, SeatType seatType, SeatStatus status, java.math.BigDecimal price) {
    }

    public record GenerateSeatsRequest(
            @NotBlank String startRow,
            String endRow,
            @Min(1) Integer seatsPerRow,
            SeatType seatType,
            @Positive java.math.BigDecimal price) {
    }

    public record ShowRequest(
            @NotNull Long movieId,
            @NotNull Long screenId,
            @NotNull LocalDate showDate,
            @NotNull @FutureOrPresent LocalDateTime startsAt,
            @NotNull LocalDateTime endsAt,
            ShowStatus status) {
    }

    public record ShowResponse(
            Long id,
            Long movieId,
            String movieTitle,
            String theaterName,
            String screenName,
            LocalDate showDate,
            LocalDateTime showTime,
            BigDecimal price,
            Integer availableSeats) {
    }

    public record ShowSeatResponse(
            Long id,
            Long showId,
            Long seatId,
            String rowLabel,
            Integer seatNumber,
            SeatType seatType,
            BigDecimal price,
            ShowSeatStatus seatStatus) {
    }

    public record BookingRequest(Long userId, @NotNull Long showId, @NotEmpty List<Long> showSeatIds) {
    }

    public record BookingUpdateRequest(@NotNull BookingStatus bookingStatus) {
    }

    public record BookingResponse(
            Long id,
            String bookingReference,
            Long showId,
            String movieTitle,
            BookingStatus bookingStatus,
            Integer seatsCount,
            BigDecimal totalAmount,
            List<ShowSeatResponse> seats) {
    }

    public record PaymentRequest(
            @NotNull Long bookingId,
            @NotNull PaymentMethod paymentMethod,
            @NotBlank String provider,
            String providerTransactionId,
            String upiId,
            // card
            String cardNumber,
            String cardHolderName,
            Integer cardExpiryMonth,
            Integer cardExpiryYear,
            // net banking
            String bankName,
            String accountHolderName,
            String referenceNumber,
            // wallet
            String walletProvider,
            String walletMobile) {
    }

    public record PaymentUpdateRequest(
            @NotNull PaymentStatus paymentStatus,
            String providerTransactionId,
            String failureReason) {
    }

    public record PaymentResponse(
            Long id,
            Long bookingId,
            String paymentReference,
            String provider,
            String providerTransactionId,
            String upiId,
            String cardLast4,
            String cardHolderName,
            Integer cardExpiryMonth,
            Integer cardExpiryYear,
            String bankName,
            String accountHolderName,
            String referenceNumber,
            String walletProvider,
            String walletMobile,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            BigDecimal amount,
            String currency) {
    }
}

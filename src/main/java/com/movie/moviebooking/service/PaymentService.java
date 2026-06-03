package com.movie.moviebooking.service;

import com.movie.moviebooking.dto.ApiDtos.PaymentRequest;
import com.movie.moviebooking.dto.ApiDtos.PaymentResponse;
import com.movie.moviebooking.dto.ApiDtos.PaymentUpdateRequest;
import com.movie.moviebooking.entity.Booking;
import com.movie.moviebooking.entity.BookingStatus;
import com.movie.moviebooking.entity.Payment;
import com.movie.moviebooking.entity.PaymentStatus;
import com.movie.moviebooking.entity.ShowSeatStatus;
import com.movie.moviebooking.exception.ResourceNotFoundException;
import com.movie.moviebooking.repository.BookingRepository;
import com.movie.moviebooking.repository.BookingSeatRepository;
import com.movie.moviebooking.repository.PaymentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingService bookingService,
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
    }

    @Transactional
    public PaymentResponse pay(PaymentRequest request) {
        Booking booking = bookingService.getBooking(request.bookingId());
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentReference("PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        // validate and map dynamic payment fields
        payment.setProvider(request.provider());
        payment.setProviderTransactionId(request.providerTransactionId());
        payment.setUpiId(request.upiId());

        // CARD
        if (request.cardNumber() != null && !request.cardNumber().isBlank()) {
            String digits = request.cardNumber().replaceAll("\\D", "");
            if (digits.length() < 12 || digits.length() > 19) {
                throw new com.movie.moviebooking.exception.BadRequestException("Invalid card number");
            }
            payment.setCardLast4(digits.substring(digits.length() - 4));
            payment.setCardHolderName(request.cardHolderName());
            payment.setCardExpiryMonth(request.cardExpiryMonth());
            payment.setCardExpiryYear(request.cardExpiryYear());
        }

        // NET_BANKING
        if (request.bankName() != null && !request.bankName().isBlank()) {
            payment.setBankName(request.bankName());
            payment.setAccountHolderName(request.accountHolderName());
            payment.setReferenceNumber(request.referenceNumber());
        }

        // WALLET
        if (request.walletProvider() != null && !request.walletProvider().isBlank()) {
            payment.setWalletProvider(request.walletProvider());
            payment.setWalletMobile(request.walletMobile());
        }

        // Basic UPI validation when provided
        if (request.paymentMethod() == com.movie.moviebooking.entity.PaymentMethod.UPI) {
            String upi = request.upiId();
            if (upi == null || !upi.matches("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$")) {
                throw new com.movie.moviebooking.exception.BadRequestException("Invalid UPI ID");
            }
            payment.setUpiId(upi);
        }

        payment.setPaymentMethod(request.paymentMethod());
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency("INR");
        payment.setPaidAt(Instant.now());

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);
        bookingSeatRepository.findByBookingId(booking.getId()).forEach(bookingSeat -> {
            bookingSeat.getShowSeat().setSeatStatus(ShowSeatStatus.BOOKED);
            bookingSeat.getShowSeat().setLockedByUser(null);
            bookingSeat.getShowSeat().setLockedUntil(null);
        });

        return toResponse(paymentRepository.save(payment));
    }

    public List<PaymentResponse> findByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId).stream().map(this::toResponse).toList();
    }

    public List<PaymentResponse> findAll() {
        return paymentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public PaymentResponse findById(Long id) {
        return toResponse(getPayment(id));
    }

    @Transactional
    public PaymentResponse update(Long id, PaymentUpdateRequest request) {
        Payment payment = getPayment(id);
        payment.setPaymentStatus(request.paymentStatus());
        payment.setProviderTransactionId(request.providerTransactionId());
        payment.setFailureReason(request.failureReason());
        payment.setUpdatedAt(Instant.now());
        if (request.paymentStatus() == PaymentStatus.SUCCESS && payment.getPaidAt() == null) {
            payment.setPaidAt(Instant.now());
        }
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(Long id) {
        paymentRepository.delete(getPayment(id));
    }

    public Payment getPayment(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getPaymentReference(),
                payment.getProvider(),
                payment.getProviderTransactionId(),
                payment.getUpiId(),
                payment.getCardLast4(),
                payment.getCardHolderName(),
                payment.getCardExpiryMonth(),
                payment.getCardExpiryYear(),
                payment.getBankName(),
                payment.getAccountHolderName(),
                payment.getReferenceNumber(),
                payment.getWalletProvider(),
                payment.getWalletMobile(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getAmount(),
                payment.getCurrency());
    }
}

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
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.razorpay.RazorpayClient;
import com.razorpay.Order;
import org.json.JSONObject;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final com.movie.moviebooking.service.TicketService ticketService;
    private final com.movie.moviebooking.service.EmailService emailService;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingService bookingService,
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            com.movie.moviebooking.service.TicketService ticketService,
            com.movie.moviebooking.service.EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.ticketService = ticketService;
        this.emailService = emailService;
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

    /**
     * Create a Razorpay order for a booking (test mode supported via properties).
     */
    public Map<String, Object> createRazorpayOrder(Long bookingId) throws Exception {
        Booking booking = bookingService.getBooking(bookingId);
        // amount in paise
        long amountPaise = booking.getTotalAmount().multiply(new java.math.BigDecimal(100)).longValue();
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", booking.getBookingReference());
        orderRequest.put("payment_capture", 1);
        Order order = client.orders.create(orderRequest);
        // save a payment skeleton
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setPaymentStatus(com.movie.moviebooking.entity.PaymentStatus.INITIATED);
        payment.setRazorpayOrderId(order.get("id"));
        paymentRepository.save(payment);
        Map<String, Object> resp = new HashMap<>();
        resp.put("orderId", order.get("id"));
        resp.put("amount", amountPaise);
        resp.put("currency", "INR");
        resp.put("key", razorpayKeyId);
        resp.put("bookingId", booking.getId());
        return resp;
    }

    /**
     * Verify Razorpay signature and complete booking/payment.
     */
    public Map<String, Object> verifyAndCompleteRazorpayPayment(Long bookingId, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) throws Exception {
        Booking booking = bookingService.getBooking(bookingId);
        // verify signature HMAC_SHA256(orderId|paymentId, secret)
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256");
        sha256Hmac.init(keySpec);
        byte[] macData = sha256Hmac.doFinal(payload.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : macData) sb.append(String.format("%02x", b));
        String computed = sb.toString();
        if (!computed.equals(razorpaySignature)) {
            // mark booking as FAILED and return
            booking.setBookingStatus(BookingStatus.CANCELLED);
            booking.setUpdatedAt(Instant.now());
            bookingRepository.save(booking);
            throw new com.movie.moviebooking.exception.BadRequestException("Invalid payment signature");
        }
        // find payment skeleton
        java.util.List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        Payment payment = payments.stream().filter(p -> razorpayOrderId.equals(p.getRazorpayOrderId())).findFirst().orElse(new Payment());
        payment.setBooking(booking);
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setPaymentStatus(com.movie.moviebooking.entity.PaymentStatus.SUCCESS);
        payment.setAmount(booking.getTotalAmount());
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);

        // finalize booking
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(com.movie.moviebooking.entity.PaymentStatus.SUCCESS);
        booking.setPaidAt(Instant.now());
        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);

        // mark seats as BOOKED
        bookingSeatRepository.findByBookingId(booking.getId()).forEach(bs -> {
            bs.getShowSeat().setSeatStatus(ShowSeatStatus.BOOKED);
            bs.getShowSeat().setLockedByUser(null);
            bs.getShowSeat().setLockedUntil(null);
        });

        byte[] pdfBytes = null;
        // generate ticket PDF
        try {
            pdfBytes = ticketService.generateTicketPdf(booking.getId());
        } catch (Exception e) {
            // log and continue
            System.err.println("Failed to generate ticket PDF: " + e.getMessage());
        }

        // send email with ticket if user email present
        try {
            if (booking.getUser() != null && booking.getUser().getEmail() != null && pdfBytes != null) {
                String to = booking.getUser().getEmail();
                String subject = "Movie Ticket Confirmation - " + booking.getShow().getMovie().getTitle();
                String body = "<p>Hi " + booking.getUser().getFullName() + ",</p>" +
                        "<p>Thanks for booking. Attached is your ticket.</p>" +
                        "<p>Booking reference: " + booking.getBookingReference() + "</p>";
                emailService.sendTicketEmail(to, subject, body, pdfBytes, "ticket-" + booking.getBookingReference() + ".pdf");
            }
        } catch (Exception e) {
            System.err.println("Failed to send ticket email: " + e.getMessage());
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "OK");
        resp.put("bookingId", booking.getId());
        resp.put("paymentId", razorpayPaymentId);
        return resp;
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

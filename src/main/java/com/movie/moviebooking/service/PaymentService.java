package com.movie.moviebooking.service;

import com.movie.moviebooking.dto.ApiDtos.PaymentRequest;
import com.movie.moviebooking.dto.ApiDtos.PaymentResponse;
import com.movie.moviebooking.dto.ApiDtos.PaymentUpdateRequest;
import com.movie.moviebooking.entity.Booking;
import com.movie.moviebooking.entity.BookingSeat;
import com.movie.moviebooking.entity.BookingStatus;
import com.movie.moviebooking.entity.Payment;
import com.movie.moviebooking.entity.PaymentStatus;
import com.movie.moviebooking.entity.ShowSeat;
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
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final com.movie.moviebooking.service.TicketService ticketService;
    private final com.movie.moviebooking.service.EmailService emailService;

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
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

    @Transactional(readOnly = true)
    public List<PaymentResponse> findByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId).stream().map(this::toResponse).toList();
    }

    /**
     * Create a Razorpay order for a booking (test mode supported via properties).
     */
    @Transactional
    public Map<String, Object> createRazorpayOrder(Long bookingId) throws Exception {
        Booking booking = bookingService.getBooking(bookingId);
        // amount in paise
        long amountPaise = booking.getTotalAmount().multiply(new java.math.BigDecimal(100)).longValue();
        log.info("[Razorpay Order Creation] Booking ID: {}, Amount: {} INR ({} paise)", bookingId, booking.getTotalAmount(), amountPaise);
        log.info("[Razorpay Credentials check] razorpayKeyId: '{}', razorpayKeySecret is null: {}, razorpayKeySecret length: {}", 
                 razorpayKeyId, (razorpayKeySecret == null), (razorpayKeySecret != null ? razorpayKeySecret.length() : 0));
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", booking.getBookingReference());
        orderRequest.put("payment_capture", 1);
        Order order = client.orders.create(orderRequest);
        // save a payment skeleton — ensure non-null paymentMethod and paymentReference to satisfy DB constraints
        Payment payment = new Payment();
        payment.setBooking(booking);
        // generate a payment reference similar to the manual pay() flow
        payment.setPaymentReference("PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        payment.setAmount(booking.getTotalAmount());
        payment.setPaymentStatus(com.movie.moviebooking.entity.PaymentStatus.INITIATED);
        payment.setRazorpayOrderId(order.get("id"));
        // default to CARD for Razorpay skeleton; the exact method will be finalized on verification
        payment.setPaymentMethod(com.movie.moviebooking.entity.PaymentMethod.CARD);
        payment.setProvider("RAZORPAY");
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
    @Transactional
    public Map<String, Object> verifyAndCompleteRazorpayPayment(Long bookingId, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) throws Exception {
        Booking booking = bookingService.getBooking(bookingId);

        // Idempotency Check
        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            log.info("Booking ID: {} is already CONFIRMED. Skipping duplicate execution.", bookingId);
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "OK");
            resp.put("bookingId", booking.getId());
            resp.put("paymentId", razorpayPaymentId);
            return resp;
        }

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
        // ensure paymentMethod is set (safety for skeleton-less or incomplete records)
        if (payment.getPaymentMethod() == null) {
            payment.setPaymentMethod(com.movie.moviebooking.entity.PaymentMethod.CARD);
        }
        if (payment.getPaymentReference() == null) {
            payment.setPaymentReference("PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        }
        if (payment.getProvider() == null) {
            payment.setProvider("RAZORPAY");
        }
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

        // Generate Ticket PDF
        byte[] pdfBytes = null;
        try {
            log.info("[Payment Service] Starting PDF ticket generation for booking ID: {}", booking.getId());
            pdfBytes = ticketService.generateTicketPdf(booking.getId());
            log.info("[Payment Service] PDF ticket generation completed. Bytes generated: {}", pdfBytes != null ? pdfBytes.length : 0);
        } catch (Exception e) {
            log.error("[Payment Service] Failed to generate ticket PDF for booking ID {}: {}", booking.getId(), e.getMessage(), e);
        }

        // Format details for HTML email template
        String userName = booking.getUser() != null ? booking.getUser().getFullName() : "Valued Customer";
        String userEmail = booking.getUser() != null ? booking.getUser().getEmail() : "N/A";
        String movieName = booking.getShow() != null && booking.getShow().getMovie() != null ? booking.getShow().getMovie().getTitle() : "N/A";
        String theaterName = (booking.getShow() != null && booking.getShow().getScreen() != null && booking.getShow().getScreen().getTheater() != null) ?
                booking.getShow().getScreen().getTheater().getName() : "N/A";
        
        java.time.format.DateTimeFormatter emailDateFormat = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy");
        java.time.format.DateTimeFormatter emailTimeFormat = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
        
        String showDate = booking.getShow() != null ? booking.getShow().getShowDate().format(emailDateFormat) : "N/A";
        String showTime = booking.getShow() != null ? booking.getShow().getStartsAt().format(emailTimeFormat) : "N/A";
        
        StringBuilder seatsSb = new StringBuilder();
        for (int i = 0; i < booking.getSeats().size(); i++) {
            BookingSeat bs = booking.getSeats().get(i);
            if (bs.getShowSeat() != null && bs.getShowSeat().getSeat() != null) {
                seatsSb.append(bs.getShowSeat().getSeat().getRowLabel()).append(bs.getShowSeat().getSeat().getSeatNumber());
                if (i < booking.getSeats().size() - 1) {
                    seatsSb.append(", ");
                }
            }
        }
        String seatNumbers = seatsSb.toString().isEmpty() ? "N/A" : seatsSb.toString();
        int ticketsCount = booking.getSeatsCount();
        String amountPaid = booking.getTotalAmount() != null ? booking.getTotalAmount().toString() : "0.00";
        String bookingStatus = booking.getBookingStatus() != null ? booking.getBookingStatus().toString() : "PENDING";
        
        java.time.format.DateTimeFormatter bookingCreatedFormat = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a")
                .withZone(java.time.ZoneId.systemDefault());
        String bookingDate = bookingCreatedFormat.format(booking.getCreatedAt() != null ? booking.getCreatedAt() : Instant.now());

        String htmlBody = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"utf-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <title>Booking Confirmed</title>" +
                "    <style>" +
                "        body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f4f6f9; color: #333333; margin: 0; padding: 0; }" +
                "        .container { max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08); }" +
                "        .header { background-color: #0f172a; color: #ffffff; padding: 25px 20px; text-align: center; }" +
                "        .header h1 { margin: 0; font-size: 24px; font-weight: 800; color: #f97316; letter-spacing: 1px; }" +
                "        .banner { background-color: #ecfdf5; border-left: 5px solid #10b981; padding: 15px 20px; margin: 20px; border-radius: 4px; }" +
                "        .banner-title { color: #065f46; font-weight: 700; font-size: 16px; margin: 0 0 5px 0; }" +
                "        .banner-desc { color: #047857; font-size: 14px; margin: 0; }" +
                "        .details-section { padding: 0 20px 20px 20px; }" +
                "        .details-table { width: 100%; border-collapse: collapse; margin-top: 15px; }" +
                "        .details-table td { padding: 12px 15px; border-bottom: 1px solid #e2e8f0; font-size: 14px; }" +
                "        .details-table td.label { font-weight: 600; color: #64748b; width: 40%; }" +
                "        .details-table td.value { color: #0f172a; font-weight: 500; }" +
                "        .footer { background-color: #f8fafc; padding: 20px; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #94a3b8; }" +
                "        .thank-you { font-size: 16px; font-weight: 600; color: #0f172a; text-align: center; margin: 25px 0; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"header\">" +
                "            <h1>DIVYANSHU MOVIES</h1>" +
                "        </div>" +
                "        <div class=\"banner\">" +
                "            <h2 class=\"banner-title\">🎟️ Booking Confirmed!</h2>" +
                "            <p class=\"banner-desc\">Your payment has been verified successfully. Your booking is confirmed. Below are your booking details.</p>" +
                "        </div>" +
                "        <div class=\"details-section\">" +
                "            <table class=\"details-table\">" +
                "                <tr><td class=\"label\">User Name</td><td class=\"value\">" + userName + "</td></tr>" +
                "                <tr><td class=\"label\">User Email</td><td class=\"value\">" + userEmail + "</td></tr>" +
                "                <tr><td class=\"label\">Booking ID</td><td class=\"value\">#" + booking.getId() + "</td></tr>" +
                "                <tr><td class=\"label\">Movie Name</td><td class=\"value\">" + movieName + "</td></tr>" +
                "                <tr><td class=\"label\">Theater Name</td><td class=\"value\">" + theaterName + "</td></tr>" +
                "                <tr><td class=\"label\">Show Date</td><td class=\"value\">" + showDate + "</td></tr>" +
                "                <tr><td class=\"label\">Show Time</td><td class=\"value\">" + showTime + "</td></tr>" +
                "                <tr><td class=\"label\">Seat Numbers</td><td class=\"value\">" + seatNumbers + "</td></tr>" +
                "                <tr><td class=\"label\">Number of Tickets</td><td class=\"value\">" + ticketsCount + "</td></tr>" +
                "                <tr><td class=\"label\">Amount Paid</td><td class=\"value\">Rs. " + amountPaid + "</td></tr>" +
                "                <tr><td class=\"label\">Payment ID</td><td class=\"value\">" + razorpayPaymentId + "</td></tr>" +
                "                <tr><td class=\"label\">Booking Status</td><td class=\"value\">" + bookingStatus + "</td></tr>" +
                "                <tr><td class=\"label\">Booking Date</td><td class=\"value\">" + bookingDate + "</td></tr>" +
                "            </table>" +
                "            <div class=\"thank-you\">Thank you for booking with Divyanshu Movies! Enjoy your show!</div>" +
                "        </div>" +
                "        <div class=\"footer\">" +
                "            &copy; 2026 Divyanshu Movies. All rights reserved." +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";

        // Send Email confirmation
        try {
            if (booking.getUser() != null && booking.getUser().getEmail() != null) {
                String to = booking.getUser().getEmail();
                String subject = "Movie Ticket Confirmation - " + movieName;
                log.info("[Payment Service] Triggering email confirmation call...");
                log.info("[Payment Service] Recipient Email: {}", to);
                log.info("[Payment Service] Subject: {}", subject);
                log.info("[Payment Service] Ticket Path: {}", booking.getTicketPath());

                if (pdfBytes != null) {
                    emailService.sendTicketEmail(to, subject, htmlBody, pdfBytes, "ticket-" + booking.getBookingReference() + ".pdf");
                    log.info("[Payment Service] EmailService.sendTicketEmail invocation completed successfully.");
                } else {
                    log.error("[Payment Service] Skipping email trigger: pdfBytes are NULL!");
                }
            } else {
                log.error("[Payment Service] Skipping email trigger: User or User email is NULL!");
            }
        } catch (Exception e) {
            log.error("[Payment Service] CRITICAL ERROR during email transmission: {}", e.getMessage(), e);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "OK");
        resp.put("bookingId", booking.getId());
        resp.put("paymentId", razorpayPaymentId);
        return resp;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll() {
        return paymentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
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

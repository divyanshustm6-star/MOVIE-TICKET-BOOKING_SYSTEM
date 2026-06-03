package com.movie.moviebooking.controller;

import com.movie.moviebooking.dto.ApiDtos.PaymentRequest;
import com.movie.moviebooking.dto.ApiDtos.PaymentResponse;
import com.movie.moviebooking.dto.ApiDtos.PaymentUpdateRequest;
import com.movie.moviebooking.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public PaymentResponse pay(@Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<PaymentResponse> all() {
        return paymentService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse byId(@PathVariable Long id) {
        return paymentService.findById(id);
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<PaymentResponse> byBooking(@PathVariable Long bookingId) {
        return paymentService.findByBooking(bookingId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse update(@PathVariable Long id, @Valid @RequestBody PaymentUpdateRequest request) {
        return paymentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        paymentService.delete(id);
    }
}

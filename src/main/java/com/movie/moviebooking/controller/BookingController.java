package com.movie.moviebooking.controller;

import com.movie.moviebooking.dto.ApiDtos.BookingRequest;
import com.movie.moviebooking.dto.ApiDtos.BookingResponseDto;
import com.movie.moviebooking.dto.ApiDtos.BookingUpdateRequest;
import com.movie.moviebooking.service.BookingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final com.movie.moviebooking.service.TicketService ticketService;

    public BookingController(BookingService bookingService, com.movie.moviebooking.service.TicketService ticketService) {
        this.bookingService = bookingService;
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public BookingResponseDto book(@Valid @RequestBody BookingRequest request, Principal principal) {
        return bookingService.book(request, principal);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingResponseDto> all() {
        return bookingService.findAll();
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<BookingResponseDto> history(Principal principal) {
        return bookingService.history(principal);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public BookingResponseDto byId(@PathVariable Long id, java.security.Principal principal) {
        try {
            // fetch DTO with required fields (method is transactional and returns DTO)
            com.movie.moviebooking.dto.ApiDtos.BookingResponseDto dto = bookingService.findById(id);
            String ownerEmail = dto.userEmail();
            boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                if (principal == null || ownerEmail == null || !principal.getName().equals(ownerEmail)) {
                    throw new org.springframework.security.access.AccessDeniedException("Access denied");
                }
            }
            return dto;
        } catch (Exception e) {
            // surface stack trace in Spring console for debugging
            e.printStackTrace();
            throw e;
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BookingResponseDto update(@PathVariable Long id, @Valid @RequestBody BookingUpdateRequest request) {
        return bookingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        bookingService.delete(id);
    }

    @GetMapping("/{id}/ticket")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public org.springframework.http.ResponseEntity<byte[]> downloadTicket(@PathVariable Long id, java.security.Principal principal) throws Exception {
        try {
            com.movie.moviebooking.dto.ApiDtos.BookingResponseDto dto = bookingService.findById(id);
            String ownerEmail = dto.userEmail();
            boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                if (principal == null || ownerEmail == null || !principal.getName().equals(ownerEmail)) {
                    throw new org.springframework.security.access.AccessDeniedException("Access denied");
                }
            }
            byte[] pdf = ticketService.loadTicketBytes(id);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=booking-" + id + ".pdf");
            headers.add(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/pdf");
            return new org.springframework.http.ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}


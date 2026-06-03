package com.movie.moviebooking.controller;

import com.movie.moviebooking.dto.ApiDtos.ShowRequest;
import com.movie.moviebooking.dto.ApiDtos.ShowResponse;
import com.movie.moviebooking.dto.ApiDtos.ShowSeatResponse;
import com.movie.moviebooking.service.ShowService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shows")
public class ShowController {
    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping
    public List<ShowResponse> all(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) LocalDate date) {
        return showService.findAll(movieId, date);
    }

    @GetMapping("/{id}")
    public ShowResponse byId(@PathVariable Long id) {
        return showService.findById(id);
    }

    @GetMapping("/{id}/seats")
    public List<ShowSeatResponse> seats(@PathVariable Long id) {
        return showService.seats(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ShowResponse create(@Valid @RequestBody ShowRequest request) {
        return showService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ShowResponse update(@PathVariable Long id, @Valid @RequestBody ShowRequest request) {
        return showService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        showService.delete(id);
    }
}

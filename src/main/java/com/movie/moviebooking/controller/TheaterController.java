package com.movie.moviebooking.controller;

import com.movie.moviebooking.dto.ApiDtos.ScreenRequest;
import com.movie.moviebooking.dto.ApiDtos.ScreenResponse;
import com.movie.moviebooking.dto.ApiDtos.SeatRequest;
import com.movie.moviebooking.dto.ApiDtos.SeatResponse;
import com.movie.moviebooking.dto.ApiDtos.TheaterRequest;
import com.movie.moviebooking.dto.ApiDtos.TheaterResponse;
import com.movie.moviebooking.service.ScreenService;
import com.movie.moviebooking.service.SeatService;
import com.movie.moviebooking.service.TheaterService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/theaters")
public class TheaterController {
    private final TheaterService theaterService;
    private final ScreenService screenService;
    private final SeatService seatService;

    public TheaterController(TheaterService theaterService, ScreenService screenService, SeatService seatService) {
        this.theaterService = theaterService;
        this.screenService = screenService;
        this.seatService = seatService;
    }

    @GetMapping
    public List<TheaterResponse> all(@RequestParam(required = false) String city) {
        return theaterService.findAll(city);
    }

    @GetMapping("/{id}")
    public TheaterResponse byId(@PathVariable Long id) {
        return theaterService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TheaterResponse create(@Valid @RequestBody TheaterRequest request) {
        return theaterService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TheaterResponse update(@PathVariable Long id, @Valid @RequestBody TheaterRequest request) {
        return theaterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        theaterService.delete(id);
    }

    @GetMapping("/{theaterId}/screens")
    public List<ScreenResponse> screens(@PathVariable Long theaterId) {
        return screenService.findAll(theaterId);
    }

    @PostMapping("/screens")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ScreenResponse createScreen(@Valid @RequestBody ScreenRequest request) {
        return screenService.create(request);
    }

    @PutMapping("/screens/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ScreenResponse updateScreen(@PathVariable Long id, @Valid @RequestBody ScreenRequest request) {
        return screenService.update(id, request);
    }

    @DeleteMapping("/screens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteScreen(@PathVariable Long id) {
        screenService.delete(id);
    }

    @GetMapping("/screens/{screenId}/seats")
    public List<SeatResponse> seats(@PathVariable Long screenId) {
        return seatService.findByScreen(screenId);
    }

    @PostMapping("/seats")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SeatResponse createSeat(@Valid @RequestBody SeatRequest request) {
        return seatService.create(request);
    }

    @PutMapping("/seats/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SeatResponse updateSeat(@PathVariable Long id, @Valid @RequestBody SeatRequest request) {
        return seatService.update(id, request);
    }

    @DeleteMapping("/seats/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSeat(@PathVariable Long id) {
        seatService.delete(id);
    }
}

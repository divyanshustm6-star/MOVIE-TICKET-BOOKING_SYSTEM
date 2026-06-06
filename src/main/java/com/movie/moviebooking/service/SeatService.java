package com.movie.moviebooking.service;

import com.movie.moviebooking.dto.ApiDtos.GenerateSeatsRequest;
import com.movie.moviebooking.dto.ApiDtos.SeatRequest;
import com.movie.moviebooking.dto.ApiDtos.SeatResponse;
import com.movie.moviebooking.entity.MovieShow;
import com.movie.moviebooking.entity.Seat;
import com.movie.moviebooking.entity.ShowSeat;
import com.movie.moviebooking.exception.ResourceNotFoundException;
import com.movie.moviebooking.repository.BookingSeatRepository;
import com.movie.moviebooking.repository.MovieShowRepository;
import com.movie.moviebooking.repository.SeatRepository;
import com.movie.moviebooking.repository.ShowSeatRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatService {
    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ScreenService screenService;
    private final MovieShowRepository movieShowRepository;

    @org.springframework.beans.factory.annotation.Value("${app.enforce-seat-prices:false}")
    private boolean enforceSeatPrices;

    public SeatService(
            SeatRepository seatRepository,
            ShowSeatRepository showSeatRepository,
            BookingSeatRepository bookingSeatRepository,
            ScreenService screenService,
            MovieShowRepository movieShowRepository) {
        this.seatRepository = seatRepository;
        this.showSeatRepository = showSeatRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.screenService = screenService;
        this.movieShowRepository = movieShowRepository;
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> findByScreen(Long screenId) {
        return seatRepository.findByScreenIdOrderByRowLabelAscSeatNumberAsc(screenId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SeatResponse create(SeatRequest request) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SeatService.class);
        log.debug("SeatService.create: incoming request.price={} for screenId={} row={} num={}", request.price(),
                request.screenId(), request.rowLabel(), request.seatNumber());
        Seat seat = apply(new Seat(), request);
        log.debug("SeatService.create: before save seat.price={}", seat.getPrice());
        Seat saved = seatRepository.save(seat);
        log.debug("SeatService.create: after save saved.price={}", saved.getPrice());
        return toResponse(saved);
    }

    @Transactional
    public List<SeatResponse> generateSeats(Long screenId, GenerateSeatsRequest request) {
        String startRow = request.startRow().trim().toUpperCase();
        String endRow = request.endRow() == null || request.endRow().isBlank() ? startRow : request.endRow().trim().toUpperCase();
        if (startRow.length() != 1 || endRow.length() != 1) {
            throw new com.movie.moviebooking.exception.BadRequestException("Row labels must be single letters");
        }
        char s = startRow.charAt(0);
        char e = endRow.charAt(0);
        if (s > e) { char tmp = s; s = e; e = tmp; }
        int rowsCount = e - s + 1;
        int seatsPerRow = request.seatsPerRow();
        int seatsToCreate = rowsCount * seatsPerRow;

        var screen = screenService.getScreen(screenId);
        long existingCount = seatRepository.countByScreenId(screenId);
        if (screen.getTotalSeats() != null && existingCount + seatsToCreate > screen.getTotalSeats()) {
            throw new com.movie.moviebooking.exception.BadRequestException("Cannot create seats: capacity exceeded");
        }

        List<Seat> toSave = new ArrayList<>();
        for (char row = s; row <= e; row++) {
            String rowLabel = String.valueOf(row);
            for (int i = 1; i <= seatsPerRow; i++) {
                if (!seatRepository.existsByScreenIdAndRowLabelAndSeatNumber(screenId, rowLabel, i)) {
                    Seat seat = new Seat();
                    seat.setScreen(screen);
                    seat.setRowLabel(rowLabel);
                    seat.setSeatNumber(i);
                    seat.setSeatType(request.seatType() == null ? com.movie.moviebooking.entity.SeatType.REGULAR : request.seatType());
                    seat.setStatus(com.movie.moviebooking.entity.SeatStatus.ACTIVE);
                    // set price if provided in request; default to 0 if null
                    if (request.price() != null) seat.setPrice(request.price());
                    toSave.add(seat);
                }
            }
        }
        List<Seat> saved = seatRepository.saveAll(toSave);

        // create show seats for existing shows on this screen
        List<MovieShow> shows = movieShowRepository.findByScreenId(screenId);

        List<ShowSeat> createdShowSeats = new ArrayList<>();
        for (MovieShow show : shows) {
            for (Seat seat : saved) {
                if (!showSeatRepository.existsByShowIdAndSeatId(show.getId(), seat.getId())) {
                    ShowSeat ss = new ShowSeat();
                    ss.setShow(show);
                    ss.setSeat(seat);
                    if (seat.getPrice() == null) {
                        org.slf4j.LoggerFactory.getLogger(SeatService.class).warn("SeatService.generateSeats: seat id={} has null price; setting show_seat.price=0. Enable app.enforce-seat-prices=true to fail.", seat.getId());
                        if (enforceSeatPrices) {
                            throw new com.movie.moviebooking.exception.BadRequestException("Missing price for seat id=" + seat.getId());
                        }
                        ss.setPrice(java.math.BigDecimal.ZERO);
                    } else {
                        ss.setPrice(seat.getPrice());
                    }
                    ss.setSeatStatus(com.movie.moviebooking.entity.ShowSeatStatus.AVAILABLE);
                    createdShowSeats.add(ss);
                }
            }
        }
        if (!createdShowSeats.isEmpty()) showSeatRepository.saveAll(createdShowSeats);

        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SeatResponse update(Long id, SeatRequest request) {
        Seat seat = getSeat(id);
        seat.setUpdatedAt(Instant.now());
        return toResponse(seatRepository.save(apply(seat, request)));
    }

    @Transactional
    public void delete(Long id) {
        List<ShowSeat> showSeats = showSeatRepository.findBySeatId(id);
        showSeats.forEach(showSeat -> bookingSeatRepository.
                deleteAll(bookingSeatRepository.findByShowSeatId(showSeat.getId())));
        showSeatRepository.deleteAll(showSeats);
        seatRepository.delete(getSeat(id));
    }

    public Seat getSeat(Long id) {
        return seatRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
    }

    private Seat apply(Seat seat, SeatRequest request) {
        seat.setScreen(screenService.getScreen(request.screenId()));
        seat.setRowLabel(request.rowLabel());
        seat.setSeatNumber(request.seatNumber());
        if (request.seatType() != null) {
            seat.setSeatType(request.seatType());
        }
        if (request.status() != null) {
            seat.setStatus(request.status());
        }
        if (request.price() != null) {
            seat.setPrice(request.price());
        }
        return seat;
    }

    public SeatResponse toResponse(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getScreen().getId(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getSeatType(),
                seat.getStatus(),
                seat.getPrice());
    }
}

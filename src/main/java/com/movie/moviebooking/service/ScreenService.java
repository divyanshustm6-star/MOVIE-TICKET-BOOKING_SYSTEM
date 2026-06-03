package com.movie.moviebooking.service;

import com.movie.moviebooking.dto.ApiDtos.ScreenRequest;
import com.movie.moviebooking.dto.ApiDtos.ScreenResponse;
import com.movie.moviebooking.entity.Screen;
import com.movie.moviebooking.exception.BadRequestException;
import com.movie.moviebooking.exception.ResourceNotFoundException;
import com.movie.moviebooking.repository.MovieShowRepository;
import com.movie.moviebooking.repository.ScreenRepository;
import com.movie.moviebooking.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScreenService {
    private static final Logger log = LoggerFactory.getLogger(ScreenService.class);

    private final ScreenRepository screenRepository;
    private final MovieShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final TheaterService theaterService;

    public ScreenService(
            ScreenRepository screenRepository,
            MovieShowRepository showRepository,
            SeatRepository seatRepository,
            TheaterService theaterService) {
        this.screenRepository = screenRepository;
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.theaterService = theaterService;
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> findAll(Long theaterId) {
        List<Screen> screens = theaterId == null ? screenRepository.findAll() : screenRepository.findByTheaterId(theaterId);
        return screens.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ScreenResponse create(ScreenRequest request) {
        return toResponse(screenRepository.save(apply(new Screen(), request)));
    }

    @Transactional
    public ScreenResponse update(Long id, ScreenRequest request) {
        Screen screen = getScreen(id);
        screen.setUpdatedAt(Instant.now());
        return toResponse(screenRepository.save(apply(screen, request)));
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting Screen ID={}", id);
        List<com.movie.moviebooking.entity.MovieShow> shows = showRepository.findByScreenId(id);
        if (!shows.isEmpty()) {
            throw new BadRequestException("Cannot delete screen with scheduled shows");
        }
        var seats = seatRepository.findByScreenId(id);
        log.info("Deleting Seats Count={} for Screen ID={}", seats.size(), id);
        seatRepository.deleteAll(seats);
        screenRepository.delete(getScreen(id));
        boolean exists = screenRepository.existsById(id);
        log.info("Screen ID={} exists after delete? {}", id, exists);
    }

    public Screen getScreen(Long id) {
        return screenRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Screen not found"));
    }

    private Screen apply(Screen screen, ScreenRequest request) {
        screen.setTheater(theaterService.getTheater(request.theaterId()));
        screen.setName(request.name());
        screen.setTotalSeats(request.totalSeats());
        if (request.screenType() != null) {
            screen.setScreenType(request.screenType());
        }
        if (request.status() != null) {
            screen.setStatus(request.status());
        }
        return screen;
    }

    public ScreenResponse toResponse(Screen screen) {
        return new ScreenResponse(
                screen.getId(),
                screen.getTheater().getId(),
                screen.getName(),
                screen.getScreenType(),
                screen.getTotalSeats(),
                screen.getStatus());
    }
}

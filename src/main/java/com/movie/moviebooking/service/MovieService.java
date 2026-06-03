package com.movie.moviebooking.service;

import com.movie.moviebooking.dto.ApiDtos.MovieRequest;
import com.movie.moviebooking.dto.ApiDtos.MovieResponse;
import com.movie.moviebooking.entity.Movie;
import com.movie.moviebooking.exception.ResourceNotFoundException;
import com.movie.moviebooking.repository.MovieRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieService {
    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> findAll() {
        return movieRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MovieResponse findById(Long id) {
        return movieRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
    }

    @Transactional
    public MovieResponse create(MovieRequest request) {
        return toResponse(movieRepository.save(apply(new Movie(), request)));
    }

    @Transactional
    public MovieResponse update(Long id, MovieRequest request) {
        Movie movie = getMovie(id);
        movie.setUpdatedAt(Instant.now());
        return toResponse(movieRepository.save(apply(movie, request)));
    }

    @Transactional
    public void delete(Long id) {
        movieRepository.delete(getMovie(id));
    }

    public Movie getMovie(Long id) {
        return movieRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
    }

    private Movie apply(Movie movie, MovieRequest request) {
        movie.setTitle(request.title());
        movie.setDescription(request.description());
        movie.setLanguage(request.language());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setReleaseDate(request.releaseDate());
        movie.setCertification(request.certification());
        movie.setPosterUrl(request.posterUrl());
        movie.setTrailerUrl(request.trailerUrl());
        if (request.status() != null) {
            movie.setStatus(request.status());
        }
        movie.setGenres(request.genres() == null ? new HashSet<>() : new HashSet<>(request.genres()));
        return movie;
    }

    public MovieResponse toResponse(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getLanguage(),
                movie.getDurationMinutes(),
                movie.getReleaseDate(),
                movie.getCertification(),
                movie.getPosterUrl(),
                movie.getTrailerUrl(),
                movie.getStatus(),
                movie.getGenres());
    }

}

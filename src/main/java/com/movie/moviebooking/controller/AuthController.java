package com.movie.moviebooking.controller;

import com.movie.moviebooking.dto.ApiDtos.AuthResponse;
import com.movie.moviebooking.dto.ApiDtos.LoginRequest;
import com.movie.moviebooking.dto.ApiDtos.RegisterRequest;
import com.movie.moviebooking.dto.ApiDtos.UserResponse;
import com.movie.moviebooking.service.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        log.debug("AuthController.register - email={}", request.email());
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        log.debug("AuthController.login - email={}", request.email());
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(Principal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication token is required");
        }
        return authService.toUserResponse(authService.currentUser(principal.getName()));
    }
}

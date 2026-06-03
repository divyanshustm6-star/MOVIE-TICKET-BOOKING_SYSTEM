package com.movie.moviebooking.service;

import com.movie.moviebooking.dto.ApiDtos.AuthResponse;
import com.movie.moviebooking.dto.ApiDtos.LoginRequest;
import com.movie.moviebooking.dto.ApiDtos.RegisterRequest;
import com.movie.moviebooking.dto.ApiDtos.UserResponse;
import com.movie.moviebooking.entity.Role;
import com.movie.moviebooking.entity.RoleName;
import com.movie.moviebooking.entity.User;
import com.movie.moviebooking.exception.BadRequestException;
import com.movie.moviebooking.exception.ResourceNotFoundException;
import com.movie.moviebooking.repository.RoleRepository;
import com.movie.moviebooking.repository.UserRepository;
import com.movie.moviebooking.security.JwtService;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }
        if (request.phone() != null && !request.phone().isBlank() && userRepository.existsByPhone(request.phone())) {
            throw new BadRequestException("Phone is already registered");
        }
        Role role = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("User role not configured"));
        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.getRoles().add(role);
        User saved = userRepository.save(user);
        return authResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.debug("AuthService.login - attempt login for email={}", request.email());
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            log.warn("Authentication failed for email={}: {}", request.email(), ex.getMessage());
            throw ex;
        }
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setLastLoginAt(Instant.now());
        User saved = userRepository.save(user);
        log.debug("AuthService.login - authenticated and loaded user id={}, email={}", saved.getId(), saved.getEmail());
        return authResponse(saved);
    }

    public User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), roles(user));
    }

    private AuthResponse authResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                roles(user));
    }

    private Set<String> roles(User user) {
        return user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet());
    }
}

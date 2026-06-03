package com.movie.moviebooking.service;

import com.movie.moviebooking.dto.ApiDtos.UserRequest;
import com.movie.moviebooking.dto.ApiDtos.UserResponse;
import com.movie.moviebooking.entity.Role;
import com.movie.moviebooking.entity.RoleName;
import com.movie.moviebooking.entity.User;
import com.movie.moviebooking.exception.BadRequestException;
import com.movie.moviebooking.exception.ResourceNotFoundException;
import com.movie.moviebooking.repository.RoleRepository;
import com.movie.moviebooking.repository.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse findById(Long id) {
        return toResponse(getUser(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }
        User user = new User();
        apply(user, request);
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("Password is required");
        }
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = getUser(id);
        apply(user, request);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setUpdatedAt(Instant.now());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(getUser(id));
    }

    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void apply(User user, UserRequest request) {
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRoles(resolveRoles(request.roles()));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<String> requestedRoles = roleNames == null || roleNames.isEmpty()
                ? Set.of(RoleName.ROLE_USER.name())
                : roleNames;
        return requestedRoles.stream()
                .map(roleName -> roleRepository.findByName(toRoleName(roleName))
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName)))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private RoleName toRoleName(String roleName) {
        String normalized = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        return RoleName.valueOf(normalized.toUpperCase());
    }

    public UserResponse toResponse(User user) {
        Set<String> roles = user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet());
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), roles);
    }
}

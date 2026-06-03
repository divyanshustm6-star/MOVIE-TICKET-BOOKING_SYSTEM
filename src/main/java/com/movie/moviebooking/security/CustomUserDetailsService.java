package com.movie.moviebooking.security;

import com.movie.moviebooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("CustomUserDetailsService.loadUserByUsername - received username/email='{}'", username);
        String email = username == null ? null : username.trim();
        if (email == null || email.isBlank()) {
            log.warn("Empty username passed to loadUserByUsername");
            throw new UsernameNotFoundException("User not found");
        }

        return userRepository.findByEmail(email)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .map(user -> {
                    log.debug("UserRepository returned user with email={}", user.getEmail());
                    return org.springframework.security.core.userdetails.User.builder()
                            .username(user.getEmail())
                            .password(user.getPasswordHash())
                            .authorities(user.getRoles().stream()
                                    .map(role -> role.getName().name())
                                    .toArray(String[]::new))
                            .disabled(!"ACTIVE".equals(user.getAccountStatus().name()))
                            .build();
                })
                .orElseGet(() -> {
                    log.warn("User not found for email={}", email);
                    throw new UsernameNotFoundException("User not found");
                });
    }
}

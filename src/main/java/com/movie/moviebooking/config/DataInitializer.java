package com.movie.moviebooking.config;

import com.movie.moviebooking.entity.Role;
import com.movie.moviebooking.entity.RoleName;
import com.movie.moviebooking.repository.RoleRepository;
import com.movie.moviebooking.repository.UserRepository;
import com.movie.moviebooking.entity.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository, JdbcTemplate jdbcTemplate, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        migrateCustomerRole();
        createRole(RoleName.ROLE_ADMIN, "Can manage movies, theaters, screens, shows, bookings, and reports");
        createRole(RoleName.ROLE_USER, "Can browse shows, book seats, pay, and view booking history");
        createDefaultAdmin();
        // Ensure admin@gmail.com has a known password and ROLE_ADMIN assigned
        resetAdminPassword();
        createTemporaryAdmin();
        createOrUpdateDebugUserFromEnv();
    }

    private void createTemporaryAdmin() {
        final String tempEmail = "admin@example.com";
        final String tempPassword = "Admin123@";
        if (userRepository.existsByEmail(tempEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow(() -> new IllegalStateException("Admin role not found"));
        User admin = new User();
        admin.setFullName("Temporary Admin");
        admin.setEmail(tempEmail);
        admin.setPasswordHash(passwordEncoder.encode(tempPassword));
        admin.setEmailVerified(true);
        admin.getRoles().add(adminRole);
        userRepository.save(admin);
    }

    private void createOrUpdateDebugUserFromEnv() {
        String debugEmail = System.getenv("DEBUG_USER_EMAIL");
        String debugPassword = System.getenv("DEBUG_USER_PASSWORD");
        if (debugEmail == null || debugEmail.isBlank() || debugPassword == null || debugPassword.isBlank()) {
            return; // nothing to do
        }
        try {
            Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow(() -> new IllegalStateException("User role not found"));
            User existing = userRepository.findByEmail(debugEmail).orElse(null);
            if (existing == null) {
                User u = new User();
                u.setFullName("Debug User");
                u.setEmail(debugEmail);
                u.setPasswordHash(passwordEncoder.encode(debugPassword));
                u.setEmailVerified(true);
                u.setAccountStatus(com.movie.moviebooking.entity.AccountStatus.ACTIVE);
                u.getRoles().add(userRole);
                userRepository.save(u);
                System.out.println("[DataInitializer] Created debug user: " + debugEmail);
            } else {
                existing.setPasswordHash(passwordEncoder.encode(debugPassword));
                existing.setAccountStatus(com.movie.moviebooking.entity.AccountStatus.ACTIVE);
                existing.getRoles().add(userRole);
                userRepository.save(existing);
                System.out.println("[DataInitializer] Updated debug user password: " + debugEmail);
            }
        } catch (Exception ex) {
            System.err.println("[DataInitializer] Failed to create/update debug user: " + ex.getMessage());
        }
    }

    private void migrateCustomerRole() {
        Integer customerRoleCount = jdbcTemplate.queryForObject(
                "select count(*) from roles where name = 'ROLE_CUSTOMER'",
                Integer.class);
        if (customerRoleCount == null || customerRoleCount == 0) {
            return;
        }

        Integer userRoleCount = jdbcTemplate.queryForObject(
                "select count(*) from roles where name = 'ROLE_USER'",
                Integer.class);
        if (userRoleCount != null && userRoleCount > 0) {
            jdbcTemplate.update("""
                    delete ur from user_roles ur
                    join roles customer_role on customer_role.id = ur.role_id and customer_role.name = 'ROLE_CUSTOMER'
                    join roles user_role on user_role.name = 'ROLE_USER'
                    join user_roles existing_user_role
                        on existing_user_role.user_id = ur.user_id
                        and existing_user_role.role_id = user_role.id
                    """);
            jdbcTemplate.update("""
                    update user_roles ur
                    join roles customer_role on customer_role.id = ur.role_id and customer_role.name = 'ROLE_CUSTOMER'
                    join roles user_role on user_role.name = 'ROLE_USER'
                    set ur.role_id = user_role.id
                    """);
            jdbcTemplate.update("delete from roles where name = 'ROLE_CUSTOMER'");
            return;
        }

        jdbcTemplate.update("update roles set name = 'ROLE_USER' where name = 'ROLE_CUSTOMER'");
    }

    private void createRole(RoleName roleName, String description) {
        roleRepository.findByName(roleName).orElseGet(() -> {
            Role role = new Role();
            role.setName(roleName);
            role.setDescription(description);
            return roleRepository.save(role);
        });
    }

    private void createDefaultAdmin() {
        final String adminEmail = "admin@gmail.com";
        final String adminPassword = "Admin@123";
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow(() -> new IllegalStateException("Admin role not found"));
        User admin = new User();
        admin.setFullName("Administrator");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setEmailVerified(true);
        admin.getRoles().add(adminRole);
        userRepository.save(admin);
    }

    private void resetAdminPassword() {
        final String adminEmail = "admin@gmail.com";
        final String adminPassword = "Admin@123";
        try {
            userRepository.findByEmail(adminEmail).ifPresent(user -> {
                user.setPasswordHash(passwordEncoder.encode(adminPassword));
                user.setAccountStatus(com.movie.moviebooking.entity.AccountStatus.ACTIVE);
                Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                        .orElseThrow(() -> new IllegalStateException("Admin role not found"));
                user.getRoles().add(adminRole);
                userRepository.save(user);
                System.out.println("Admin password reset: " + adminEmail);
            });
        } catch (Exception ex) {
            System.err.println("Failed to reset admin password: " + ex.getMessage());
        }
    }
}


package com.movie.moviebooking.repository;

import com.movie.moviebooking.entity.Role;
import com.movie.moviebooking.entity.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}

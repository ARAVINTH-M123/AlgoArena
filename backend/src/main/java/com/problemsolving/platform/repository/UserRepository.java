package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.User;
import com.problemsolving.platform.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    List<User> findAllByEmailIgnoreCase(String email);
    List<User> findAllByOrderByCreatedAtDesc();
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
}

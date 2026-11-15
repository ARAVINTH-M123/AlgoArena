package com.problemsolving.platform.service;

import com.problemsolving.platform.api.dto.admin.AdminSessionResponse;
import com.problemsolving.platform.api.dto.admin.AdminUserSummary;
import com.problemsolving.platform.domain.entity.User;
import com.problemsolving.platform.domain.enums.Role;
import com.problemsolving.platform.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Backing service for the management console (/api/admin/**).
 *
 * Every method here is only reachable by callers holding ROLE_ADMIN (enforced by
 * SecurityConfig on /api/admin/** plus @PreAuthorize checks). Future admin modules
 * (user management actions, submissions, analytics, announcements, settings, audit
 * logs) can be added as additional methods/endpoints under the same authorization
 * boundary; contest management is intentionally out of scope for now.
 */
@Service
public class AdminService {

    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Identity of the caller; used by the console to confirm the session is still a valid admin. */
    public AdminSessionResponse currentAdmin(String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin session"));

        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }

        return new AdminSessionResponse(user.getUsername(), user.getEmail(), user.getRole().name());
    }

    /** All platform accounts, newest first. No password hashes are ever returned. */
    public List<AdminUserSummary> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(user -> new AdminUserSummary(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole() == null ? Role.USER.name() : user.getRole().name(),
                        user.getRating(),
                        user.getStreakDays(),
                        user.getCreatedAt() == null ? null : user.getCreatedAt().format(CREATED_AT_FORMAT)))
                .toList();
    }
}

package com.problemsolving.platform.config;

import com.problemsolving.platform.domain.entity.User;
import com.problemsolving.platform.domain.enums.Role;
import com.problemsolving.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.regex.Pattern;

/**
 * Creates the initial admin account from environment variables on first startup only.
 *
 * Security rules enforced here:
 * - Credentials are never hardcoded; they come from ADMIN_USERNAME / ADMIN_EMAIL / ADMIN_PASSWORD
 *   (overridable through the app.admin.bootstrap.* properties).
 * - The password is stored as a BCrypt hash through the shared PasswordEncoder.
 * - Nothing is overwritten: if any ADMIN account already exists the bootstrap is a no-op.
 * - There is no public endpoint behind this; once setup succeeded the mechanism is inert for
 *   the environment and can additionally be switched off with ADMIN_BOOTSTRAP_ENABLED=false.
 */
@Configuration
public class AdminBootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapConfig.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    @Value("${app.admin.bootstrap.enabled:true}")
    private boolean enabled;

    @Value("${app.admin.bootstrap.username:}")
    private String username;

    @Value("${app.admin.bootstrap.email:}")
    private String email;

    @Value("${app.admin.bootstrap.password:}")
    private String password;

    @Bean
    CommandLineRunner ensureInitialAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!enabled) {
                log.info("Admin bootstrap is disabled (app.admin.bootstrap.enabled=false).");
                return;
            }

            if (userRepository.existsByRole(Role.ADMIN)) {
                log.info("An ADMIN account already exists; initial admin bootstrap is disabled.");
                return;
            }

            if (isBlank(username) || isBlank(email) || isBlank(password)) {
                log.warn("Initial admin bootstrap skipped: set the ADMIN_USERNAME, ADMIN_EMAIL and "
                        + "ADMIN_PASSWORD environment variables to create the first admin account.");
                return;
            }

            String normalizedUsername = username.trim();
            String normalizedEmail = email.trim().toLowerCase();

            if (normalizedUsername.length() < 3 || normalizedUsername.length() > 50) {
                log.error("Admin bootstrap aborted: username must be between 3 and 50 characters.");
                return;
            }
            if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
                log.error("Admin bootstrap aborted: ADMIN_EMAIL is not a valid email address.");
                return;
            }
            if (password.length() < MIN_PASSWORD_LENGTH) {
                log.error("Admin bootstrap aborted: ADMIN_PASSWORD must be at least {} characters.", MIN_PASSWORD_LENGTH);
                return;
            }
            if (userRepository.existsByUsername(normalizedUsername)
                    || userRepository.existsByEmail(normalizedEmail)) {
                log.error("Admin bootstrap aborted: the configured admin username or email is already taken. "
                        + "Existing accounts are never overwritten.");
                return;
            }

            User admin = new User();
            admin.setUsername(normalizedUsername);
            admin.setEmail(normalizedEmail);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            log.info("Initial admin account '{}' created from environment configuration. "
                    + "The bootstrap mechanism is now inert for this environment.", normalizedUsername);
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

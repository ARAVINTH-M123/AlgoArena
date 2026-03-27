package com.problemsolving.platform.config;

import com.problemsolving.platform.domain.entity.User;
import com.problemsolving.platform.domain.enums.Role;
import com.problemsolving.platform.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapConfig {

    private static final String ADMIN_EMAIL = "algoarenaadmin@gmail.com";
    private static final String ADMIN_PASSWORD = "123456";
    private static final String ADMIN_USERNAME = "algoarenaadmin";

    @Bean
    CommandLineRunner ensureDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseGet(User::new);
            admin.setUsername(ADMIN_USERNAME);
            admin.setEmail(ADMIN_EMAIL);
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        };
    }
}

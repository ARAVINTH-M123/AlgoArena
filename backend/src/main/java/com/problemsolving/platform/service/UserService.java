package com.problemsolving.platform.service;

import com.problemsolving.platform.api.dto.auth.AuthResponse;
import com.problemsolving.platform.api.dto.user.ProfileDto;
import com.problemsolving.platform.domain.entity.User;
import com.problemsolving.platform.domain.enums.Role;
import com.problemsolving.platform.domain.enums.Verdict;
import com.problemsolving.platform.repository.SubmissionRepository;
import com.problemsolving.platform.repository.UserRepository;
import com.problemsolving.platform.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       SubmissionRepository submissionRepository,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.jwtService = jwtService;
    }

    public List<String> allUsernames() {
        return userRepository.findAll().stream().map(User::getUsername).sorted().toList();
    }

    public ProfileDto profile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<com.problemsolving.platform.domain.entity.Submission> submissions = submissionRepository.findByUserUsernameOrderBySubmittedAtDesc(username);

        long accepted = submissions.stream().filter(s -> s.getVerdict() == Verdict.ACCEPTED).count();
        long solved = submissions.stream()
                .filter(s -> s.getVerdict() == Verdict.ACCEPTED)
                .map(s -> s.getProblem().getId())
                .distinct()
                .count();

        return new ProfileDto(user.getUsername(), user.getRating(), user.getStreakDays(), solved, accepted);
    }

    public AuthResponse updateUsername(String currentUsername, String nextUsername) {
        String normalizedUsername = nextUsername == null ? "" : nextUsername.trim();
        if (normalizedUsername.length() < 3 || normalizedUsername.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must be between 3 and 50 characters");
        }

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getUsername().equals(normalizedUsername) && userRepository.existsByUsername(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }

        user.setUsername(normalizedUsername);
        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }

        String token = jwtService.generateToken(saved.getUsername());
        String role = saved.getRole() == null ? Role.USER.name() : saved.getRole().name();
        return new AuthResponse(token, saved.getUsername(), role);
    }
}

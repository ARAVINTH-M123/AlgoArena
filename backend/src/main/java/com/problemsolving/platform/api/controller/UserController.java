package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.auth.AuthResponse;
import com.problemsolving.platform.api.dto.user.ProfileDto;
import com.problemsolving.platform.api.dto.user.UpdateUsernameRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import com.problemsolving.platform.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<String> allUsers() {
        return userService.allUsernames();
    }

    @GetMapping("/{username}")
    public ProfileDto profile(@PathVariable String username) {
        return userService.profile(username);
    }

    @PutMapping("/me/username")
    public AuthResponse updateMyUsername(Authentication authentication,
                                         @Valid @RequestBody UpdateUsernameRequest request) {
        return userService.updateUsername(authentication.getName(), request.getUsername());
    }
}

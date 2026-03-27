package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.auth.AdminLoginRequest;
import com.problemsolving.platform.api.dto.auth.AuthResponse;
import com.problemsolving.platform.api.dto.auth.LoginRequest;
import com.problemsolving.platform.api.dto.auth.RegisterRequest;
import com.problemsolving.platform.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/admin/login")
    public AuthResponse adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return authService.adminLogin(request);
    }
}

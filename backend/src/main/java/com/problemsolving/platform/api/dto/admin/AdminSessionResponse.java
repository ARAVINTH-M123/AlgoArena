package com.problemsolving.platform.api.dto.admin;

/**
 * Identity of the currently authenticated admin, used by the management console to
 * verify its session against the backend (not just local storage).
 * Never contains credentials or password hashes.
 */
public class AdminSessionResponse {

    private final String username;
    private final String email;
    private final String role;

    public AdminSessionResponse(String username, String email, String role) {
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}

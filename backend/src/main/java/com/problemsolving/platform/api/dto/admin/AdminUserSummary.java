package com.problemsolving.platform.api.dto.admin;

/**
 * Admin-facing summary of a platform account.
 * Deliberately excludes the password hash and any credentials.
 */
public class AdminUserSummary {

    private final Long id;
    private final String username;
    private final String email;
    private final String role;
    private final Integer rating;
    private final Integer streakDays;
    private final String createdAt;

    public AdminUserSummary(Long id, String username, String email, String role,
                            Integer rating, Integer streakDays, String createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.rating = rating;
        this.streakDays = streakDays;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
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

    public Integer getRating() {
        return rating;
    }

    public Integer getStreakDays() {
        return streakDays;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}

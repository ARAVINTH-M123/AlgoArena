package com.problemsolving.platform.api.dto.user;

public class ProfileDto {

    private final String username;
    private final Integer rating;
    private final Integer streakDays;
    private final Long solvedProblems;
    private final Long acceptedSubmissions;

    public ProfileDto(String username, Integer rating, Integer streakDays, Long solvedProblems, Long acceptedSubmissions) {
        this.username = username;
        this.rating = rating;
        this.streakDays = streakDays;
        this.solvedProblems = solvedProblems;
        this.acceptedSubmissions = acceptedSubmissions;
    }

    public String getUsername() {
        return username;
    }

    public Integer getRating() {
        return rating;
    }

    public Integer getStreakDays() {
        return streakDays;
    }

    public Long getSolvedProblems() {
        return solvedProblems;
    }

    public Long getAcceptedSubmissions() {
        return acceptedSubmissions;
    }
}

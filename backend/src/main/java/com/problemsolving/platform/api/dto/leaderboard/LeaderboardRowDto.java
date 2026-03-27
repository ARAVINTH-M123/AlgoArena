package com.problemsolving.platform.api.dto.leaderboard;

public class LeaderboardRowDto {

    private final Long userId;
    private final String username;
    private final Integer score;
    private final Integer timePenalty;

    public LeaderboardRowDto(Long userId, String username, Integer score, Integer timePenalty) {
        this.userId = userId;
        this.username = username;
        this.score = score;
        this.timePenalty = timePenalty;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Integer getScore() {
        return score;
    }

    public Integer getTimePenalty() {
        return timePenalty;
    }
}

package com.problemsolving.platform.api.dto.leaderboard;

public class LeaderboardRowDto {

    private final Long userId;
    private final String username;
    private final Integer score;
    private final Integer timePenalty;
    private final String nationName;
    private final String nationRankName;
    private final Integer nationRankIndex;
    private final Integer nationTier;

    public LeaderboardRowDto(Long userId, String username, Integer score, Integer timePenalty,
                             String nationName, String nationRankName, Integer nationRankIndex, Integer nationTier) {
        this.userId = userId;
        this.username = username;
        this.score = score;
        this.timePenalty = timePenalty;
        this.nationName = nationName;
        this.nationRankName = nationRankName;
        this.nationRankIndex = nationRankIndex;
        this.nationTier = nationTier;
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

    public String getNationName() {
        return nationName;
    }

    /** Rank name of the user's nation, for example "Kernel". */
    public String getNationRankName() {
        return nationRankName;
    }

    /** 0 (Null) to 9 (Singularity); used to color the badge. */
    public Integer getNationRankIndex() {
        return nationRankIndex;
    }

    /** 1 to 3, shown as I, II, III. */
    public Integer getNationTier() {
        return nationTier;
    }
}

package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.leaderboard.LeaderboardRowDto;
import com.problemsolving.platform.service.LeaderboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public List<LeaderboardRowDto> global() {
        return leaderboardService.global();
    }
}

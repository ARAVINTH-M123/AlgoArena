package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.nation.NationDto;
import com.problemsolving.platform.api.dto.nation.UpdateNationProfileRequest;
import com.problemsolving.platform.service.nation.NationProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/nation")
public class NationController {

    private final NationProfileService nationProfileService;

    public NationController(NationProfileService nationProfileService) {
        this.nationProfileService = nationProfileService;
    }

    @GetMapping("/me")
    public NationDto mine(Authentication authentication) {
        return nationProfileService.view(authentication.getName(), true, Instant.now());
    }

    /** Public, like profiles: anyone can look at a nation. The owner additionally gets cooldown info. */
    @GetMapping("/{username}")
    public NationDto byUsername(Authentication authentication, @PathVariable String username) {
        boolean owner = authentication != null
                && authentication.isAuthenticated()
                && username.equals(authentication.getName());
        return nationProfileService.view(username, owner, Instant.now());
    }

    @PutMapping("/me/profile")
    public NationDto updateProfile(Authentication authentication, @RequestBody UpdateNationProfileRequest request) {
        return nationProfileService.updateProfile(authentication.getName(), request, Instant.now());
    }
}

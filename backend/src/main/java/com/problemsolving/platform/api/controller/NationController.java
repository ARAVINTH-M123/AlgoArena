package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.nation.NationViewDto;
import com.problemsolving.platform.service.NationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nation")
public class NationController {

    private final NationService nationService;

    public NationController(NationService nationService) {
        this.nationService = nationService;
    }

    @GetMapping("/{username}")
    public NationViewDto byUsername(@PathVariable String username) {
        return nationService.getByUsername(username);
    }

    @GetMapping("/me")
    public NationViewDto me(Authentication authentication) {
        return nationService.getForCurrentUser(authentication.getName());
    }
}

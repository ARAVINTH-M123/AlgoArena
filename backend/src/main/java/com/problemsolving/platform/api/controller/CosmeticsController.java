package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.cosmetics.CosmeticItemDto;
import com.problemsolving.platform.api.dto.cosmetics.CosmeticsDto;
import com.problemsolving.platform.api.dto.cosmetics.EquipRequest;
import com.problemsolving.platform.service.cosmetics.CosmeticService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/cosmetics")
public class CosmeticsController {

    private final CosmeticService cosmeticService;

    public CosmeticsController(CosmeticService cosmeticService) {
        this.cosmeticService = cosmeticService;
    }

    /** Public: every cosmetic that exists, for browsing. */
    @GetMapping("/catalog")
    public List<CosmeticItemDto> catalog() {
        return cosmeticService.catalog();
    }

    /** What the signed-in user owns and uses. */
    @GetMapping("/me")
    public CosmeticsDto mine(Authentication authentication) {
        return cosmeticService.mine(authentication.getName());
    }

    /** Choose a cosmetic; key "auto" (or empty) goes back to the automatic one of the current rank. */
    @PostMapping("/equip")
    public CosmeticsDto equip(Authentication authentication, @RequestBody EquipRequest request) {
        return cosmeticService.equip(authentication.getName(), request, Instant.now());
    }
}

package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.cosmetics.CacheOpenResultDto;
import com.problemsolving.platform.api.dto.cosmetics.CacheStateDto;
import com.problemsolving.platform.api.dto.cosmetics.CraftRequest;
import com.problemsolving.platform.api.dto.cosmetics.CraftResultDto;
import com.problemsolving.platform.api.dto.cosmetics.CosmeticItemDto;
import com.problemsolving.platform.api.dto.cosmetics.CosmeticsDto;
import com.problemsolving.platform.api.dto.cosmetics.EquipRequest;
import com.problemsolving.platform.service.cosmetics.CacheService;
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
    private final CacheService cacheService;

    public CosmeticsController(CosmeticService cosmeticService, CacheService cacheService) {
        this.cosmeticService = cosmeticService;
        this.cacheService = cacheService;
    }

    /** Public: every cosmetic that exists, for browsing. */
    @GetMapping("/catalog")
    public List<CosmeticItemDto> catalog() {
        return cosmeticService.catalog();
    }

    /** What the signed-in user owns and uses (also the "collection": owned flags per item). */
    @GetMapping("/me")
    public CosmeticsDto mine(Authentication authentication) {
        return cosmeticService.mine(authentication.getName());
    }

    /** Choose a cosmetic; key "auto" (or empty) goes back to the automatic one of the current rank (or none). */
    @PostMapping("/equip")
    public CosmeticsDto equip(Authentication authentication, @RequestBody EquipRequest request) {
        return cosmeticService.equip(authentication.getName(), request, Instant.now());
    }

    /** Cache and shard state plus the exact odds (login). */
    @GetMapping("/caches")
    public CacheStateDto caches(Authentication authentication) {
        return cacheService.state(authentication.getName());
    }

    /** Opens one Mystery Cache and reveals what it contained (login). */
    @PostMapping("/caches/open")
    public CacheOpenResultDto openCache(Authentication authentication) {
        return cacheService.open(authentication.getName(), Instant.now());
    }

    /** Crafts one cache-pool cosmetic for shards; body: { key } (login). */
    @PostMapping("/craft")
    public CraftResultDto craft(Authentication authentication, @RequestBody CraftRequest request) {
        return cacheService.craft(authentication.getName(), request.getKey(), Instant.now());
    }
}

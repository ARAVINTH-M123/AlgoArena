package com.problemsolving.platform.service.nation;

import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.Nation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NationRankLookupTest {

    private static final long M = NationProperties.MICRO;

    private InMemoryNationStore store;
    private NationRankLookup lookup;

    @BeforeEach
    void setUp() {
        store = new InMemoryNationStore();
        lookup = new NationRankLookup(store, new NationProperties());
    }

    @Test
    void usersWithoutANationAreRankNullWithTheDefaultName() {
        Map<Long, String> users = new LinkedHashMap<>();
        users.put(1L, "alice");
        NationRankLookup.Info info = lookup.describe(users).get(1L);
        assertEquals("alice's Land", info.nationName());
        assertEquals(0, info.rankIndex());
        assertEquals("Null", info.rankName());
        assertEquals(1, info.tier());
        assertEquals(0, store.nationCount());          // looking things up creates nothing
    }

    @Test
    void existingNationsShowTheirNameRankAndTier() {
        Nation bob = store.lockOrCreateNation(2L);
        bob.setName("Stonebridge");
        bob.setRankIndex(4);                            // Kernel: 27,000 .. 51,000 XP, tiers of 8,000
        bob.setXp(44_000 * M);                          // third part of the range
        Nation carol = store.lockOrCreateNation(3L);
        carol.setName("Skyhaven");
        carol.setRankIndex(9);
        carol.setXp(291_000 * M);                       // Singularity II

        Map<Long, String> users = new LinkedHashMap<>();
        users.put(1L, "alice");                         // no nation
        users.put(2L, "bob");
        users.put(3L, "carol");
        Map<Long, NationRankLookup.Info> result = lookup.describe(users);

        assertEquals("Stonebridge", result.get(2L).nationName());
        assertEquals("Kernel", result.get(2L).rankName());
        assertEquals(4, result.get(2L).rankIndex());
        assertEquals(3, result.get(2L).tier());
        assertEquals("Skyhaven", result.get(3L).nationName());
        assertEquals("Singularity", result.get(3L).rankName());
        assertEquals(2, result.get(3L).tier());
        assertEquals("alice's Land", result.get(1L).nationName());
    }

    @Test
    void anEmptyUserListGivesAnEmptyResult() {
        assertEquals(0, lookup.describe(new LinkedHashMap<>()).size());
    }
}

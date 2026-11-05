package com.problemsolving.platform.service.nation;

import com.problemsolving.platform.api.dto.nation.NationResultDto;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.Nation;
import com.problemsolving.platform.domain.entity.NationTrackState;
import com.problemsolving.platform.domain.enums.Difficulty;
import com.problemsolving.platform.domain.enums.NationTrack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NationServiceTest {

    private static final long M = NationProperties.MICRO;
    private static final Long USER = 1L;
    private static final Instant T0 = Instant.parse("2026-03-01T12:00:00Z");

    private InMemoryNationStore store;
    private NationProperties props;
    private NationService service;
    private long nextProblem;

    @BeforeEach
    void setUp() {
        store = new InMemoryNationStore();
        props = new NationProperties();
        service = new NationService(store, props);
        nextProblem = 1;
    }

    // ---------------------------------------------------------------- helpers

    private NationResultDto solve(Difficulty difficulty, Instant at) {
        return service.awardForAcceptedSolve(USER, nextProblem++, difficulty, at);
    }

    private NationResultDto solveOnDay(Difficulty difficulty, int dayOffset) {
        return solve(difficulty, T0.plus(Duration.ofDays(dayOffset)));
    }

    private Nation nation() {
        return store.lockOrCreateNation(USER);
    }

    private NationTrackState state(NationTrack track) {
        return store.getOrCreateTrackState(nation().getId(), track, NationCatalog.initialItemKey(track));
    }

    /** Puts the nation into a known mid-game state. */
    private Nation prepare(int rank, long xpPoints, int mediums, int hards, boolean gateDone) {
        Nation n = nation();
        n.setRankIndex(rank);
        n.setXp(xpPoints * M);
        n.setMediumSolves(mediums);
        n.setHardSolves(hards);
        int era = NationRules.contentEra(rank);
        for (NationTrack track : NationTrack.values()) {
            NationTrackState s = state(track);
            s.setEraCursor(0);
            if (track == NationTrack.LANDMARKS) {
                s.setGateDone(gateDone);
                s.setCurrentItemKey(gateDone
                        ? NationCatalog.items(era, track).get(0).key()
                        : NationCatalog.gate(rank).key());
            } else {
                s.setCurrentItemKey(NationCatalog.items(era, track).get(0).key());
            }
        }
        return n;
    }

    private long countCompleted(NationResultDto result, NationTrack track) {
        return result.itemsCompleted().stream().filter(item -> item.track().equals(track.name())).count();
    }

    // ------------------------------------------------------------ first solve

    @Test
    void firstSolveAwardsXpAndTrackPoints() {
        NationResultDto r = solve(Difficulty.EASY, T0);
        assertEquals("FIRST", r.mode());
        assertEquals(null, r.reason());
        assertEquals(10 * M, r.xpGained());
        assertEquals(10 * M, r.basicsPoints());
        assertEquals(4 * M, r.structuresPoints());
        assertEquals(1 * M, r.landmarksPoints());
        assertEquals("Null", r.rankName());
        assertEquals(1, r.tier());
        assertEquals(10 * M, r.nationXp());
        assertEquals("Grass patch", r.tracks().get(0).itemName());
        assertEquals(0, r.tracks().get(0).stage());
    }

    @Test
    void trackPointsDependOnDifficulty() {
        NationResultDto medium = solve(Difficulty.MEDIUM, T0);
        assertEquals(30 * M, medium.xpGained());
        assertEquals(5 * M, medium.basicsPoints());
        assertEquals(10 * M, medium.structuresPoints());
        assertEquals(4 * M, medium.landmarksPoints());

        NationResultDto hard = solve(Difficulty.HARD, T0);
        assertEquals(70 * M, hard.xpGained());
        assertEquals(3 * M, hard.basicsPoints());
        assertEquals(8 * M, hard.structuresPoints());
        assertEquals(10 * M, hard.landmarksPoints());

        assertEquals(1, nation().getMediumSolves());
        assertEquals(1, nation().getHardSolves());
    }

    // ------------------------------------------------- idempotency / re-solve

    @Test
    void duplicateInsideTwentyFourHoursGivesNothing() {
        service.awardForAcceptedSolve(USER, 77L, Difficulty.EASY, T0);
        NationResultDto again = service.awardForAcceptedSolve(USER, 77L, Difficulty.EASY, T0.plus(Duration.ofHours(23)));
        assertEquals("NO_OP", again.mode());
        assertEquals("ALREADY_COUNTED", again.reason());
        assertEquals(10 * M, nation().getXp());
    }

    @Test
    void resolveAfterTwentyFourHoursGivesExactlyOneTenThousandth() {
        service.awardForAcceptedSolve(USER, 77L, Difficulty.EASY, T0);
        NationResultDto r = service.awardForAcceptedSolve(USER, 77L, Difficulty.EASY, T0.plus(Duration.ofHours(24)));
        assertEquals("RESOLVE", r.mode());
        assertEquals(1_000L, r.xpGained());
        assertEquals(1_000L, r.basicsPoints());
        assertEquals(400L, r.structuresPoints());
        assertEquals(100L, r.landmarksPoints());
        assertEquals(10 * M + 1_000L, nation().getXp());
        // a re-solve touches no solve counters and adds no active day
        assertEquals(1, nation().getEasySolves());
        assertEquals(1L, store.countActiveDays(nation().getId()));
        // the 24h window restarts from the re-solve
        NationResultDto third = service.awardForAcceptedSolve(USER, 77L, Difficulty.EASY, T0.plus(Duration.ofHours(30)));
        assertEquals("NO_OP", third.mode());
    }

    @Test
    void awardingCanBeSwitchedOff() {
        props.setAwardEnabled(false);
        NationResultDto r = solve(Difficulty.HARD, T0);
        assertEquals("NO_OP", r.mode());
        assertEquals("AWARDS_DISABLED", r.reason());
        assertEquals(0, store.nationCount());
    }

    // ------------------------------------------------------------ item building

    @Test
    void oneSolveNeverCompletesMoreThanOneItemPerTrackAtNormalSettings() {
        for (int i = 0; i < 200; i++) {
            NationResultDto r = solve(Difficulty.HARD, T0);
            for (NationTrack track : NationTrack.values()) {
                assertTrue(countCompleted(r, track) <= 1, "track " + track + " completed >1 item on solve " + i);
            }
        }
    }

    @Test
    void overflowCarriesToTheNextItem() {
        props.setXpMultiplier(7);
        NationResultDto r = solve(Difficulty.EASY, T0);
        assertEquals(70 * M, r.basicsPoints());
        assertEquals(1L, countCompleted(r, NationTrack.BASICS));
        assertEquals("e1.grass-patch", r.itemsCompleted().get(0).key());
        assertEquals(20 * M, state(NationTrack.BASICS).getProgress());
        assertEquals("e1.tree", state(NationTrack.BASICS).getCurrentItemKey());
    }

    @Test
    void severalItemsCanCompleteInOneAwardWhenMultiplierIsRaised() {
        props.setXpMultiplier(100);
        NationResultDto r = solve(Difficulty.HARD, T0);
        assertEquals(6L, countCompleted(r, NationTrack.BASICS));
        assertEquals(10L, countCompleted(r, NationTrack.STRUCTURES));
        assertEquals(10L, countCompleted(r, NationTrack.LANDMARKS));
        // gate landmark first, then bonus landmarks
        assertEquals("e1.watchtower", r.itemsCompleted().stream()
                .filter(item -> item.track().equals("LANDMARKS")).findFirst().get().key());
        // watchtower completed -> Init; Boot is blocked because Town Hall (gate) is not built yet
        assertEquals("Init", r.rankName());
        assertEquals(1, r.rankUps().size());
        assertEquals("e1.town-hall", state(NationTrack.LANDMARKS).getCurrentItemKey());
    }

    // ----------------------------------------------------------------- ranks

    @Test
    void rankUpNeedsEnoughXpEvenWhenGateLandmarkIsDone() {
        NationResultDto last = null;
        for (int i = 1; i <= 12; i++) {
            last = solve(Difficulty.HARD, T0);   // 12 x 70 = 840 XP (< 900)
        }
        assertTrue(state(NationTrack.LANDMARKS).isGateDone());   // 12 x 10 landmark points built Watchtower
        assertEquals("Null", last.rankName());
        NationResultDto thirteenth = solve(Difficulty.HARD, T0);  // 910 XP
        assertEquals("Init", thirteenth.rankName());
        assertEquals("Init", thirteenth.rankUps().get(0));
        assertEquals(1, store.rankEventCount());
    }

    @Test
    void aRankUpReportsTheCosmeticsItUnlocked() {
        NationResultDto last = null;
        for (int i = 1; i <= 12; i++) {
            last = solve(Difficulty.HARD, T0);
            assertEquals(0, last.unlocked().size());          // nothing unlocked without a rank-up
        }
        NationResultDto rankUp = solve(Difficulty.HARD, T0);   // the 13th hard solve reaches Init
        assertEquals("Init", rankUp.rankName());
        assertEquals(2, rankUp.unlocked().size());
        assertEquals("THEME", rankUp.unlocked().get(0).category());
        assertEquals("theme.ember", rankUp.unlocked().get(0).key());
        assertEquals("Ember", rankUp.unlocked().get(0).name());
        assertEquals("border.ember-glow", rankUp.unlocked().get(1).key());
    }

    @Test
    void gateLandmarkAloneBlocksRankUp() {
        NationResultDto r = null;
        for (int i = 0; i < 99; i++) {
            r = solveOnDay(Difficulty.EASY, i);   // one easy a day: no daily halving
        }
        assertEquals(990 * M, nation().getXp());   // XP requirement (900) is long satisfied
        assertEquals("Null", r.rankName());       // ...but only 99 landmark points: Watchtower not finished
        assertFalse(state(NationTrack.LANDMARKS).isGateDone());
        NationResultDto hundredth = solveOnDay(Difficulty.EASY, 99);
        assertEquals("Init", hundredth.rankName());
    }

    @Test
    void gateIsBuiltBeforeBonusLandmarksAndProgressCarriesAcrossRankUp() {
        for (int i = 1; i <= 10; i++) {
            solve(Difficulty.HARD, T0);
        }
        assertTrue(state(NationTrack.LANDMARKS).isGateDone());
        assertEquals("e1.great-barn", state(NationTrack.LANDMARKS).getCurrentItemKey());
        for (int i = 11; i <= 13; i++) {
            solve(Difficulty.HARD, T0);
        }
        assertEquals(1, nation().getRankIndex());
        assertFalse(state(NationTrack.LANDMARKS).isGateDone());
        assertEquals("e1.town-hall", state(NationTrack.LANDMARKS).getCurrentItemKey());
        assertEquals(30 * M, state(NationTrack.LANDMARKS).getProgress());   // 3 solves of progress carried over
    }

    @Test
    void missingMediumsBlockKernelUntilTheyAreMet() {
        prepare(3, 30_000, 0, 0, true);                 // Root, XP above the Kernel threshold
        assertEquals("Root", solve(Difficulty.EASY, T0).rankName());
        nation().setMediumSolves(100);
        assertEquals("Kernel", solveOnDay(Difficulty.EASY, 1).rankName());
    }

    @Test
    void missingHardsBlockQuantumUntilTheyAreMet() {
        Nation n = prepare(7, 210_000, 1_800, 599, true);   // Core
        store.addActiveDays(n.getId(), 400, LocalDate.of(2024, 1, 1));
        assertEquals("Core", solve(Difficulty.EASY, T0).rankName());
        n.setHardSolves(600);
        assertEquals("Quantum", solveOnDay(Difficulty.EASY, 1).rankName());
    }

    @Test
    void missingActiveDaysBlockCoreUntilTheyAreMet() {
        Nation n = prepare(6, 140_000, 900, 50, true);   // Cloud
        store.addActiveDays(n.getId(), 198, LocalDate.of(2024, 1, 1));
        assertEquals("Cloud", solve(Difficulty.EASY, T0).rankName());   // 199 active days
        assertEquals("Core", solveOnDay(Difficulty.EASY, 1).rankName()); // 200 active days
    }

    @Test
    void newContentEraRestartsBasicsAndStructuresQueues() {
        prepare(2, 11_990, 0, 0, true);                  // Boot, 10 XP short of Root
        NationResultDto r = solve(Difficulty.EASY, T0);
        assertEquals("Root", r.rankName());
        assertEquals("e2.cobblestone-road", state(NationTrack.BASICS).getCurrentItemKey());
        assertEquals("e2.wooden-house", state(NationTrack.STRUCTURES).getCurrentItemKey());
        assertEquals("e2.windmill", state(NationTrack.LANDMARKS).getCurrentItemKey());
        assertEquals(10 * M, state(NationTrack.BASICS).getProgress());   // progress carried in
    }

    @Test
    void completingTheTeleportGateGrantsAscendedStatus() {
        Nation n = prepare(9, 300_000, 4_000, 2_000, false);
        store.addActiveDays(n.getId(), 800, LocalDate.of(2020, 1, 1));
        state(NationTrack.LANDMARKS).setProgress(795 * M);
        assertEquals("e5.teleport-gate", state(NationTrack.LANDMARKS).getCurrentItemKey());
        assertFalse(n.isAscended());
        NationResultDto r = solve(Difficulty.HARD, T0);   // +10 landmark points -> 805 >= 800
        assertTrue(n.isAscended());
        assertEquals("Singularity", r.rankName());
    }

    @Test
    void subTiersSplitEachRankIntoThreeParts() {
        assertEquals(0, NationRules.tierIndex(0L, 0, props));
        assertEquals(0, NationRules.tierIndex(299_999_999L, 0, props));
        assertEquals(1, NationRules.tierIndex(300 * M, 0, props));
        assertEquals(2, NationRules.tierIndex(600 * M, 0, props));
        assertEquals(2, NationRules.tierIndex(899 * M, 0, props));
        assertEquals(0, NationRules.tierIndex(34_999 * M, 4, props));   // Kernel 27,000 .. 51,000, step 8,000
        assertEquals(1, NationRules.tierIndex(35_000 * M, 4, props));
        assertEquals(2, NationRules.tierIndex(43_000 * M, 4, props));
        // Singularity: fixed spacing
        assertEquals(0, NationRules.tierIndex(285_000 * M, 9, props));
        assertEquals(0, NationRules.tierIndex(289_999 * M, 9, props));
        assertEquals(1, NationRules.tierIndex(290_000 * M, 9, props));
        assertEquals(2, NationRules.tierIndex(295_000 * M, 9, props));
        assertEquals(2, NationRules.tierIndex(400_000 * M, 9, props));
    }

    // ----------------------------------------------- daily limit, time zone, days

    @Test
    void easySolvesAfterTheFifthOfTheDayGiveExactlyHalf() {
        for (int i = 0; i < 5; i++) {
            assertEquals(10 * M, solve(Difficulty.EASY, T0).xpGained());
        }
        NationResultDto sixth = solve(Difficulty.EASY, T0);
        assertEquals(5 * M, sixth.xpGained());
        assertEquals(5 * M, sixth.basicsPoints());
        assertEquals(2 * M, sixth.structuresPoints());
        assertEquals(500_000L, sixth.landmarksPoints());
        // medium and hard are never halved
        assertEquals(30 * M, solve(Difficulty.MEDIUM, T0).xpGained());
        // a new day resets the counter
        assertEquals(10 * M, solveOnDay(Difficulty.EASY, 1).xpGained());
    }

    @Test
    void localDayFollowsTheNationTimeZone() {
        nation().setTimeZone("Asia/Kolkata");
        Instant lateEveningUtc = Instant.parse("2026-03-01T20:00:00Z");   // 2 March 01:30 in India
        Instant nextMiddayUtc = Instant.parse("2026-03-02T10:00:00Z");    // 2 March 15:30 in India
        for (int i = 0; i < 5; i++) {
            solve(Difficulty.EASY, lateEveningUtc);
        }
        // same Indian day -> still over the limit -> halved (in UTC this would be a fresh day)
        assertEquals(5 * M, solve(Difficulty.EASY, nextMiddayUtc).xpGained());
        assertEquals(1L, store.countActiveDays(nation().getId()));
    }

    @Test
    void activeDayIsRecordedOncePerLocalDay() {
        solve(Difficulty.HARD, T0);
        solve(Difficulty.HARD, T0.plusSeconds(60));
        assertEquals(1L, store.countActiveDays(nation().getId()));
        solveOnDay(Difficulty.HARD, 1);
        assertEquals(2L, store.countActiveDays(nation().getId()));
    }
}

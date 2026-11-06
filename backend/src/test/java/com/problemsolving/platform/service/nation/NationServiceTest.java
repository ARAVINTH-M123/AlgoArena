package com.problemsolving.platform.service.nation;

import com.problemsolving.platform.api.dto.nation.NationResultDto;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.Nation;
import com.problemsolving.platform.domain.entity.NationTrackState;
import com.problemsolving.platform.domain.enums.Difficulty;
import com.problemsolving.platform.domain.enums.NationTrack;
import com.problemsolving.platform.service.cosmetics.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

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
        // seed 7: occasionally drops a cache during the long solve loops; harmless for the assertions below
        service = new NationService(store, props, new CacheService(store, new Random(7)));
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
        // since Phase 7 the first solve of the day carries the +50% daily-goal bonus (10 + 5)
        assertEquals(15 * M, r.xpGained());
        assertEquals(5 * M, r.bonusXp());
        assertEquals(List.of("DAILY_GOAL"), r.bonusLabels());
        assertEquals(1, r.streakDays());
        assertEquals(1, r.dailyGoalSolvedToday());
        assertEquals(3, r.dailyGoalTarget());
        assertEquals(10 * M, r.basicsPoints());          // track points never get the bonus
        assertEquals(4 * M, r.structuresPoints());
        assertEquals(1 * M, r.landmarksPoints());
        assertEquals("Null", r.rankName());
        assertEquals(1, r.tier());
        assertEquals(15 * M, r.nationXp());
        assertEquals("Grass patch", r.tracks().get(0).itemName());
        assertEquals(0, r.tracks().get(0).stage());
    }

    @Test
    void trackPointsDependOnDifficulty() {
        NationResultDto medium = solve(Difficulty.MEDIUM, T0);   // 2nd solve of the day, still under the goal
        assertEquals(45 * M, medium.xpGained());                 // 30 + 50% goal bonus
        assertEquals(15 * M, medium.bonusXp());
        assertEquals(5 * M, medium.basicsPoints());
        assertEquals(10 * M, medium.structuresPoints());
        assertEquals(4 * M, medium.landmarksPoints());

        NationResultDto hard = solve(Difficulty.HARD, T0);       // 3rd solve, the last one under the goal
        assertEquals(105 * M, hard.xpGained());                  // 70 + 50% goal bonus
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
        assertEquals(15 * M, nation().getXp());   // only the first award (10 + 5 goal bonus)
    }

    @Test
    void resolveAfterTwentyFourHoursGivesExactlyOneTenThousandth() {
        service.awardForAcceptedSolve(USER, 77L, Difficulty.EASY, T0);
        NationResultDto r = service.awardForAcceptedSolve(USER, 77L, Difficulty.EASY, T0.plus(Duration.ofHours(24)));
        assertEquals("RESOLVE", r.mode());
        assertEquals(1_000L, r.xpGained());     // 1/10,000 of the base 10 XP: no goal bonus, no retention
        assertEquals(0L, r.bonusXp());
        assertEquals(0, r.bonusLabels().size());
        assertEquals(1_000L, r.basicsPoints());
        assertEquals(400L, r.structuresPoints());
        assertEquals(100L, r.landmarksPoints());
        assertEquals(15 * M + 1_000L, nation().getXp());
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
        for (int i = 1; i <= 10; i++) {
            last = solve(Difficulty.HARD, T0);   // 3x105 + 7x70 + 50 (lifetime milestone 10) = 855 XP (< 900)
        }
        assertTrue(state(NationTrack.LANDMARKS).isGateDone());   // 10 x 10 landmark points built Watchtower
        assertEquals("Null", last.rankName());
        NationResultDto eleventh = solve(Difficulty.HARD, T0);    // 925 XP
        assertEquals("Init", eleventh.rankName());
        assertEquals("Init", eleventh.rankUps().get(0));
        assertEquals(1, store.rankEventCount());
    }

    @Test
    void aRankUpReportsTheCosmeticsItUnlocked() {
        NationResultDto last = null;
        for (int i = 1; i <= 10; i++) {
            last = solve(Difficulty.HARD, T0);
            assertEquals(0, last.unlocked().size());          // nothing unlocked without a rank-up
        }
        NationResultDto rankUp = solve(Difficulty.HARD, T0);   // the 11th hard solve reaches Init (925 XP)
        assertEquals("Init", rankUp.rankName());
        assertEquals(4, rankUp.unlocked().size());            // since 5B: theme + border + anthem + emblem
        assertEquals("THEME", rankUp.unlocked().get(0).category());
        assertEquals("theme.ember", rankUp.unlocked().get(0).key());
        assertEquals("Ember", rankUp.unlocked().get(0).name());
        assertEquals("border.ember-glow", rankUp.unlocked().get(1).key());
        assertEquals("ANTHEM", rankUp.unlocked().get(2).category());
        assertEquals("anthem.campfire", rankUp.unlocked().get(2).key());
        assertEquals("FLAG_EMBLEM", rankUp.unlocked().get(3).category());
        assertEquals("emblem.seedling", rankUp.unlocked().get(3).key());
    }

    @Test
    void gateLandmarkAloneBlocksRankUp() {
        NationResultDto r = null;
        for (int i = 0; i < 99; i++) {
            r = solveOnDay(Difficulty.EASY, i);   // one easy a day: no daily halving
        }
        assertEquals(3915 * M, nation().getXp());   // 99 days x 15 (goal bonus) + streak milestones 3/7/14/30 (2010)
        // ... + lifetime milestones 10/25/50 (420): 1485 + 2010 + 420 = 3915; the 100th solve grants milestone 100
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
        // the first 3 solves of the day also carry the +50% daily-goal bonus (Phase 7)
        assertEquals(15 * M, solve(Difficulty.EASY, T0).xpGained());
        assertEquals(15 * M, solve(Difficulty.EASY, T0).xpGained());
        assertEquals(15 * M, solve(Difficulty.EASY, T0).xpGained());
        assertEquals(10 * M, solve(Difficulty.EASY, T0).xpGained());
        assertEquals(10 * M, solve(Difficulty.EASY, T0).xpGained());
        NationResultDto sixth = solve(Difficulty.EASY, T0);
        assertEquals(5 * M, sixth.xpGained());
        assertEquals(0L, sixth.bonusXp());      // over the goal AND over the easy limit
        assertEquals(5 * M, sixth.basicsPoints());
        assertEquals(2 * M, sixth.structuresPoints());
        assertEquals(500_000L, sixth.landmarksPoints());
        // medium and hard are never halved (this is the 6th first-solve of the day: past the goal)
        assertEquals(30 * M, solve(Difficulty.MEDIUM, T0).xpGained());
        // a new day resets both counters: full easy XP with the goal bonus again
        NationResultDto nextDay = solveOnDay(Difficulty.EASY, 1);
        assertEquals(15 * M, nextDay.xpGained());
        assertEquals(5 * M, nextDay.bonusXp());
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

    // ----------------------------------------------- Extra XP and retention (Phase 7)

    @Test
    void dailyGoalBonusAppliesToTheFirstSolvesOfTheDay() {
        NationResultDto first = solve(Difficulty.EASY, T0);      // 1 of 3: 10 + 5
        assertEquals(15 * M, first.xpGained());
        assertEquals(5 * M, first.bonusXp());
        assertEquals(List.of("DAILY_GOAL"), first.bonusLabels());
        assertEquals(1, first.dailyGoalSolvedToday());
        assertEquals(3, first.dailyGoalTarget());

        NationResultDto second = solve(Difficulty.MEDIUM, T0);   // 2 of 3: 30 + 15
        assertEquals(45 * M, second.xpGained());
        assertEquals(2, second.dailyGoalSolvedToday());

        NationResultDto third = solve(Difficulty.HARD, T0);      // 3 of 3: 70 + 35
        assertEquals(105 * M, third.xpGained());

        NationResultDto fourth = solve(Difficulty.EASY, T0);     // past the goal: no bonus
        assertEquals(10 * M, fourth.xpGained());
        assertEquals(0L, fourth.bonusXp());
        assertEquals(0, fourth.bonusLabels().size());
        assertEquals(4, fourth.dailyGoalSolvedToday());

        NationResultDto nextDay = solveOnDay(Difficulty.EASY, 1); // the goal resets with the local day
        assertEquals(15 * M, nextDay.xpGained());
        assertEquals(List.of("DAILY_GOAL"), nextDay.bonusLabels());
        assertEquals(1, nextDay.dailyGoalSolvedToday());
    }

    @Test
    void dailyGoalBonusIsPercentOfTheActuallyAwardedXp() {
        props.setDailyEasyLimit(2);                              // easies 3+ are halved
        props.setDailyGoalSolves(10);                            // goal covers the whole day
        assertEquals(15 * M, solve(Difficulty.EASY, T0).xpGained());
        assertEquals(15 * M, solve(Difficulty.EASY, T0).xpGained());
        // halved first: 5 XP, then +50% of the halved amount
        NationResultDto third = solve(Difficulty.EASY, T0);
        assertEquals(7_500_000L, third.xpGained());
        assertEquals(2_500_000L, third.bonusXp());
    }

    @Test
    void streakGrowsOnConsecutiveDaysResetsAfterAGapAndMilestonesRegrant() {
        NationResultDto day0 = solveOnDay(Difficulty.EASY, 0);
        assertEquals(1, day0.streakDays());
        NationResultDto day1 = solveOnDay(Difficulty.EASY, 1);
        assertEquals(2, day1.streakDays());
        assertEquals(0L, day1.bonusXp() - 5 * M);                // only the goal bonus so far

        NationResultDto day2 = solveOnDay(Difficulty.EASY, 2);   // streak 3 -> the first milestone
        assertEquals(3, day2.streakDays());
        assertEquals(List.of("DAILY_GOAL", "STREAK_MILESTONE_3"), day2.bonusLabels());
        assertEquals(15 * M + 30 * M, day2.xpGained());          // goal bonus + 30-point milestone

        NationResultDto sameDay = solve(Difficulty.EASY, T0.plus(Duration.ofDays(2)).plusSeconds(60));
        assertEquals(3, sameDay.streakDays());                   // a second solve does not grow the streak
        assertEquals(List.of("DAILY_GOAL"), sameDay.bonusLabels());   // ...but it is still 2nd of the day: under the goal

        NationResultDto day5 = solveOnDay(Difficulty.EASY, 5);   // days 3-4 missed: the streak breaks
        assertEquals(1, day5.streakDays());
        assertEquals(3, nation().getBestStreakDays());           // the best streak is kept
        NationResultDto day6 = solveOnDay(Difficulty.EASY, 6);
        NationResultDto day7 = solveOnDay(Difficulty.EASY, 7);   // back at streak 3: the milestone regrants
        assertEquals(3, day7.streakDays());
        assertEquals(List.of("DAILY_GOAL", "STREAK_MILESTONE_3"), day7.bonusLabels());
        assertEquals(15 * M + 30 * M, day7.xpGained());
        NationResultDto day8 = solveOnDay(Difficulty.EASY, 8);   // streak 4: no new milestone
        assertEquals(4, day8.streakDays());
        assertEquals(List.of("DAILY_GOAL"), day8.bonusLabels());
        assertEquals(4, nation().getBestStreakDays());
    }

    @Test
    void streakMilestoneSevenGrantsEightyWithoutRepeatingThree() {
        NationResultDto withThree = null;
        for (int day = 0; day <= 2; day++) {
            withThree = solveOnDay(Difficulty.EASY, day);
        }
        assertEquals(List.of("DAILY_GOAL", "STREAK_MILESTONE_3"), withThree.bonusLabels());
        for (int day = 3; day <= 5; day++) {
            NationResultDto plain = solveOnDay(Difficulty.EASY, day);
            assertEquals(List.of("DAILY_GOAL"), plain.bonusLabels());   // no milestone on streak 4-6
        }
        NationResultDto seventh = solveOnDay(Difficulty.EASY, 6);
        assertEquals(7, seventh.streakDays());
        assertEquals(List.of("DAILY_GOAL", "STREAK_MILESTONE_7"), seventh.bonusLabels());
        assertEquals(15 * M + 80 * M, seventh.xpGained());       // the 7 milestone, not 3 again
        assertEquals(7, nation().getBestStreakDays());
        assertEquals(7, nation().getStreakDays());
    }

    @Test
    void comebackBonusAfterFourteenDaysAway() {
        solveOnDay(Difficulty.EASY, 0);
        NationResultDto stillAround = solveOnDay(Difficulty.EASY, 13);   // gap 13: not a comeback yet
        assertEquals(List.of("DAILY_GOAL"), stillAround.bonusLabels());
        assertEquals(1, nation().getStreakDays());               // the streak already broke at day 13

        NationResultDto comeback = solveOnDay(Difficulty.EASY, 30);      // gap 17 >= 14
        assertEquals(15 * M + 100 * M, comeback.xpGained());     // goal bonus + flat 100-point welcome back
        assertEquals(List.of("DAILY_GOAL", "COMEBACK"), comeback.bonusLabels());
        assertEquals(1, comeback.streakDays());
    }

    @Test
    void lifetimeMilestonesGrantOnceAndOldNationsCatchUp() {
        for (int day = 0; day <= 8; day++) {
            solveOnDay(Difficulty.EASY, day);
        }
        NationResultDto tenth = solveOnDay(Difficulty.EASY, 9);  // 10 lifetime solves -> the first milestone
        assertEquals(List.of("DAILY_GOAL", "LIFETIME_MILESTONE_10"), tenth.bonusLabels());
        assertEquals(15 * M + 50 * M, tenth.xpGained());

        NationResultDto eleventh = solveOnDay(Difficulty.EASY, 10);
        assertEquals(List.of("DAILY_GOAL"), eleventh.bonusLabels());     // strictly once

        // a nation that existed before Phase 7 catches up on its next first-solve
        Nation n = nation();
        n.setEasySolves(60);                                     // 61 total after the next solve
        n.setLastLifetimeMilestone(0);                           // simulate a pre-Phase-7 nation
        NationResultDto catchUp = solveOnDay(Difficulty.EASY, 11);
        assertEquals(List.of("DAILY_GOAL", "LIFETIME_MILESTONE_50"), catchUp.bonusLabels());
        assertEquals(15 * M + (50 + 120 + 250) * M, catchUp.xpGained());   // 10, 25 and 50 all at once

        NationResultDto afterCatchUp = solveOnDay(Difficulty.EASY, 12);
        assertEquals(List.of("DAILY_GOAL"), afterCatchUp.bonusLabels());   // ...and never again
    }

    @Test
    void resolveAndNoOpNeverTouchRetentionCounters() {
        service.awardForAcceptedSolve(USER, 77L, Difficulty.EASY, T0);
        int firstSolves = store.findDailyCounter(nation().getId(), LocalDate.parse("2026-03-01")).get().getFirstSolves();
        assertEquals(1, firstSolves);

        NationResultDto resolve = service.awardForAcceptedSolve(USER, 77L, Difficulty.EASY, T0.plus(Duration.ofHours(24)));
        assertEquals("RESOLVE", resolve.mode());
        assertEquals(0, resolve.streakDays());
        assertEquals(0, resolve.dailyGoalSolvedToday());
        assertEquals(1, nation().getStreakDays());               // unchanged by the re-solve
        assertEquals(1L, store.countActiveDays(nation().getId()));
        // the re-solve did not count towards any daily goal and created no counter for the new local day
        assertEquals(1, store.findDailyCounter(nation().getId(), LocalDate.parse("2026-03-01")).get().getFirstSolves());
        assertFalse(store.findDailyCounter(nation().getId(), LocalDate.parse("2026-03-02")).isPresent());

        props.setAwardEnabled(false);
        NationResultDto disabled = service.awardForAcceptedSolve(USER, 78L, Difficulty.EASY, T0.plus(Duration.ofDays(3)));
        assertEquals("NO_OP", disabled.mode());
        assertEquals(0, disabled.streakDays());
        assertEquals(0, disabled.dailyGoalSolvedToday());
    }

    // ----------------------------------------------- Mystery Caches (Phase 6)

    @Test
    void firstSolvesCanDropCachesButReSolvesAndNoOpsNeverDo() {
        // scripted random: 0.0 always rolls under every drop chance -> every FIRST solve drops a cache
        NationService alwaysDrop = new NationService(store, props, new CacheService(store,
                new ScriptedRandom(new double[]{0.0})));
        NationResultDto first = alwaysDrop.awardForAcceptedSolve(USER, 501L, Difficulty.EASY, T0);
        assertTrue(first.cacheDropped());
        assertEquals(1, store.nationOrNull(USER).getUnopenedCaches());

        // the same problem 24 h later is a RESOLVE: awarded again, but no drop and no roll
        NationResultDto resolve = alwaysDrop.awardForAcceptedSolve(
                USER, 501L, Difficulty.EASY, T0.plus(Duration.ofHours(24)));
        assertEquals("RESOLVE", resolve.mode());
        assertFalse(resolve.cacheDropped());
        assertEquals(1, store.nationOrNull(USER).getUnopenedCaches());

        // with awards disabled nothing is rolled at all
        props.setAwardEnabled(false);
        NationResultDto disabled = alwaysDrop.awardForAcceptedSolve(USER, 502L, Difficulty.HARD, T0);
        assertEquals("NO_OP", disabled.mode());
        assertFalse(disabled.cacheDropped());
        assertEquals(1, store.nationOrNull(USER).getUnopenedCaches());
    }
}

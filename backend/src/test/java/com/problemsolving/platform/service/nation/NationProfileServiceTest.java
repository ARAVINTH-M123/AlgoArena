package com.problemsolving.platform.service.nation;

import com.problemsolving.platform.api.dto.nation.NationDto;
import com.problemsolving.platform.api.dto.nation.UpdateNationProfileRequest;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.Nation;
import com.problemsolving.platform.domain.entity.NationTrackState;
import com.problemsolving.platform.domain.enums.Difficulty;
import com.problemsolving.platform.domain.enums.NationTrack;
import com.problemsolving.platform.api.dto.cosmetics.EquipRequest;
import com.problemsolving.platform.service.cosmetics.CosmeticService;
import com.problemsolving.platform.service.presence.InMemoryPresenceStore;
import com.problemsolving.platform.service.presence.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NationProfileServiceTest {

    private static final long M = NationProperties.MICRO;
    private static final Long USER = 1L;
    private static final String NAME = "alice";
    private static final Instant T0 = Instant.parse("2026-03-01T12:00:00Z");

    private InMemoryNationStore store;
    private NationProperties props;
    private NationService awards;
    private NationProfileService profile;
    private PresenceService presence;

    @BeforeEach
    void setUp() {
        store = new InMemoryNationStore();
        store.registerUser(USER, NAME);
        props = new NationProperties();
        awards = new NationService(store, props);
        presence = new PresenceService(new InMemoryPresenceStore(), store, props);
        profile = new NationProfileService(store, props, presence);
    }

    // ---------------------------------------------------------------- helpers

    private UpdateNationProfileRequest request(String name, String timeZone) {
        UpdateNationProfileRequest r = new UpdateNationProfileRequest();
        r.setName(name);
        r.setTimeZone(timeZone);
        return r;
    }

    private NationDto rename(String name, Instant at) {
        return profile.updateProfile(NAME, request(name, null), at);
    }

    private NationDto changeZone(String zone, Instant at) {
        return profile.updateProfile(NAME, request(null, zone), at);
    }

    /** Runs the action, expects a 400 and returns the message. */
    private String badRequest(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException ex) {
            assertEquals(400, ex.getStatusCode().value());
            return ex.getReason();
        }
        throw new AssertionError("expected a 400 response");
    }

    private Nation prepare(int rank, long xpPoints, int mediums, int hards, boolean gateDone) {
        Nation n = store.lockOrCreateNation(USER);
        n.setRankIndex(rank);
        n.setXp(xpPoints * M);
        n.setMediumSolves(mediums);
        n.setHardSolves(hards);
        int era = NationRules.contentEra(rank);
        for (NationTrack track : NationTrack.values()) {
            NationTrackState s = store.getOrCreateTrackState(n.getId(), track, NationCatalog.initialItemKey(track));
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

    private NationDto.Requirement requirement(NationDto dto, String type) {
        return dto.nextRank().requirements().stream().filter(r -> r.type().equals(type)).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------ views

    @Test
    void userWithoutSolvesGetsAnEmptyDefaultNationAndNothingIsCreated() {
        NationDto dto = profile.view(NAME, true, T0);
        assertEquals("alice's Land", dto.name());
        assertEquals("Null", dto.rankName());
        assertEquals(1, dto.tier());
        assertEquals(0L, dto.xp());
        assertEquals("UTC", dto.timeZone());
        assertEquals(3, dto.tracks().size());
        assertEquals("Grass patch", dto.tracks().get(0).itemName());
        assertEquals("Tent", dto.tracks().get(1).itemName());
        assertEquals("Watchtower", dto.tracks().get(2).itemName());
        assertEquals(0, dto.completedItems().size());
        assertEquals("Init", dto.nextRank().name());
        assertEquals(2, dto.nextRank().requirements().size());      // XP + gate landmark only
        assertEquals(900 * M, requirement(dto, "XP").required());
        assertFalse(requirement(dto, "XP").met());
        assertEquals("Build the Watchtower", requirement(dto, "GATE_LANDMARK").label());
        assertEquals(0, store.nationCount());                         // reading creates nothing
    }

    @Test
    void unknownUserIsNotFound() {
        try {
            profile.view("ghost", false, T0);
            throw new AssertionError("expected 404");
        } catch (ResponseStatusException ex) {
            assertEquals(404, ex.getStatusCode().value());
            assertEquals("User not found", ex.getReason());
        }
    }

    @Test
    void viewReflectsSolvesProgressAndCompletedItems() {
        for (int day = 0; day < 45; day++) {
            awards.awardForAcceptedSolve(USER, 100L + day, Difficulty.EASY, T0.plus(Duration.ofDays(day)));
        }
        NationDto dto = profile.view(NAME, false, T0.plus(Duration.ofDays(60)));
        assertEquals(450 * M, dto.xp());
        assertEquals(50, dto.nextRank().percentToNext());           // 450 of 900 XP
        assertEquals(45, dto.solves().easy());
        assertEquals(45L, dto.solves().activeDays());
        // 450 basics points / 50 = 9 items; 180 structures / 80 = 2 items; 45 landmark points = none yet
        long basics = dto.completedItems().stream().filter(i -> i.track().equals("BASICS")).count();
        long structures = dto.completedItems().stream().filter(i -> i.track().equals("STRUCTURES")).count();
        long landmarks = dto.completedItems().stream().filter(i -> i.track().equals("LANDMARKS")).count();
        assertEquals(9L, basics);
        assertEquals(2L, structures);
        assertEquals(0L, landmarks);
        assertEquals("e1.grass-patch", dto.completedItems().get(0).key());
        assertEquals(1, dto.completedItems().get(0).sequenceNumber());
        assertEquals("Grass patch", dto.completedItems().get(0).name());
        assertFalse(dto.owner());
    }

    @Test
    void requirementsShowExactlyWhatIsMissingForTheNextRank() {
        prepare(3, 20_000, 40, 0, false);                             // Root heading for Kernel
        NationDto dto = profile.view(NAME, true, T0);
        assertEquals("Kernel", dto.nextRank().name());
        assertEquals(27_000 * M, requirement(dto, "XP").required());
        assertFalse(requirement(dto, "XP").met());
        assertEquals(40L, requirement(dto, "MEDIUMS").current());
        assertEquals(100L, requirement(dto, "MEDIUMS").required());
        assertEquals("Build the Windmill", requirement(dto, "GATE_LANDMARK").label());
        assertEquals(null, requirement(dto, "HARDS"));                // not required for Kernel
        assertEquals(null, requirement(dto, "ACTIVE_DAYS"));
    }

    @Test
    void topRankHasNoNextRank() {
        prepare(9, 300_000, 4_000, 2_000, true);
        NationDto dto = profile.view(NAME, true, T0);
        assertEquals("Singularity", dto.rankName());
        assertEquals(null, dto.nextRank());
    }

    // ----------------------------------------------------------------- rename

    @Test
    void renameTrimsAndCollapsesSpaces() {
        NationDto dto = rename("  Stone   bridge  ", T0);
        assertEquals("Stone bridge", dto.name());
        assertTrue(dto.owner());
    }

    @Test
    void renameRejectsInvalidNames() {
        assertEquals("Nation name must be 3 to 24 characters", badRequest(() -> rename("ab", T0)));
        assertEquals("Nation name must be 3 to 24 characters",
                badRequest(() -> rename("abcdefghijklmnopqrstuvwxy", T0)));              // 25 characters
        assertEquals("Nation name can only contain letters, digits, spaces, hyphens and apostrophes",
                badRequest(() -> rename("Bad<Name>", T0)));
        assertEquals("That nation name is not allowed", badRequest(() -> rename("Fuck Land", T0)));
        assertEquals("That nation name is not allowed", badRequest(() -> rename("f-u-c-k town", T0)));
        // none of the failed attempts used up the cooldown
        assertEquals("Good Land", rename("Good Land", T0).name());
    }

    @Test
    void renameHasASevenDayCooldown() {
        rename("First Name", T0);
        String message = badRequest(() -> rename("Second Name", T0.plus(Duration.ofDays(7)).minusSeconds(1)));
        assertEquals("You can rename your nation again after 2026-03-08 (UTC)", message);
        assertEquals("Second Name", rename("Second Name", T0.plus(Duration.ofDays(7))).name());
    }

    @Test
    void renamingToTheSameNameDoesNotStartTheCooldown() {
        assertEquals("alice's Land", rename("alice's Land", T0).name());
        assertEquals("Fresh Name", rename("Fresh Name", T0.plusSeconds(5)).name());   // first real rename is immediate
    }

    @Test
    void curlyApostropheIsAccepted() {
        assertEquals("Bob's Land", rename("Bob\u2019s Land", T0).name());
    }

    // -------------------------------------------------------------- time zone

    @Test
    void timeZoneMustBeAKnownIanaId() {
        assertEquals("Unknown time zone", badRequest(() -> changeZone("Mars/Base", T0)));
        assertEquals("Unknown time zone", badRequest(() -> changeZone("+05:00", T0)));
        assertEquals("Unknown time zone", badRequest(() -> changeZone("", T0)));
        assertEquals("Asia/Kolkata", changeZone("Asia/Kolkata", T0).timeZone());
    }

    @Test
    void timeZoneHasAThirtyDayCooldownButSameZoneIsFree() {
        changeZone("Asia/Kolkata", T0);
        assertEquals("Asia/Kolkata", changeZone("Asia/Kolkata", T0.plus(Duration.ofDays(1))).timeZone());   // no-op
        String message = badRequest(() -> changeZone("Europe/Paris", T0.plus(Duration.ofDays(30)).minusSeconds(1)));
        assertEquals("You can change your time zone again after 2026-03-31 (UTC)", message);
        assertEquals("Europe/Paris", changeZone("Europe/Paris", T0.plus(Duration.ofDays(30))).timeZone());
    }

    @Test
    void invalidFieldChangesNothing() {
        String message = badRequest(() -> profile.updateProfile(NAME, request("Valid Name", "Mars/Base"), T0));
        assertEquals("Unknown time zone", message);
        assertEquals("alice's Land", profile.view(NAME, true, T0).name());
    }

    @Test
    void bothFieldsCanBeChangedTogether() {
        NationDto dto = profile.updateProfile(NAME, request("Twin Land", "Asia/Tokyo"), T0);
        assertEquals("Twin Land", dto.name());
        assertEquals("Asia/Tokyo", dto.timeZone());
    }

    @Test
    void onlyTheOwnerSeesCooldownDates() {
        rename("Cool Land", T0);
        changeZone("Asia/Kolkata", T0);
        NationDto owner = profile.view(NAME, true, T0.plus(Duration.ofDays(1)));
        assertEquals("2026-03-08T12:00:00Z", owner.renameAvailableAt());
        assertEquals("2026-03-31T12:00:00Z", owner.timeZoneChangeAvailableAt());
        NationDto visitor = profile.view(NAME, false, T0.plus(Duration.ofDays(1)));
        assertEquals(null, visitor.renameAvailableAt());
        assertEquals(null, visitor.timeZoneChangeAvailableAt());
        // once the cooldown is over the date disappears
        assertEquals(null, profile.view(NAME, true, T0.plus(Duration.ofDays(8))).renameAvailableAt());
    }

    @Test
    void changedTimeZoneIsUsedForTheNextAward() {
        changeZone("Asia/Kolkata", T0);
        Instant lateEveningUtc = Instant.parse("2026-03-05T20:00:00Z");   // already 6 March in India
        for (int i = 0; i < 5; i++) {
            awards.awardForAcceptedSolve(USER, 500L + i, Difficulty.EASY, lateEveningUtc);
        }
        Instant nextMiddayUtc = Instant.parse("2026-03-06T10:00:00Z");    // same Indian day
        assertEquals(5 * M, awards.awardForAcceptedSolve(USER, 900L, Difficulty.EASY, nextMiddayUtc).xpGained());
    }

    // -------------------------------------------------------------- presence in the nation view

    @Test
    void nationViewCarriesPresenceAndOwnerSeesTheTrueStatus() {
        presence.heartbeat(USER, "session-aaaa-1111", true, T0);
        NationDto visitor = profile.view(NAME, false, T0.plusSeconds(5));
        assertEquals("ONLINE", visitor.presence().status());
        assertEquals(null, visitor.presence().lastActiveAt());
        assertFalse(visitor.appearOffline());
    }

    @Test
    void appearOfflineHidesTheStatusFromVisitorsButNotFromTheOwner() {
        presence.heartbeat(USER, "session-aaaa-1111", true, T0);
        UpdateNationProfileRequest request = new UpdateNationProfileRequest();
        request.setAppearOffline(true);
        NationDto owner = profile.updateProfile(NAME, request, T0.plusSeconds(1));
        assertTrue(owner.appearOffline());
        assertEquals("ONLINE", owner.presence().status());                 // owners see the truth

        NationDto visitor = profile.view(NAME, false, T0.plusSeconds(2));
        assertEquals("OFFLINE", visitor.presence().status());              // visitors do not
        assertEquals(null, visitor.presence().lastActiveAt());              // and get no last-seen time
        assertFalse(visitor.appearOffline());

        request.setAppearOffline(false);
        assertEquals("ONLINE", profile.updateProfile(NAME, request, T0.plusSeconds(3)).presence().status());
        assertEquals("ONLINE", profile.view(NAME, false, T0.plusSeconds(4)).presence().status());
    }

    // ----------------------------------------------------------- equipped cosmetics

    @Test
    void nationViewShowsTheCosmeticsInEffectToEveryone() {
        NationDto fresh = profile.view(NAME, false, T0);
        assertEquals("theme.void", fresh.themeKey());
        assertEquals("border.thin-gray", fresh.borderKey());

        prepare(4, 30_000, 0, 0, true);                                   // Kernel
        assertEquals("border.stone", profile.view(NAME, false, T0).borderKey());

        EquipRequest pick = new EquipRequest();
        pick.setCategory("BORDER");
        pick.setKey("border.wood-grain");
        new CosmeticService(store, props).equip(NAME, pick, T0);
        assertEquals("border.wood-grain", profile.view(NAME, false, T0).borderKey());   // visitors see the owner's pick
    }
}

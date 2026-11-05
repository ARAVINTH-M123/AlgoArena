package com.problemsolving.platform.service.presence;

import com.problemsolving.platform.api.dto.presence.PresenceDto;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.service.nation.InMemoryNationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresenceServiceTest {

    private static final Long USER = 1L;
    private static final Instant T0 = Instant.parse("2026-03-01T12:00:00Z");
    private static final String TAB_A = "tab-aaaa-0001";
    private static final String TAB_B = "tab-bbbb-0002";

    private InMemoryNationStore store;
    private NationProperties props;
    private PresenceService presence;

    @BeforeEach
    void setUp() {
        store = new InMemoryNationStore();
        store.registerUser(USER, "alice");
        props = new NationProperties();
        presence = new PresenceService(new InMemoryPresenceStore(), store, props);
    }

    // ---------------------------------------------------------------- helpers

    private Instant at(long seconds) {
        return T0.plusSeconds(seconds);
    }

    private PresenceDto visitorView(Instant now) {
        return presence.forViewer(USER, store.nationOrNull(USER), false, now);
    }

    private String status(Instant now) {
        return visitorView(now).status();
    }

    /** Same two steps as the profile service: store the flag on the nation row, then tell presence. */
    private void chooseAppearOffline(boolean appearOffline, Instant now) {
        store.lockOrCreateNation(USER).setAppearOffline(appearOffline);
        presence.setAppearOffline(USER, appearOffline, now);
    }

    private String errorOf(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException ex) {
            return ex.getStatusCode().value() + " " + ex.getReason();
        }
        throw new AssertionError("expected an error response");
    }

    /** Keeps a tab alive with inactive heartbeats every 30 s, from fromSecond up to and including untilSecond. */
    private void keepAliveInactive(String tab, long fromSecond, long untilSecond) {
        for (long s = fromSecond; s <= untilSecond; s += 30) {
            assertTrue(presence.heartbeat(USER, tab, false, at(s)));
        }
    }

    // ------------------------------------------------------------ core status

    @Test
    void nobodyIsOfflineWithoutAHeartbeat() {
        assertEquals("OFFLINE", status(at(0)));
    }

    @Test
    void anActiveHeartbeatMakesTheUserOnline() {
        assertTrue(presence.heartbeat(USER, TAB_A, true, at(0)));
        assertEquals("ONLINE", status(at(1)));
    }

    @Test
    void aSessionIsLiveForExactlySeventyFiveSeconds() {
        presence.heartbeat(USER, TAB_A, true, at(0));
        assertEquals("ONLINE", status(at(74)));
        assertEquals("OFFLINE", status(at(75)));
    }

    @Test
    void noActivityForFiveMinutesTurnsOnlineIntoIdle() {
        presence.heartbeat(USER, TAB_A, true, at(0));          // the only activity
        keepAliveInactive(TAB_A, 30, 270);                      // tab stays open, user does nothing
        assertEquals("ONLINE", status(at(299)));
        assertEquals("IDLE", status(at(300)));
    }

    @Test
    void newActivityBringsAnIdleUserBackOnline() {
        presence.heartbeat(USER, TAB_A, true, at(0));
        keepAliveInactive(TAB_A, 30, 330);
        assertEquals("IDLE", status(at(340)));
        presence.heartbeat(USER, TAB_A, true, at(360));
        assertEquals("ONLINE", status(at(361)));
    }

    @Test
    void severalSessionsCountTogether() {
        presence.heartbeat(USER, TAB_A, true, at(0));
        keepAliveInactive(TAB_A, 30, 390);                      // tab A: open but untouched for 6 minutes
        assertEquals("IDLE", status(at(395)));
        presence.heartbeat(USER, TAB_B, true, at(400));         // tab B: the user is active here
        assertEquals("ONLINE", status(at(401)));
        presence.offline(USER, TAB_B, at(410));                 // B closes; A is still open but idle
        assertEquals("IDLE", status(at(411)));
        presence.offline(USER, TAB_A, at(420));
        assertEquals("OFFLINE", status(at(421)));
    }

    // ------------------------------------------------------- input validation

    @Test
    void heartbeatsFasterThanFifteenSecondsAreRejected() {
        assertTrue(presence.heartbeat(USER, TAB_A, true, at(0)));
        assertFalse(presence.heartbeat(USER, TAB_A, true, at(14)));
        assertTrue(presence.heartbeat(USER, TAB_A, true, at(15)));
        assertTrue(presence.heartbeat(USER, TAB_B, true, at(16)));   // another tab has its own limit
    }

    @Test
    void sessionIdsMustLookLikeRandomIds() {
        assertEquals("400 Invalid session id", errorOf(() -> presence.heartbeat(USER, "short", true, at(0))));
        assertEquals("400 Invalid session id", errorOf(() -> presence.heartbeat(USER, "bad id with spaces!", true, at(0))));
        assertEquals("400 Invalid session id", errorOf(() -> presence.heartbeat(USER, null, true, at(0))));
        assertEquals("400 Invalid session id", errorOf(() -> presence.offline(USER, "x", at(0))));
    }

    @Test
    void oneUserCannotHoldMoreThanTheSessionLimit() {
        props.setPresenceMaxSessionsPerUser(3);
        for (int i = 1; i <= 4; i++) {
            presence.heartbeat(USER, "session-number-" + i, true, at(i));
        }
        // the oldest session (1) was dropped; stop the other three and the user is offline
        presence.offline(USER, "session-number-2", at(10));
        presence.offline(USER, "session-number-3", at(10));
        presence.offline(USER, "session-number-4", at(10));
        assertEquals("OFFLINE", status(at(11)));
    }

    // -------------------------------------------------------------- last seen

    @Test
    void lastSeenIsWrittenToTheDatabaseAtMostOncePerMinute() {
        presence.heartbeat(USER, TAB_A, true, at(0));     // first heartbeat: written
        presence.heartbeat(USER, TAB_A, true, at(30));    // within 60 s: not written
        presence.heartbeat(USER, TAB_A, true, at(45));    // still within 60 s of the write: not written
        assertEquals(1, store.lastSeenWrites());
        presence.heartbeat(USER, TAB_A, true, at(60));    // a minute later: written
        assertEquals(2, store.lastSeenWrites());
    }

    @Test
    void closingATabRecordsLastSeenImmediatelyAndOfflineShowsIt() {
        presence.heartbeat(USER, TAB_A, true, at(0));
        presence.offline(USER, TAB_A, at(10));
        PresenceDto view = visitorView(at(11));
        assertEquals("OFFLINE", view.status());
        assertEquals(at(10).toString(), view.lastActiveAt());
    }

    @Test
    void anExpiredSessionLeavesItsLastHeartbeatAsLastSeen() {
        presence.heartbeat(USER, TAB_A, true, at(0));
        presence.heartbeat(USER, TAB_A, true, at(30));    // not persisted yet (throttle)
        presence.tick(at(120));                            // the tab vanished without saying goodbye
        assertEquals(at(30).toString(), visitorView(at(121)).lastActiveAt());
    }

    // ------------------------------------------------------------ appear offline

    @Test
    void appearOfflineHidesTheUserFromVisitorsOnly() {
        presence.heartbeat(USER, TAB_A, true, at(0));
        chooseAppearOffline(true, at(1));
        PresenceDto visitor = presence.forViewer(USER, store.nationOrNull(USER), false, at(2));
        assertEquals("OFFLINE", visitor.status());
        assertEquals(null, visitor.lastActiveAt());
        assertEquals("ONLINE", presence.forViewer(USER, store.nationOrNull(USER), true, at(2)).status());
        chooseAppearOffline(false, at(3));
        assertEquals("ONLINE", status(at(4)));
    }

    // -------------------------------------------------------- live notifications

    @Test
    void listenersAreToldOnlyWhenTheVisibleStateChanges() {
        List<PresenceDto> events = new ArrayList<>();
        presence.subscribe(USER, events::add);

        presence.heartbeat(USER, TAB_A, true, at(0));
        assertEquals(1, events.size());
        assertEquals("ONLINE", events.get(0).status());

        keepAliveInactive(TAB_A, 30, 270);                  // ordinary heartbeats change nothing
        assertEquals(1, events.size());

        presence.tick(at(300));                              // five quiet minutes: IDLE
        assertEquals(2, events.size());
        assertEquals("IDLE", events.get(1).status());

        presence.tick(at(310));                              // no change, no event
        assertEquals(2, events.size());

        presence.tick(at(400));                              // the last heartbeat is long gone: session expires
        assertEquals(3, events.size());
        assertEquals("OFFLINE", events.get(2).status());
        assertEquals(at(270).toString(), events.get(2).lastActiveAt());
    }

    @Test
    void closingTheLastTabPublishesOfflineImmediately() {
        List<PresenceDto> events = new ArrayList<>();
        presence.heartbeat(USER, TAB_A, true, at(0));
        presence.subscribe(USER, events::add);
        presence.offline(USER, TAB_A, at(5));
        assertEquals(1, events.size());
        assertEquals("OFFLINE", events.get(0).status());
    }

    @Test
    void switchingAppearOfflineNotifiesWatchersAtOnce() {
        List<PresenceDto> events = new ArrayList<>();
        presence.heartbeat(USER, TAB_A, true, at(0));
        presence.subscribe(USER, events::add);
        chooseAppearOffline(true, at(1));
        assertEquals("OFFLINE", events.get(events.size() - 1).status());
        chooseAppearOffline(false, at(2));
        assertEquals("ONLINE", events.get(events.size() - 1).status());
    }

    @Test
    void aFirstSolveMakesTheFigureGlowForSixtySeconds() {
        List<PresenceDto> events = new ArrayList<>();
        presence.heartbeat(USER, TAB_A, true, at(0));
        presence.subscribe(USER, events::add);

        presence.markSolved(USER, at(1));
        assertEquals(60, events.get(events.size() - 1).justSolvedSecondsLeft());
        assertEquals(30, visitorView(at(31)).justSolvedSecondsLeft());
        assertEquals(1, visitorView(at(60)).justSolvedSecondsLeft());
        assertEquals(0, visitorView(at(61)).justSolvedSecondsLeft());

        int before = events.size();
        presence.heartbeat(USER, TAB_A, false, at(30));      // keep the session alive
        presence.heartbeat(USER, TAB_A, false, at(60));
        presence.tick(at(61));                                // the glow ends: exactly one more event
        assertEquals(before + 1, events.size());
        assertEquals(0, events.get(events.size() - 1).justSolvedSecondsLeft());
        assertEquals("ONLINE", events.get(events.size() - 1).status());
    }

    @Test
    void anOfflineUserNeverGlows() {
        presence.markSolved(USER, at(0));
        assertEquals(0, visitorView(at(1)).justSolvedSecondsLeft());
    }

    @Test
    void streamSubscriptionsPerNationAreLimitedAndCanBeReleased() {
        props.setPresenceMaxStreamsPerUser(2);
        Runnable first = presence.subscribe(USER, dto -> { });
        presence.subscribe(USER, dto -> { });
        assertEquals("429 Too many viewers are watching this nation",
                errorOf(() -> presence.subscribe(USER, dto -> { })));
        first.run();
        presence.subscribe(USER, dto -> { });                 // a slot is free again
    }

    @Test
    void aBrokenListenerDoesNotStopTheOthers() {
        List<PresenceDto> events = new ArrayList<>();
        presence.subscribe(USER, dto -> {
            throw new IllegalStateException("closed stream");
        });
        presence.subscribe(USER, events::add);
        presence.heartbeat(USER, TAB_A, true, at(0));
        assertEquals(1, events.size());
    }
}

package com.problemsolving.platform.service.nation;

import com.problemsolving.platform.domain.entity.CacheOpenLog;
import com.problemsolving.platform.domain.entity.Nation;
import com.problemsolving.platform.domain.entity.NationActiveDay;
import com.problemsolving.platform.domain.entity.NationCompletedItem;
import com.problemsolving.platform.domain.entity.NationCosmeticOwned;
import com.problemsolving.platform.domain.entity.NationDailyCounter;
import com.problemsolving.platform.domain.entity.NationProblemAward;
import com.problemsolving.platform.domain.entity.NationRankEvent;
import com.problemsolving.platform.domain.entity.NationTrackState;
import com.problemsolving.platform.domain.enums.NationTrack;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything the Nation game logic needs from storage. NationService depends only on this port,
 * so the rules can be unit-tested with an in-memory store while production uses JpaNationStore.
 */
public interface NationStore {

    /** Returns the user's nation (created if missing), locked for update inside the current transaction. */
    Nation lockOrCreateNation(Long userId);

    Nation saveNation(Nation nation);

    Optional<NationProblemAward> findAward(Long nationId, Long problemId);

    NationProblemAward saveAward(NationProblemAward award);

    NationDailyCounter getOrCreateDailyCounter(Long nationId, LocalDate day);

    NationDailyCounter saveDailyCounter(NationDailyCounter counter);

    /** Read-only lookup of a day's counter (used for the daily-goal progress); never creates a row. */
    Optional<NationDailyCounter> findDailyCounter(Long nationId, LocalDate day);

    /** Records an active day; returns true only when the day was newly added. */
    boolean recordActiveDay(Long nationId, LocalDate day);

    long countActiveDays(Long nationId);

    NationTrackState getOrCreateTrackState(Long nationId, NationTrack track, String initialItemKey);

    NationTrackState saveTrackState(NationTrackState state);

    long countCompletedItems(Long nationId, String catalogKey);

    NationCompletedItem saveCompletedItem(NationCompletedItem item);

    NationRankEvent saveRankEvent(NationRankEvent event);

    // ---------------- acquired cosmetics (Phase 6: caches, shards, special grants) ----------------

    /** Every acquired cosmetic of the user (rank cosmetics are NOT here - they stay derived from the rank). */
    List<NationCosmeticOwned> findOwnedCosmetics(Long userId);

    boolean ownsCosmetic(Long userId, String cosmeticKey);

    NationCosmeticOwned saveOwnedCosmetic(NationCosmeticOwned owned);

    CacheOpenLog saveCacheOpenLog(CacheOpenLog log);

    // ---------------- read side (no locking, never creates rows) ----------------

    Optional<Nation> findNation(Long userId);

    List<NationTrackState> findTrackStates(Long nationId);

    List<NationCompletedItem> findCompletedItems(Long nationId);

    Optional<Long> findUserId(String username);

    /** Nations of the given users, keyed by user id (users without a nation are simply absent). */
    Map<Long, Nation> findNationsByUserIds(Collection<Long> userIds);

    /** Stores when the user was last seen alive (creates the nation row if needed). */
    void recordLastSeen(Long userId, Instant at);
}

package com.problemsolving.platform.service.nation;

import com.problemsolving.platform.domain.entity.Nation;
import com.problemsolving.platform.domain.entity.NationCompletedItem;
import com.problemsolving.platform.domain.entity.NationDailyCounter;
import com.problemsolving.platform.domain.entity.NationProblemAward;
import com.problemsolving.platform.domain.entity.NationRankEvent;
import com.problemsolving.platform.domain.entity.NationTrackState;
import com.problemsolving.platform.domain.enums.NationTrack;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Test-only store: keeps everything in memory so the game rules can be tested without a database. */
public class InMemoryNationStore implements NationStore {

    private long nextId = 1;
    private int lastSeenWrites = 0;
    private final Map<Long, Nation> nationsByUser = new HashMap<>();
    private final Map<String, Long> userIdsByName = new HashMap<>();
    private final Map<Long, String> namesByUserId = new HashMap<>();
    private final Map<String, NationProblemAward> awards = new HashMap<>();
    private final Map<String, NationDailyCounter> counters = new HashMap<>();
    private final Set<String> activeDays = new HashSet<>();
    private final Map<String, NationTrackState> trackStates = new HashMap<>();
    private final List<NationCompletedItem> completedItems = new ArrayList<>();
    private final List<NationRankEvent> rankEvents = new ArrayList<>();

    private void assignId(Object entity) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            if (field.get(entity) == null) {
                field.set(entity, nextId++);
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public Nation lockOrCreateNation(Long userId) {
        return nationsByUser.computeIfAbsent(userId, id -> {
            Nation nation = new Nation();
            nation.setUserId(id);
            nation.setName(namesByUserId.getOrDefault(id, "user-" + id) + "'s Land");
            assignId(nation);
            return nation;
        });
    }

    @Override
    public Nation saveNation(Nation nation) {
        return nation;
    }

    @Override
    public Optional<NationProblemAward> findAward(Long nationId, Long problemId) {
        return Optional.ofNullable(awards.get(nationId + ":" + problemId));
    }

    @Override
    public NationProblemAward saveAward(NationProblemAward award) {
        assignId(award);
        awards.put(award.getNationId() + ":" + award.getProblemId(), award);
        return award;
    }

    @Override
    public NationDailyCounter getOrCreateDailyCounter(Long nationId, LocalDate day) {
        return counters.computeIfAbsent(nationId + ":" + day, key -> {
            NationDailyCounter counter = new NationDailyCounter();
            counter.setNationId(nationId);
            counter.setLocalDay(day);
            assignId(counter);
            return counter;
        });
    }

    @Override
    public NationDailyCounter saveDailyCounter(NationDailyCounter counter) {
        return counter;
    }

    @Override
    public boolean recordActiveDay(Long nationId, LocalDate day) {
        return activeDays.add(nationId + ":" + day);
    }

    @Override
    public long countActiveDays(Long nationId) {
        return activeDays.stream().filter(key -> key.startsWith(nationId + ":")).count();
    }

    @Override
    public NationTrackState getOrCreateTrackState(Long nationId, NationTrack track, String initialItemKey) {
        return trackStates.computeIfAbsent(nationId + ":" + track, key -> {
            NationTrackState state = new NationTrackState();
            state.setNationId(nationId);
            state.setTrack(track);
            state.setCurrentItemKey(initialItemKey);
            assignId(state);
            return state;
        });
    }

    @Override
    public NationTrackState saveTrackState(NationTrackState state) {
        return state;
    }

    @Override
    public long countCompletedItems(Long nationId, String catalogKey) {
        return completedItems.stream()
                .filter(item -> item.getNationId().equals(nationId) && item.getCatalogKey().equals(catalogKey))
                .count();
    }

    @Override
    public NationCompletedItem saveCompletedItem(NationCompletedItem item) {
        assignId(item);
        completedItems.add(item);
        return item;
    }

    @Override
    public NationRankEvent saveRankEvent(NationRankEvent event) {
        assignId(event);
        rankEvents.add(event);
        return event;
    }

    // ---------------- test helpers ----------------

    public int nationCount() {
        return nationsByUser.size();
    }

    void addActiveDays(Long nationId, int count, LocalDate startDay) {
        for (int i = 0; i < count; i++) {
            activeDays.add(nationId + ":" + startDay.plusDays(i));
        }
    }

    List<NationCompletedItem> completed(Long nationId) {
        return completedItems.stream().filter(item -> item.getNationId().equals(nationId)).toList();
    }

    int rankEventCount() {
        return rankEvents.size();
    }

    // ---- read side ----

    @Override
    public Optional<Nation> findNation(Long userId) {
        return Optional.ofNullable(nationsByUser.get(userId));
    }

    @Override
    public List<NationTrackState> findTrackStates(Long nationId) {
        return trackStates.values().stream().filter(state -> state.getNationId().equals(nationId)).toList();
    }

    @Override
    public List<NationCompletedItem> findCompletedItems(Long nationId) {
        return completed(nationId);
    }

    @Override
    public Optional<Long> findUserId(String username) {
        return Optional.ofNullable(userIdsByName.get(username));
    }

    public void registerUser(Long userId, String username) {
        userIdsByName.put(username, userId);
        namesByUserId.put(userId, username);
    }

    @Override
    public void recordLastSeen(Long userId, Instant at) {
        Nation nation = lockOrCreateNation(userId);
        if (nation.getLastSeenAt() == null || at.isAfter(nation.getLastSeenAt())) {
            nation.setLastSeenAt(at);
        }
        lastSeenWrites++;
    }

    public int lastSeenWrites() {
        return lastSeenWrites;
    }

    public Nation nationOrNull(Long userId) {
        return nationsByUser.get(userId);
    }

    @Override
    public Map<Long, Nation> findNationsByUserIds(Collection<Long> userIds) {
        Map<Long, Nation> found = new HashMap<>();
        for (Long userId : userIds) {
            Nation nation = nationsByUser.get(userId);
            if (nation != null) {
                found.put(userId, nation);
            }
        }
        return found;
    }
}

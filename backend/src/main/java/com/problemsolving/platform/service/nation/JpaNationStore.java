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
import com.problemsolving.platform.domain.entity.User;
import com.problemsolving.platform.domain.enums.NationTrack;
import com.problemsolving.platform.repository.CacheOpenLogRepository;
import com.problemsolving.platform.repository.NationActiveDayRepository;
import com.problemsolving.platform.repository.NationCompletedItemRepository;
import com.problemsolving.platform.repository.NationCosmeticOwnedRepository;
import com.problemsolving.platform.repository.NationDailyCounterRepository;
import com.problemsolving.platform.repository.NationProblemAwardRepository;
import com.problemsolving.platform.repository.NationRankEventRepository;
import com.problemsolving.platform.repository.NationRepository;
import com.problemsolving.platform.repository.NationTrackStateRepository;
import com.problemsolving.platform.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Production storage for the Nation system, backed by the Spring Data repositories. */
@Component
public class JpaNationStore implements NationStore {

    private final NationRepository nationRepository;
    private final NationTrackStateRepository trackStateRepository;
    private final NationCompletedItemRepository completedItemRepository;
    private final NationProblemAwardRepository awardRepository;
    private final NationDailyCounterRepository dailyCounterRepository;
    private final NationActiveDayRepository activeDayRepository;
    private final NationRankEventRepository rankEventRepository;
    private final NationCosmeticOwnedRepository ownedCosmeticRepository;
    private final CacheOpenLogRepository cacheOpenLogRepository;
    private final UserRepository userRepository;

    public JpaNationStore(NationRepository nationRepository,
                          NationTrackStateRepository trackStateRepository,
                          NationCompletedItemRepository completedItemRepository,
                          NationProblemAwardRepository awardRepository,
                          NationDailyCounterRepository dailyCounterRepository,
                          NationActiveDayRepository activeDayRepository,
                          NationRankEventRepository rankEventRepository,
                          NationCosmeticOwnedRepository ownedCosmeticRepository,
                          CacheOpenLogRepository cacheOpenLogRepository,
                          UserRepository userRepository) {
        this.nationRepository = nationRepository;
        this.trackStateRepository = trackStateRepository;
        this.completedItemRepository = completedItemRepository;
        this.awardRepository = awardRepository;
        this.dailyCounterRepository = dailyCounterRepository;
        this.activeDayRepository = activeDayRepository;
        this.rankEventRepository = rankEventRepository;
        this.ownedCosmeticRepository = ownedCosmeticRepository;
        this.cacheOpenLogRepository = cacheOpenLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Nation lockOrCreateNation(Long userId) {
        Optional<Nation> existing = nationRepository.findByUserIdForUpdate(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for nation: " + userId));
        Nation nation = new Nation();
        nation.setUserId(userId);
        nation.setName(user.getUsername() + "'s Land");
        nation.setTimeZone("UTC");
        Instant now = Instant.now();
        nation.setCreatedAt(now);
        nation.setUpdatedAt(now);
        return nationRepository.save(nation);
    }

    @Override
    public Nation saveNation(Nation nation) {
        return nationRepository.save(nation);
    }

    @Override
    public Optional<NationProblemAward> findAward(Long nationId, Long problemId) {
        return awardRepository.findByNationIdAndProblemId(nationId, problemId);
    }

    @Override
    public NationProblemAward saveAward(NationProblemAward award) {
        return awardRepository.save(award);
    }

    @Override
    public NationDailyCounter getOrCreateDailyCounter(Long nationId, LocalDate day) {
        return dailyCounterRepository.findByNationIdAndLocalDay(nationId, day).orElseGet(() -> {
            NationDailyCounter counter = new NationDailyCounter();
            counter.setNationId(nationId);
            counter.setLocalDay(day);
            counter.setEasyFirstSolves(0);
            return dailyCounterRepository.save(counter);
        });
    }

    @Override
    public NationDailyCounter saveDailyCounter(NationDailyCounter counter) {
        return dailyCounterRepository.save(counter);
    }

    @Override
    public Optional<NationDailyCounter> findDailyCounter(Long nationId, LocalDate day) {
        return dailyCounterRepository.findByNationIdAndLocalDay(nationId, day);
    }

    @Override
    public boolean recordActiveDay(Long nationId, LocalDate day) {
        if (activeDayRepository.existsByNationIdAndLocalDay(nationId, day)) {
            return false;
        }
        NationActiveDay activeDay = new NationActiveDay();
        activeDay.setNationId(nationId);
        activeDay.setLocalDay(day);
        activeDayRepository.save(activeDay);
        return true;
    }

    @Override
    public long countActiveDays(Long nationId) {
        return activeDayRepository.countByNationId(nationId);
    }

    @Override
    public NationTrackState getOrCreateTrackState(Long nationId, NationTrack track, String initialItemKey) {
        return trackStateRepository.findByNationIdAndTrack(nationId, track).orElseGet(() -> {
            NationTrackState state = new NationTrackState();
            state.setNationId(nationId);
            state.setTrack(track);
            state.setCurrentItemKey(initialItemKey);
            return trackStateRepository.save(state);
        });
    }

    @Override
    public NationTrackState saveTrackState(NationTrackState state) {
        return trackStateRepository.save(state);
    }

    @Override
    public long countCompletedItems(Long nationId, String catalogKey) {
        return completedItemRepository.countByNationIdAndCatalogKey(nationId, catalogKey);
    }

    @Override
    public NationCompletedItem saveCompletedItem(NationCompletedItem item) {
        return completedItemRepository.save(item);
    }

    @Override
    public NationRankEvent saveRankEvent(NationRankEvent event) {
        return rankEventRepository.save(event);
    }

    @Override
    public List<NationCosmeticOwned> findOwnedCosmetics(Long userId) {
        return ownedCosmeticRepository.findByUserId(userId);
    }

    @Override
    public boolean ownsCosmetic(Long userId, String cosmeticKey) {
        return ownedCosmeticRepository.existsByUserIdAndCosmeticKey(userId, cosmeticKey);
    }

    @Override
    public NationCosmeticOwned saveOwnedCosmetic(NationCosmeticOwned owned) {
        return ownedCosmeticRepository.save(owned);
    }

    @Override
    public CacheOpenLog saveCacheOpenLog(CacheOpenLog log) {
        return cacheOpenLogRepository.save(log);
    }

    @Override
    public Optional<Nation> findNation(Long userId) {
        return nationRepository.findByUserId(userId);
    }

    @Override
    public List<NationTrackState> findTrackStates(Long nationId) {
        return trackStateRepository.findByNationId(nationId);
    }

    @Override
    public List<NationCompletedItem> findCompletedItems(Long nationId) {
        return completedItemRepository.findByNationIdOrderByCompletedAtAsc(nationId);
    }

    @Override
    public Map<Long, Nation> findNationsByUserIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return nationRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(Nation::getUserId, nation -> nation));
    }

    @Override
    public Optional<Long> findUserId(String username) {
        return userRepository.findByUsername(username).map(User::getId);
    }

    @Override
    @Transactional
    public void recordLastSeen(Long userId, Instant at) {
        Nation nation = lockOrCreateNation(userId);
        if (nation.getLastSeenAt() == null || at.isAfter(nation.getLastSeenAt())) {
            nation.setLastSeenAt(at);
            nationRepository.save(nation);
        }
    }
}

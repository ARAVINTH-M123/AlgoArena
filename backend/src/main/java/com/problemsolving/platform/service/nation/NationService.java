package com.problemsolving.platform.service.nation;

import com.problemsolving.platform.api.dto.nation.NationResultDto;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.Nation;
import com.problemsolving.platform.domain.entity.NationCompletedItem;
import com.problemsolving.platform.domain.entity.NationDailyCounter;
import com.problemsolving.platform.domain.entity.NationProblemAward;
import com.problemsolving.platform.domain.entity.NationRankEvent;
import com.problemsolving.platform.domain.entity.NationTrackState;
import com.problemsolving.platform.domain.enums.Difficulty;
import com.problemsolving.platform.domain.enums.NationMode;
import com.problemsolving.platform.domain.enums.NationTrack;
import com.problemsolving.platform.service.cosmetics.CosmeticCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Nation game logic. The ONE integration point with the rest of the platform is
 * {@link #awardForAcceptedSolve}: call it when a submission is ACCEPTED.
 */
@Service
public class NationService {

    private static final NationTrack[] TRACKS = NationTrack.values();

    private final NationStore store;
    private final NationProperties props;

    public NationService(NationStore store, NationProperties props) {
        this.store = store;
        this.props = props;
    }

    @Transactional
    public NationResultDto awardForAcceptedSolve(Long userId, Long problemId, Difficulty difficulty, Instant now) {
        if (!props.isAwardEnabled()) {
            return NationResultDto.noOp(NationResultDto.REASON_AWARDS_DISABLED);
        }

        Nation nation = store.lockOrCreateNation(userId);
        Long nationId = nation.getId();
        LocalDate localDay = now.atZone(zoneOf(nation)).toLocalDate();

        // ---- decide FIRST / RESOLVE / NO_OP ----
        NationProblemAward award = store.findAward(nationId, problemId).orElse(null);
        NationMode mode;
        if (award == null) {
            mode = NationMode.FIRST;
        } else {
            Instant resolveAllowedAt = award.getLastAwardedAt().plus(Duration.ofHours(props.getResolveCooldownHours()));
            if (now.isBefore(resolveAllowedAt)) {
                return NationResultDto.noOp(NationResultDto.REASON_ALREADY_COUNTED);
            }
            mode = NationMode.RESOLVE;
        }

        // ---- amounts for this award (micro-points) ----
        long xpGain = props.xpMicro(difficulty);
        long[] gain = new long[TRACKS.length];
        for (NationTrack track : TRACKS) {
            gain[track.ordinal()] = props.trackPointsMicro(difficulty, track);
        }

        if (mode == NationMode.FIRST) {
            if (difficulty == Difficulty.EASY) {
                NationDailyCounter counter = store.getOrCreateDailyCounter(nationId, localDay);
                if (counter.getEasyFirstSolves() >= props.getDailyEasyLimit()) {
                    xpGain /= props.getEasyOverLimitDivisor();
                    for (int i = 0; i < gain.length; i++) {
                        gain[i] /= props.getEasyOverLimitDivisor();
                    }
                }
                counter.setEasyFirstSolves(counter.getEasyFirstSolves() + 1);
                store.saveDailyCounter(counter);
            }
        } else {
            xpGain /= props.getResolveBonusDivisor();
            for (int i = 0; i < gain.length; i++) {
                gain[i] /= props.getResolveBonusDivisor();
            }
        }

        long multiplier = Math.max(1L, props.getXpMultiplier());
        xpGain *= multiplier;
        for (int i = 0; i < gain.length; i++) {
            gain[i] *= multiplier;
        }

        // ---- bookkeeping that differs by mode ----
        if (mode == NationMode.FIRST) {
            switch (difficulty) {
                case EASY -> nation.setEasySolves(nation.getEasySolves() + 1);
                case MEDIUM -> nation.setMediumSolves(nation.getMediumSolves() + 1);
                case HARD -> nation.setHardSolves(nation.getHardSolves() + 1);
            }
            store.recordActiveDay(nationId, localDay);
            nation.setLastSolveAt(now);

            NationProblemAward created = new NationProblemAward();
            created.setNationId(nationId);
            created.setProblemId(problemId);
            created.setDifficulty(difficulty);
            created.setXpAwarded(xpGain);
            created.setFirstAwardedAt(now);
            created.setLastAwardedAt(now);
            created.setResolveBonusCount(0);
            store.saveAward(created);
        } else {
            award.setLastAwardedAt(now);
            award.setResolveBonusCount(award.getResolveBonusCount() + 1);
            store.saveAward(award);
        }

        // ---- add XP and track points ----
        nation.setXp(nation.getXp() + xpGain);
        NationTrackState[] states = new NationTrackState[TRACKS.length];
        for (NationTrack track : TRACKS) {
            NationTrackState state = store.getOrCreateTrackState(
                    nationId, track, NationCatalog.initialItemKey(track));
            state.setProgress(state.getProgress() + gain[track.ordinal()]);
            states[track.ordinal()] = state;
        }

        // ---- complete items and rank up until nothing changes ----
        long activeDays = store.countActiveDays(nationId);
        List<NationCatalog.Item> completed = new ArrayList<>();
        List<Integer> rankUps = new ArrayList<>();
        boolean changed;
        do {
            boolean itemsDone = settleTracks(nation, states, now, completed);
            boolean ranksDone = settleRanks(nation, states, activeDays, now, rankUps);
            changed = itemsDone || ranksDone;
        } while (changed);

        nation.setUpdatedAt(now);
        store.saveNation(nation);
        for (NationTrackState state : states) {
            store.saveTrackState(state);
        }

        return buildResult(mode, nation, states, xpGain, gain, completed, rankUps);
    }

    // ------------------------------------------------------------------ items

    private boolean settleTracks(Nation nation, NationTrackState[] states, Instant now,
                                 List<NationCatalog.Item> completedOut) {
        boolean any = false;
        for (NationTrack track : TRACKS) {
            NationTrackState state = states[track.ordinal()];
            while (true) {
                long cost = props.itemCostMicro(NationRules.contentEra(nation.getRankIndex()), track);
                if (cost <= 0 || state.getProgress() < cost) {
                    break;
                }
                completeItem(nation, state, cost, now, completedOut);
                any = true;
            }
        }
        return any;
    }

    private void completeItem(Nation nation, NationTrackState state, long cost, Instant now,
                              List<NationCatalog.Item> completedOut) {
        NationCatalog.Item item = NationCatalog.find(state.getCurrentItemKey());
        int era = NationRules.contentEra(nation.getRankIndex());

        state.setProgress(state.getProgress() - cost);
        int sequence = state.getSequenceNumber() + 1;
        state.setSequenceNumber(sequence);
        state.setItemsCompleted(state.getItemsCompleted() + 1);

        NationCompletedItem record = new NationCompletedItem();
        record.setNationId(nation.getId());
        record.setCatalogKey(item.key());
        record.setTrack(state.getTrack());
        record.setContentEra(era);
        record.setSequenceNumber(sequence);
        record.setInstanceNumber((int) store.countCompletedItems(nation.getId(), item.key()) + 1);
        record.setCompletedAt(now);
        store.saveCompletedItem(record);
        completedOut.add(item);

        if (state.getTrack() == NationTrack.LANDMARKS && item.kind() == NationCatalog.Kind.GATE) {
            state.setGateDone(true);
            state.setEraCursor(0);
            if (nation.getRankIndex() >= NationProperties.MAX_RANK_INDEX) {
                nation.setAscended(true);
            }
        } else {
            state.setEraCursor(state.getEraCursor() + 1);
        }
        state.setCurrentItemKey(nextItemKey(nation.getRankIndex(), state));
    }

    /** Next item in this track's fixed queue for the nation's current rank / content era. */
    private String nextItemKey(int rankIndex, NationTrackState state) {
        int era = NationRules.contentEra(rankIndex);
        if (state.getTrack() == NationTrack.LANDMARKS && !state.isGateDone()) {
            return NationCatalog.gate(rankIndex).key();
        }
        List<NationCatalog.Item> list = NationCatalog.items(era, state.getTrack());
        return list.get(state.getEraCursor() % list.size()).key();
    }

    // ------------------------------------------------------------------ ranks

    private boolean settleRanks(Nation nation, NationTrackState[] states, long activeDays, Instant now,
                                List<Integer> rankUpsOut) {
        NationTrackState landmarks = states[NationTrack.LANDMARKS.ordinal()];
        boolean ranked = false;

        while (nation.getRankIndex() < NationProperties.MAX_RANK_INDEX) {
            int next = nation.getRankIndex() + 1;
            boolean ok = nation.getXp() >= props.rankXpMicro(next)
                    && nation.getMediumSolves() >= props.rankMinMediums(next)
                    && nation.getHardSolves() >= props.rankMinHards(next)
                    && activeDays >= props.rankMinActiveDays(next)
                    && landmarks.isGateDone();
            if (!ok) {
                break;
            }

            int oldEra = NationRules.contentEra(nation.getRankIndex());
            nation.setRankIndex(next);
            int newEra = NationRules.contentEra(next);

            NationRankEvent event = new NationRankEvent();
            event.setNationId(nation.getId());
            event.setRankIndex(next);
            event.setReachedAt(now);
            store.saveRankEvent(event);
            rankUpsOut.add(next);

            // landmarks restart at the new rank's gate landmark; progress carries over
            landmarks.setGateDone(false);
            landmarks.setEraCursor(0);
            landmarks.setCurrentItemKey(NationCatalog.gate(next).key());

            // a new content era restarts the Basics and Structures queues from their first item
            if (newEra != oldEra) {
                for (NationTrack track : new NationTrack[]{NationTrack.BASICS, NationTrack.STRUCTURES}) {
                    NationTrackState state = states[track.ordinal()];
                    state.setEraCursor(0);
                    state.setCurrentItemKey(NationCatalog.items(newEra, track).get(0).key());
                }
            }
            ranked = true;
        }
        return ranked;
    }

    // ------------------------------------------------------------------ result

    private NationResultDto buildResult(NationMode mode, Nation nation, NationTrackState[] states,
                                        long xpGain, long[] gain, List<NationCatalog.Item> completed,
                                        List<Integer> rankUps) {
        int era = NationRules.contentEra(nation.getRankIndex());

        List<NationResultDto.CompletedItem> completedDto = new ArrayList<>();
        for (NationCatalog.Item item : completed) {
            completedDto.add(new NationResultDto.CompletedItem(item.track().name(), item.key(), item.name()));
        }

        List<String> rankUpNames = new ArrayList<>();
        for (int rank : rankUps) {
            rankUpNames.add(props.rankName(rank));
        }

        List<NationResultDto.TrackProgress> tracks = new ArrayList<>();
        for (NationTrack track : TRACKS) {
            NationTrackState state = states[track.ordinal()];
            long cost = props.itemCostMicro(era, track);
            tracks.add(new NationResultDto.TrackProgress(
                    track.name(),
                    state.getCurrentItemKey(),
                    NationCatalog.find(state.getCurrentItemKey()).name(),
                    state.getProgress(),
                    cost,
                    NationRules.stage(state.getProgress(), cost)));
        }

        return new NationResultDto(
                mode.name(),
                xpGain,
                gain[NationTrack.BASICS.ordinal()],
                gain[NationTrack.STRUCTURES.ordinal()],
                gain[NationTrack.LANDMARKS.ordinal()],
                completedDto,
                rankUpNames,
                props.rankName(nation.getRankIndex()),
                NationRules.tierIndex(nation.getXp(), nation.getRankIndex(), props) + 1,
                nation.getXp(),
                tracks,
                null,
                unlockedCosmetics(rankUps));
    }

    private static List<NationResultDto.UnlockedCosmetic> unlockedCosmetics(List<Integer> rankUps) {
        List<NationResultDto.UnlockedCosmetic> unlocked = new ArrayList<>();
        for (CosmeticCatalog.Item item : CosmeticCatalog.unlockedAtRanks(rankUps)) {
            unlocked.add(new NationResultDto.UnlockedCosmetic(item.category().name(), item.key(), item.displayName()));
        }
        return unlocked;
    }

    private ZoneId zoneOf(Nation nation) {
        try {
            return ZoneId.of(nation.getTimeZone());
        } catch (DateTimeException | NullPointerException ex) {
            return ZoneId.of("UTC");
        }
    }
}

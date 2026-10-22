package com.problemsolving.platform.service;

import com.problemsolving.platform.api.dto.nation.*;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.*;
import com.problemsolving.platform.domain.enums.Difficulty;
import com.problemsolving.platform.domain.enums.NationPresenceStatus;
import com.problemsolving.platform.domain.enums.NationRank;
import com.problemsolving.platform.domain.enums.NationTrack;
import com.problemsolving.platform.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class NationService {

    private final NationRepository nationRepository;
    private final NationCatalogItemRepository nationCatalogItemRepository;
    private final NationCompletedItemRepository nationCompletedItemRepository;
    private final NationProblemProgressRepository nationProblemProgressRepository;
    private final NationDailyEasyCounterRepository nationDailyEasyCounterRepository;
    private final UserRepository userRepository;
    private final NationProperties nationProperties;

    public NationService(NationRepository nationRepository,
                         NationCatalogItemRepository nationCatalogItemRepository,
                         NationCompletedItemRepository nationCompletedItemRepository,
                         NationProblemProgressRepository nationProblemProgressRepository,
                         NationDailyEasyCounterRepository nationDailyEasyCounterRepository,
                         UserRepository userRepository,
                         NationProperties nationProperties) {
        this.nationRepository = nationRepository;
        this.nationCatalogItemRepository = nationCatalogItemRepository;
        this.nationCompletedItemRepository = nationCompletedItemRepository;
        this.nationProblemProgressRepository = nationProblemProgressRepository;
        this.nationDailyEasyCounterRepository = nationDailyEasyCounterRepository;
        this.userRepository = userRepository;
        this.nationProperties = nationProperties;
    }

    @Transactional
    public NationAwardSummaryDto awardForAcceptedSubmission(User user, Problem problem, LocalDateTime solvedAt) {
        Nation nation = findOrCreateNation(user);

        if (nationProblemProgressRepository.existsByNationIdAndProblemId(nation.getId(), problem.getId())) {
            return new NationAwardSummaryDto(
                    false,
                    0,
                    0,
                    0,
                    0,
                    nation.getRank().getDisplayName(),
                    nation.getRank().getDisplayName(),
                    List.of());
        }

        double dayMultiplier = resolveDailyMultiplier(nation, problem.getDifficulty(), solvedAt.toLocalDate());
        int xpGain = scaledReward(nationProperties.getXpRewards().get(problem.getDifficulty()), dayMultiplier);

        NationProperties.TrackReward trackReward = nationProperties.getTrackRewards().get(problem.getDifficulty());
        int basicsGain = scaledReward(trackReward.basics(), dayMultiplier);
        int structuresGain = scaledReward(trackReward.structures(), dayMultiplier);
        int landmarksGain = scaledReward(trackReward.landmarks(), dayMultiplier);

        nation.setXp(nation.getXp() + xpGain);
        if (problem.getDifficulty() == Difficulty.MEDIUM) {
            nation.setMediumFirstSolves(nation.getMediumFirstSolves() + 1);
        }
        if (problem.getDifficulty() == Difficulty.HARD) {
            nation.setHardFirstSolves(nation.getHardFirstSolves() + 1);
        }

        String rankBefore = nation.getRank().getDisplayName();

        List<String> completedItems = new ArrayList<>();
        completedItems.addAll(applyTrackProgress(nation, NationTrack.BASICS, basicsGain, solvedAt));
        completedItems.addAll(applyTrackProgress(nation, NationTrack.STRUCTURES, structuresGain, solvedAt));
        completedItems.addAll(applyTrackProgress(nation, NationTrack.LANDMARKS, landmarksGain, solvedAt));

        NationRank nextRank = resolveRank(nation);
        if (nextRank.ordinal() > nation.getRank().ordinal()) {
            nation.setRank(nextRank);
            resetTrackStateForRankChange(nation);
        }

        nationRepository.save(nation);

        NationProblemProgress progress = new NationProblemProgress();
        progress.setNation(nation);
        progress.setProblem(problem);
        progress.setDifficulty(problem.getDifficulty());
        progress.setAwardedAt(solvedAt);
        progress.setXpAwarded(xpGain);
        progress.setBasicsPointsAwarded(basicsGain);
        progress.setStructuresPointsAwarded(structuresGain);
        progress.setLandmarksPointsAwarded(landmarksGain);
        nationProblemProgressRepository.save(progress);

        return new NationAwardSummaryDto(
                true,
                xpGain,
                basicsGain,
                structuresGain,
                landmarksGain,
                rankBefore,
                nation.getRank().getDisplayName(),
                completedItems
        );
    }

    @Transactional(readOnly = true)
    public NationViewDto getByUsername(String username) {
        Nation nation = nationRepository.findByUserUsername(username)
                .orElseGet(() -> {
                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                    return buildDefaultNation(user);
                });
        return toViewDto(nation);
    }

    @Transactional(readOnly = true)
    public NationViewDto getForCurrentUser(String username) {
        Nation nation = nationRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nation not found"));
        return toViewDto(nation);
    }

    private Nation findOrCreateNation(User user) {
        return nationRepository.findByUserId(user.getId())
                .orElseGet(() -> nationRepository.save(buildDefaultNation(user)));
    }

    private Nation buildDefaultNation(User user) {
        Nation nation = new Nation();
        nation.setUser(user);
        nation.setRank(NationRank.NULL_RANK);
        nation.setXp(0L);
        nation.setMediumFirstSolves(0);
        nation.setHardFirstSolves(0);
        nation.setBasicsProgress(0);
        nation.setStructuresProgress(0);
        nation.setLandmarksProgress(0);
        nation.setCurrentBasicsItemOrder(1);
        nation.setCurrentStructuresItemOrder(1);
        nation.setCurrentLandmarksItemOrder(1);
        nation.setAppearOffline(false);
        nation.setLastSeenAt(null);
        return nation;
    }

    private int scaledReward(int base, double dayMultiplier) {
        double scaled = base * nationProperties.getXpMultiplier() * dayMultiplier;
        return Math.max(0, (int) Math.round(scaled));
    }

    private double resolveDailyMultiplier(Nation nation, Difficulty difficulty, LocalDate solveDate) {
        if (difficulty != Difficulty.EASY) {
            return 1.0;
        }

        NationDailyEasyCounter counter = nationDailyEasyCounterRepository
                .findByNationIdAndSolveDate(nation.getId(), solveDate)
                .orElseGet(() -> {
                    NationDailyEasyCounter created = new NationDailyEasyCounter();
                    created.setNation(nation);
                    created.setSolveDate(solveDate);
                    created.setEasyFirstSolves(0);
                    return created;
                });

        int countBefore = counter.getEasyFirstSolves();
        counter.setEasyFirstSolves(countBefore + 1);
        nationDailyEasyCounterRepository.save(counter);

        if (countBefore >= nationProperties.getDailyEasyFirstSolveLimit()) {
            return nationProperties.getDailyEasyPostLimitPercent() / 100.0;
        }
        return 1.0;
    }

    private List<String> applyTrackProgress(Nation nation, NationTrack track, int points, LocalDateTime solvedAt) {
        List<String> completed = new ArrayList<>();
        if (points <= 0) {
            return completed;
        }

        int era = nation.getRank().era();
        int progress = getProgress(nation, track) + points;
        int currentOrder = getCurrentOrder(nation, track);

        Optional<NationCatalogItem> currentItem = nationCatalogItemRepository.findByEraAndTrackAndItemOrder(era, track, currentOrder);
        while (currentItem.isPresent() && progress >= currentItem.get().getCost()) {
            NationCatalogItem completedItem = currentItem.get();
            progress -= completedItem.getCost();

            NationCompletedItem saveCompleted = new NationCompletedItem();
            saveCompleted.setNation(nation);
            saveCompleted.setCatalogItem(completedItem);
            saveCompleted.setTrack(track);
            saveCompleted.setEra(completedItem.getEra());
            saveCompleted.setName(completedItem.getName());
            saveCompleted.setCost(completedItem.getCost());
            saveCompleted.setCompletedAt(solvedAt);
            nationCompletedItemRepository.save(saveCompleted);
            completed.add(completedItem.getName());

            currentOrder += 1;
            currentItem = nationCatalogItemRepository.findByEraAndTrackAndItemOrder(era, track, currentOrder);
        }

        setProgress(nation, track, progress);
        setCurrentOrder(nation, track, currentOrder);
        return completed;
    }

    private void resetTrackStateForRankChange(Nation nation) {
        nation.setBasicsProgress(0);
        nation.setStructuresProgress(0);
        nation.setLandmarksProgress(0);
        nation.setCurrentBasicsItemOrder(1);
        nation.setCurrentStructuresItemOrder(1);
        nation.setCurrentLandmarksItemOrder(1);
    }

    private NationRank resolveRank(Nation nation) {
        NationRank best = NationRank.NULL_RANK;
        for (NationProperties.RankRequirement requirement : nationProperties.getRankRequirements()) {
            if (nation.getXp() >= requirement.xpThreshold()
                    && nation.getMediumFirstSolves() >= requirement.minMediumSolves()
                    && nation.getHardFirstSolves() >= requirement.minHardSolves()) {
                best = requirement.rank();
            }
        }
        return best;
    }

    private int getProgress(Nation nation, NationTrack track) {
        return switch (track) {
            case BASICS -> nation.getBasicsProgress();
            case STRUCTURES -> nation.getStructuresProgress();
            case LANDMARKS -> nation.getLandmarksProgress();
        };
    }

    private void setProgress(Nation nation, NationTrack track, int progress) {
        switch (track) {
            case BASICS -> nation.setBasicsProgress(progress);
            case STRUCTURES -> nation.setStructuresProgress(progress);
            case LANDMARKS -> nation.setLandmarksProgress(progress);
        }
    }

    private int getCurrentOrder(Nation nation, NationTrack track) {
        return switch (track) {
            case BASICS -> nation.getCurrentBasicsItemOrder();
            case STRUCTURES -> nation.getCurrentStructuresItemOrder();
            case LANDMARKS -> nation.getCurrentLandmarksItemOrder();
        };
    }

    private void setCurrentOrder(Nation nation, NationTrack track, int order) {
        switch (track) {
            case BASICS -> nation.setCurrentBasicsItemOrder(order);
            case STRUCTURES -> nation.setCurrentStructuresItemOrder(order);
            case LANDMARKS -> nation.setCurrentLandmarksItemOrder(order);
        }
    }

    private NationViewDto toViewDto(Nation nation) {
        NationRank rank = resolveRank(nation);
        NationProperties.RankRequirement currentRequirement = requirementFor(rank);
        NationRank nextRank = rank.next();

        long xpToNext = 0L;
        NationMissingRequirementsDto missing = new NationMissingRequirementsDto(0L, 0, 0);
        if (nextRank != null) {
            NationProperties.RankRequirement nextRequirement = requirementFor(nextRank);
            xpToNext = Math.max(0L, nextRequirement.xpThreshold() - nation.getXp());
            missing = new NationMissingRequirementsDto(
                    Math.max(0L, nextRequirement.xpThreshold() - nation.getXp()),
                    Math.max(0, nextRequirement.minMediumSolves() - nation.getMediumFirstSolves()),
                    Math.max(0, nextRequirement.minHardSolves() - nation.getHardFirstSolves())
            );
        }

        String subTier = computeSubTier(rank, nation.getXp());
        NationTrackProgressDto basics = buildTrackProgress(nation, NationTrack.BASICS);
        NationTrackProgressDto structures = buildTrackProgress(nation, NationTrack.STRUCTURES);
        NationTrackProgressDto landmarks = buildTrackProgress(nation, NationTrack.LANDMARKS);

        List<NationCompletedItemDto> completedItems = nation.getId() == null ? List.of() : nationCompletedItemRepository
                .findByNationIdOrderByCompletedAtDesc(nation.getId())
                .stream()
                .map(item -> new NationCompletedItemDto(
                        item.getTrack().name(),
                        item.getEra(),
                        item.getName(),
                        item.getCost(),
                        item.getCompletedAt()))
                .toList();

        NationPresenceDto presence = buildPresence(nation);

        return new NationViewDto(
                nation.getUser().getUsername(),
                rank.getDisplayName(),
                subTier,
                rank.getDisplayName() + " " + subTier,
                nation.getXp(),
                xpToNext,
                missing,
                basics,
                structures,
                landmarks,
                completedItems,
                presence
        );
    }

    private String computeSubTier(NationRank rank, long xp) {
        NationRank next = rank.next();
        if (next == null) {
            return "III";
        }

        NationProperties.RankRequirement current = requirementFor(rank);
        NationProperties.RankRequirement target = requirementFor(next);
        long range = Math.max(1, target.xpThreshold() - current.xpThreshold());
        long step = Math.max(1, range / 3);
        long delta = Math.max(0, xp - current.xpThreshold());

        if (delta < step) {
            return "I";
        }
        if (delta < step * 2) {
            return "II";
        }
        return "III";
    }

    private NationTrackProgressDto buildTrackProgress(Nation nation, NationTrack track) {
        int progress = getProgress(nation, track);
        int order = getCurrentOrder(nation, track);
        int era = nation.getRank().era();

        Optional<NationCatalogItem> item = nationCatalogItemRepository.findByEraAndTrackAndItemOrder(era, track, order);
        if (item.isEmpty()) {
            return new NationTrackProgressDto(track.name(), "Era completed", progress, 0, 100, "done");
        }

        NationCatalogItem current = item.get();
        int cost = Math.max(1, current.getCost());
        int percent = Math.min(100, (progress * 100) / cost);

        return new NationTrackProgressDto(
                track.name(),
                current.getName(),
                progress,
                cost,
                percent,
                stageFromPercent(percent)
        );
    }

    private String stageFromPercent(int percent) {
        if (percent >= 100) {
            return "done";
        }
        if (percent >= 75) {
            return "roof";
        }
        if (percent >= 50) {
            return "walls";
        }
        if (percent >= 25) {
            return "frame";
        }
        return "foundation";
    }

    private NationPresenceDto buildPresence(Nation nation) {
        NationPresenceStatus status = resolvePresenceStatus(nation);
        LocalDateTime lastSeenAt = nation.getLastSeenAt();
        String text;

        if (status == NationPresenceStatus.ONLINE) {
            text = "Online";
        } else if (status == NationPresenceStatus.IDLE) {
            text = "Idle";
        } else if (lastSeenAt != null) {
            text = "Last active " + formatAgo(lastSeenAt);
        } else {
            text = "Offline";
        }

        return new NationPresenceDto(status.name(), text, lastSeenAt, Boolean.TRUE.equals(nation.getAppearOffline()));
    }

    private NationPresenceStatus resolvePresenceStatus(Nation nation) {
        if (Boolean.TRUE.equals(nation.getAppearOffline())) {
            return NationPresenceStatus.OFFLINE;
        }
        if (nation.getLastSeenAt() == null) {
            return NationPresenceStatus.OFFLINE;
        }

        Duration elapsed = Duration.between(nation.getLastSeenAt(), LocalDateTime.now());
        if (elapsed.toMinutes() < 2) {
            return NationPresenceStatus.ONLINE;
        }
        if (elapsed.toMinutes() < 10) {
            return NationPresenceStatus.IDLE;
        }
        return NationPresenceStatus.OFFLINE;
    }

    private String formatAgo(LocalDateTime lastSeenAt) {
        Duration elapsed = Duration.between(lastSeenAt, LocalDateTime.now());
        long minutes = Math.max(0, elapsed.toMinutes());
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h ago";
        }
        long days = hours / 24;
        return days + "d ago";
    }

    private NationProperties.RankRequirement requirementFor(NationRank rank) {
        return nationProperties.getRankRequirements().stream()
                .filter(requirement -> requirement.rank() == rank)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing rank requirement for " + rank));
    }
}

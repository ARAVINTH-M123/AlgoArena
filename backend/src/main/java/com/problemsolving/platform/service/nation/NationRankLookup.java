package com.problemsolving.platform.service.nation;

import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.Nation;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/** Nation name and rank for lists of users (the leaderboard). Users without a nation count as rank Null. */
@Service
public class NationRankLookup {

    /** tier is 1..3 (shown as I, II, III). */
    public record Info(String nationName, int rankIndex, String rankName, int tier) {
    }

    private final NationStore store;
    private final NationProperties props;

    public NationRankLookup(NationStore store, NationProperties props) {
        this.store = store;
        this.props = props;
    }

    /** @param usernamesByUserId every user to describe, with the username used for the default nation name */
    public Map<Long, Info> describe(Map<Long, String> usernamesByUserId) {
        Map<Long, Nation> nations = store.findNationsByUserIds(usernamesByUserId.keySet());
        Map<Long, Info> result = new HashMap<>();
        for (Map.Entry<Long, String> entry : usernamesByUserId.entrySet()) {
            Nation nation = nations.get(entry.getKey());
            if (nation == null) {
                result.put(entry.getKey(), new Info(entry.getValue() + "'s Land", 0, props.rankName(0), 1));
            } else {
                int rank = nation.getRankIndex();
                result.put(entry.getKey(), new Info(
                        nation.getName(),
                        rank,
                        props.rankName(rank),
                        NationRules.tierIndex(nation.getXp(), rank, props) + 1));
            }
        }
        return result;
    }
}

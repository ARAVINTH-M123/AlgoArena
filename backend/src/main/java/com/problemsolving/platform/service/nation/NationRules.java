package com.problemsolving.platform.service.nation;

import com.problemsolving.platform.config.NationProperties;

/** Small pure helpers shared by the nation logic and (later) the read API. */
public final class NationRules {

    private NationRules() {
    }

    /** Content era (1..5) that decides which items exist and what they cost. */
    public static int contentEra(int rankIndex) {
        if (rankIndex <= 2) return 1;
        if (rankIndex <= 4) return 2;
        if (rankIndex <= 6) return 3;
        if (rankIndex <= 8) return 4;
        return 5;
    }

    /**
     * Sub-tier index 0..2 (shown as I, II, III). Ranks 1-9 split the XP range up to the next rank into
     * three equal parts; Singularity uses a fixed spacing.
     */
    public static int tierIndex(long xpMicro, int rankIndex, NationProperties props) {
        long low = props.rankXpMicro(rankIndex);
        long offset = Math.max(0L, xpMicro - low);
        long index;
        if (rankIndex >= NationProperties.MAX_RANK_INDEX) {
            index = offset / props.singularityTierSpacingMicro();
        } else {
            long span = props.rankXpMicro(rankIndex + 1) - low;
            index = (offset * 3L) / span;
        }
        return (int) Math.min(2L, Math.max(0L, index));
    }

    /** Construction stage 0..3 for display: foundation, frame, walls, roof. */
    public static int stage(long progressMicro, long costMicro) {
        if (costMicro <= 0) return 0;
        long stage = (progressMicro * 4L) / costMicro;
        return (int) Math.min(3L, Math.max(0L, stage));
    }
}

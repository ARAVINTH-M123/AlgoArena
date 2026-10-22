package com.problemsolving.platform.domain.enums;

public enum NationRank {
    NULL_RANK("Null"),
    INIT("Init"),
    BOOT("Boot"),
    ROOT("Root"),
    KERNEL("Kernel"),
    STACK("Stack"),
    CLOUD("Cloud"),
    CORE("Core"),
    QUANTUM("Quantum"),
    SINGULARITY("Singularity");

    private final String displayName;

    NationRank(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int era() {
        return ordinal() + 1;
    }

    public NationRank next() {
        int nextIndex = ordinal() + 1;
        NationRank[] ranks = values();
        if (nextIndex >= ranks.length) {
            return null;
        }
        return ranks[nextIndex];
    }
}

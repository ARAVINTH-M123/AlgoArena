package com.problemsolving.platform.config;

import com.problemsolving.platform.domain.enums.Difficulty;
import com.problemsolving.platform.domain.enums.NationRank;
import com.problemsolving.platform.domain.enums.NationTrack;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "nation")
public class NationProperties {

    private double xpMultiplier = 1.0;
    private int dailyEasyFirstSolveLimit = 5;
    private int dailyEasyPostLimitPercent = 50;
    private Map<Difficulty, Integer> xpRewards = defaultXpRewards();
    private Map<Difficulty, TrackReward> trackRewards = defaultTrackRewards();
    private List<RankRequirement> rankRequirements = defaultRankRequirements();
    private Map<NationRank, TrackCost> trackCostsByRank = defaultTrackCostsByRank();
    private List<CatalogItemDefinition> catalog = defaultCatalog();

    public double getXpMultiplier() {
        return xpMultiplier;
    }

    public void setXpMultiplier(double xpMultiplier) {
        this.xpMultiplier = xpMultiplier;
    }

    public int getDailyEasyFirstSolveLimit() {
        return dailyEasyFirstSolveLimit;
    }

    public void setDailyEasyFirstSolveLimit(int dailyEasyFirstSolveLimit) {
        this.dailyEasyFirstSolveLimit = dailyEasyFirstSolveLimit;
    }

    public int getDailyEasyPostLimitPercent() {
        return dailyEasyPostLimitPercent;
    }

    public void setDailyEasyPostLimitPercent(int dailyEasyPostLimitPercent) {
        this.dailyEasyPostLimitPercent = dailyEasyPostLimitPercent;
    }

    public Map<Difficulty, Integer> getXpRewards() {
        return xpRewards;
    }

    public void setXpRewards(Map<Difficulty, Integer> xpRewards) {
        this.xpRewards = xpRewards;
    }

    public Map<Difficulty, TrackReward> getTrackRewards() {
        return trackRewards;
    }

    public void setTrackRewards(Map<Difficulty, TrackReward> trackRewards) {
        this.trackRewards = trackRewards;
    }

    public List<RankRequirement> getRankRequirements() {
        return rankRequirements;
    }

    public void setRankRequirements(List<RankRequirement> rankRequirements) {
        this.rankRequirements = rankRequirements;
    }

    public Map<NationRank, TrackCost> getTrackCostsByRank() {
        return trackCostsByRank;
    }

    public void setTrackCostsByRank(Map<NationRank, TrackCost> trackCostsByRank) {
        this.trackCostsByRank = trackCostsByRank;
    }

    public List<CatalogItemDefinition> getCatalog() {
        return catalog;
    }

    public void setCatalog(List<CatalogItemDefinition> catalog) {
        this.catalog = catalog;
    }

    private static Map<Difficulty, Integer> defaultXpRewards() {
        Map<Difficulty, Integer> map = new EnumMap<>(Difficulty.class);
        map.put(Difficulty.EASY, 10);
        map.put(Difficulty.MEDIUM, 30);
        map.put(Difficulty.HARD, 70);
        return map;
    }

    private static Map<Difficulty, TrackReward> defaultTrackRewards() {
        Map<Difficulty, TrackReward> map = new EnumMap<>(Difficulty.class);
        map.put(Difficulty.EASY, new TrackReward(10, 4, 1));
        map.put(Difficulty.MEDIUM, new TrackReward(5, 10, 4));
        map.put(Difficulty.HARD, new TrackReward(3, 8, 10));
        return map;
    }

    private static List<RankRequirement> defaultRankRequirements() {
        List<RankRequirement> list = new ArrayList<>();
        list.add(new RankRequirement(NationRank.NULL_RANK, 0, 0, 0));
        list.add(new RankRequirement(NationRank.INIT, 900, 0, 0));
        list.add(new RankRequirement(NationRank.BOOT, 4500, 0, 0));
        list.add(new RankRequirement(NationRank.ROOT, 12000, 0, 0));
        list.add(new RankRequirement(NationRank.KERNEL, 27000, 100, 0));
        list.add(new RankRequirement(NationRank.STACK, 51000, 250, 0));
        list.add(new RankRequirement(NationRank.CLOUD, 87000, 500, 0));
        list.add(new RankRequirement(NationRank.CORE, 135000, 900, 50));
        list.add(new RankRequirement(NationRank.QUANTUM, 204000, 1800, 600));
        list.add(new RankRequirement(NationRank.SINGULARITY, 285000, 3400, 1700));
        return list;
    }

    private static Map<NationRank, TrackCost> defaultTrackCostsByRank() {
        Map<NationRank, TrackCost> map = new EnumMap<>(NationRank.class);
        map.put(NationRank.NULL_RANK, new TrackCost(50, 80, 100));
        map.put(NationRank.INIT, new TrackCost(50, 80, 100));
        map.put(NationRank.BOOT, new TrackCost(50, 80, 100));
        map.put(NationRank.ROOT, new TrackCost(80, 140, 200));
        map.put(NationRank.KERNEL, new TrackCost(80, 140, 200));
        map.put(NationRank.STACK, new TrackCost(120, 220, 350));
        map.put(NationRank.CLOUD, new TrackCost(120, 220, 350));
        map.put(NationRank.CORE, new TrackCost(180, 320, 550));
        map.put(NationRank.QUANTUM, new TrackCost(180, 320, 550));
        map.put(NationRank.SINGULARITY, new TrackCost(250, 450, 800));
        return map;
    }

    private static List<CatalogItemDefinition> defaultCatalog() {
        List<CatalogItemDefinition> items = new ArrayList<>();
        addTrackItems(items, 1, NationTrack.BASICS, List.of("Grass Patch", "Tree", "Dirt Path"));
        addTrackItems(items, 1, NationTrack.STRUCTURES, List.of("Tent", "Campfire", "Well"));
        addTrackItems(items, 1, NationTrack.LANDMARKS, List.of("Watchtower", "Town Hall", "Windmill"));

        addTrackItems(items, 2, NationTrack.BASICS, List.of("Fence", "Barrel", "Lamp"));
        addTrackItems(items, 2, NationTrack.STRUCTURES, List.of("Hut", "Farm Plot", "Wooden House"));
        addTrackItems(items, 2, NationTrack.LANDMARKS, List.of("Clock Tower", "Castle", "Stone Arch"));

        addTrackItems(items, 3, NationTrack.BASICS, List.of("Flower Bed", "Cobblestone Road", "Bench"));
        addTrackItems(items, 3, NationTrack.STRUCTURES, List.of("Bakery", "Market Stall", "Stone House"));
        addTrackItems(items, 3, NationTrack.LANDMARKS, List.of("Grand Windmill", "Old Keep", "Signal Spire"));

        addTrackItems(items, 4, NationTrack.BASICS, List.of("Stone Marker", "Lantern Row", "Garden Path"));
        addTrackItems(items, 4, NationTrack.STRUCTURES, List.of("Bridge", "School", "Guild Hall"));
        addTrackItems(items, 4, NationTrack.LANDMARKS, List.of("Clock Citadel", "Royal Gate", "River Monument"));

        addTrackItems(items, 5, NationTrack.BASICS, List.of("Brick Walk", "Signal Lamp", "Steel Fence"));
        addTrackItems(items, 5, NationTrack.STRUCTURES, List.of("Brick Tower", "Workshop", "Factory"));
        addTrackItems(items, 5, NationTrack.LANDMARKS, List.of("Railway Station", "Grand Foundry", "Sky Bell"));

        addTrackItems(items, 6, NationTrack.BASICS, List.of("Neon Trail", "Power Line", "Rain Garden"));
        addTrackItems(items, 6, NationTrack.STRUCTURES, List.of("Transit Hub", "Data Mill", "Hydro Plant"));
        addTrackItems(items, 6, NationTrack.LANDMARKS, List.of("Skyscraper", "Aero Dock", "Sky Bridge"));

        addTrackItems(items, 7, NationTrack.BASICS, List.of("Cloud Garden", "Solar Path", "Drone Lane"));
        addTrackItems(items, 7, NationTrack.STRUCTURES, List.of("Airport", "Cloud Forge", "Server Arc"));
        addTrackItems(items, 7, NationTrack.LANDMARKS, List.of("Solar Grid", "Orbital Spire", "Nimbus Gate"));

        addTrackItems(items, 8, NationTrack.BASICS, List.of("Quantum Moss", "Pulse Path", "Smart Canopy"));
        addTrackItems(items, 8, NationTrack.STRUCTURES, List.of("Drone Hub", "Fusion Works", "Arc Library"));
        addTrackItems(items, 8, NationTrack.LANDMARKS, List.of("Floating District", "Aurora Tower", "Beam Gate"));

        addTrackItems(items, 9, NationTrack.BASICS, List.of("Photon Grove", "Hyper Rail", "Pulse Beacon"));
        addTrackItems(items, 9, NationTrack.STRUCTURES, List.of("Sky Port", "Ion Bazaar", "Lattice Core"));
        addTrackItems(items, 9, NationTrack.LANDMARKS, List.of("Teleport Gate", "Starline Bastion", "Aether Crown"));

        addTrackItems(items, 10, NationTrack.BASICS, List.of("Stellar Bloom", "Orbit Path", "Nova Lantern"));
        addTrackItems(items, 10, NationTrack.STRUCTURES, List.of("Space Port", "Chrono Forge", "Singularity Lab"));
        addTrackItems(items, 10, NationTrack.LANDMARKS, List.of("Wormhole Nexus", "Event Horizon Ring", "Celestial Throne"));
        return items;
    }

    private static void addTrackItems(List<CatalogItemDefinition> items, int era, NationTrack track, List<String> names) {
        for (int i = 0; i < names.size(); i++) {
            items.add(new CatalogItemDefinition(era, track, names.get(i), i + 1));
        }
    }

    public record TrackReward(int basics, int structures, int landmarks) {
    }

    public record RankRequirement(NationRank rank, long xpThreshold, int minMediumSolves, int minHardSolves) {
    }

    public record TrackCost(int basics, int structures, int landmarks) {
    }

    public record CatalogItemDefinition(int era, NationTrack track, String name, int itemOrder) {
    }
}

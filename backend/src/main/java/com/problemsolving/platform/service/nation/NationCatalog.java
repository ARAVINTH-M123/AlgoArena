package com.problemsolving.platform.service.nation;

import com.problemsolving.platform.domain.enums.NationTrack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The ordered item catalog (blueprint section 5.5). Kept in code, not in the database, so every nation
 * builds exactly the same items in exactly the same order. Item cost is NOT stored here: it comes from
 * NationProperties by content era and track.
 */
public final class NationCatalog {

    public enum Kind { NORMAL, GATE, BONUS }

    public record Item(String key, NationTrack track, int era, Kind kind, Integer gateRank, String name) {
    }

    private static final Map<String, Item> BY_KEY = new LinkedHashMap<>();
    private static final Map<String, List<Item>> LISTS = new LinkedHashMap<>();
    private static final Item[] GATES = new Item[10];

    private static final String[][] BASICS = {
            {"grass patch", "tree", "rock", "dirt path", "fence", "barrel", "flower bed"},
            {"cobblestone road", "lamp post", "bench", "signpost", "hay bale", "market crate"},
            {"traffic light", "sidewalk", "billboard", "bus stop", "street trees", "power lines"},
            {"solar panel", "sensor post", "drone pad", "light path", "holo-sign", "hover pad"},
            {"energy lamp", "plasma path", "holo-garden", "portal marker"}};

    private static final String[][] STRUCTURES = {
            {"tent", "campfire", "well", "storage shed", "hut", "farm plot"},
            {"wooden house", "bakery", "market stall", "stone house", "bridge", "school"},
            {"apartments", "factory", "train track", "skyscraper", "mall", "hospital"},
            {"smart home", "charging hub", "tech lab", "sky pod", "floating home", "vertical farm"},
            {"quantum lab", "floating villa", "orbital dock"}};

    // gate landmarks, one per rank index 0..9
    private static final String[] GATE_NAMES = {
            "Watchtower", "Town Hall", "City Gate", "Windmill", "Clock Tower",
            "Railway Station", "Airport", "Space-Tech Tower", "Sky Bridge", "Teleport Gate"};

    private static final String[][] BONUS_LANDMARKS = {
            {"Great Barn", "Beacon Tower"},
            {"Castle", "Cathedral"},
            {"Stadium", "Observation Tower"},
            {"Floating District Hub", "Drone Port"},
            {"Space Port", "Singularity Core"}};

    static {
        for (int era = 1; era <= 5; era++) {
            List<Item> basics = new ArrayList<>();
            for (String name : BASICS[era - 1]) {
                basics.add(register(new Item(key(era, name), NationTrack.BASICS, era, Kind.NORMAL, null, title(name))));
            }
            LISTS.put(listId(era, NationTrack.BASICS), List.copyOf(basics));

            List<Item> structures = new ArrayList<>();
            for (String name : STRUCTURES[era - 1]) {
                structures.add(register(new Item(key(era, name), NationTrack.STRUCTURES, era, Kind.NORMAL, null, title(name))));
            }
            LISTS.put(listId(era, NationTrack.STRUCTURES), List.copyOf(structures));

            List<Item> bonus = new ArrayList<>();
            for (String name : BONUS_LANDMARKS[era - 1]) {
                bonus.add(register(new Item(key(era, name), NationTrack.LANDMARKS, era, Kind.BONUS, null, name)));
            }
            LISTS.put(listId(era, NationTrack.LANDMARKS), List.copyOf(bonus));
        }
        for (int rank = 0; rank < GATE_NAMES.length; rank++) {
            int era = NationRules.contentEra(rank);
            GATES[rank] = register(new Item(key(era, GATE_NAMES[rank]), NationTrack.LANDMARKS, era,
                    Kind.GATE, rank, GATE_NAMES[rank]));
        }
    }

    private NationCatalog() {
    }

    private static Item register(Item item) {
        BY_KEY.put(item.key(), item);
        return item;
    }

    private static String listId(int era, NationTrack track) {
        return era + ":" + track.name();
    }

    static String key(int era, String name) {
        return "e" + era + "." + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private static String title(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** Item lookup by key; throws if the key is unknown (a data bug, not a user error). */
    public static Item find(String key) {
        Item item = BY_KEY.get(key);
        if (item == null) {
            throw new IllegalStateException("Unknown nation catalog item: " + key);
        }
        return item;
    }

    /** Ordered NORMAL items for BASICS / STRUCTURES, or the BONUS landmarks for LANDMARKS, of one content era. */
    public static List<Item> items(int era, NationTrack track) {
        return LISTS.get(listId(era, track));
    }

    public static Item gate(int rankIndex) {
        return GATES[rankIndex];
    }

    public static String initialItemKey(NationTrack track) {
        return switch (track) {
            case BASICS -> items(1, NationTrack.BASICS).get(0).key();
            case STRUCTURES -> items(1, NationTrack.STRUCTURES).get(0).key();
            case LANDMARKS -> gate(0).key();
        };
    }
}

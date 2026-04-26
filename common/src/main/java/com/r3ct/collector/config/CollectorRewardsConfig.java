package com.r3ct.collector.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectorRewardsConfig {

    public static int rewardXpPerItem = 5;

    // Struktura wpisu z lootem (wzorowana na Twoim JSON)
    public static class LootEntry {
        public String item;
        public int min_amount;
        public int max_amount;
        public int weight;

        public LootEntry(String item, int min, int max, int weight) {
            this.item = item; this.min_amount = min; this.max_amount = max; this.weight = weight;
        }
    }

    // Nagrody co 100 przedmiotów
    public static List<LootEntry> milestoneRewards = new ArrayList<>(List.of(
            new LootEntry("minecraft:emerald", 24, 32, 100),
            new LootEntry("minecraft:diamond", 16, 24, 100)
    ));

    // Nagrody za zakładki (Figurki / Bedrock)
    public static Map<String, String> categoryRewards = new HashMap<>(Map.ofEntries(
            Map.entry("minecraft:building_blocks", "minecraft:bedrock"),
            Map.entry("minecraft:colored_blocks", "minecraft:bedrock"),
            Map.entry("minecraft:natural_blocks", "minecraft:bedrock"),
            Map.entry("minecraft:functional_blocks", "minecraft:bedrock"),
            Map.entry("minecraft:redstone_blocks", "minecraft:bedrock"),
            Map.entry("minecraft:tools_and_utilities", "minecraft:bedrock"),
            Map.entry("minecraft:combat", "minecraft:bedrock"),
            Map.entry("minecraft:food_and_drinks", "minecraft:bedrock"),
            Map.entry("minecraft:ingredients", "minecraft:bedrock"),
            Map.entry("minecraft:spawn_eggs", "minecraft:bedrock"),
            Map.entry("modded_generic", "minecraft:bedrock")
    ));

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_DIR = Paths.get("config", "r3ct_collector");
    private static final Path PATH = CONFIG_DIR.resolve("r3ct_collector_rewards.json");

    private static class ConfigData {
        int rewardXpPerItem = CollectorRewardsConfig.rewardXpPerItem;
        List<LootEntry> milestoneRewards = CollectorRewardsConfig.milestoneRewards;
        Map<String, String> categoryRewards = CollectorRewardsConfig.categoryRewards;
    }

    public static void load() {
        File file = PATH.toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data != null) {
                    rewardXpPerItem = data.rewardXpPerItem;
                    if (data.milestoneRewards != null) milestoneRewards = data.milestoneRewards;
                    if (data.categoryRewards != null) categoryRewards = data.categoryRewards;
                }
            } catch (Exception e) {
                System.err.println("[R3CT-Collector] Error loading rewards config!");
            }
        } else {
            save();
        }
    }

    public static void save() {
        // Zabezpieczenie: najpierw upewniamy się, że nasz folder "r3ct_collector" istnieje!
        File configDir = PATH.getParent().toFile();
        if (!configDir.exists()) configDir.mkdirs();

        try (FileWriter writer = new FileWriter(PATH.toFile())) {
            GSON.toJson(new ConfigData(), writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Funkcja losująca nagrodę na podstawie wagi (weight)
    public static LootEntry getRandomMilestoneReward() {
        if (milestoneRewards.isEmpty()) return null;
        int totalWeight = milestoneRewards.stream().mapToInt(e -> e.weight).sum();
        if (totalWeight <= 0) return null;

        int random = new java.util.Random().nextInt(totalWeight);
        for (LootEntry entry : milestoneRewards) {
            random -= entry.weight;
            if (random < 0) return entry;
        }
        return null;
    }
}
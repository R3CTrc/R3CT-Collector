package com.r3ct.collector.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectorRewardsConfig {

    public static int rewardXpPerItem = 5;
    public static int milestoneInterval = 100;
    public static List<LootEntry> milestoneRewards = new ArrayList<>();
    public static Map<String, String> categoryRewards = new HashMap<>();

    private static final int CONFIG_VERSION = 1; // Aktualna wersja konfigu
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Paths.get("config", "r3ct_collector");
    private static final Path PATH = CONFIG_DIR.resolve("r3ct_collector_rewards.json");

    private static void copyDefaultConfig() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            InputStream is = CollectorRewardsConfig.class.getResourceAsStream("/assets/r3ct_collector/r3ct_collector_rewards.json");
            if (is != null) {
                Files.copy(is, PATH, StandardCopyOption.REPLACE_EXISTING);
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Sprawdzanie wersji i tworzenie backupu
    private static void checkAndMigrate() {
        if (!Files.exists(PATH)) {
            copyDefaultConfig();
            return;
        }

        boolean needsUpdate = false;
        try (FileReader reader = new FileReader(PATH.toFile())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            int version = json.has("version") ? json.get("version").getAsInt() : 0;
            if (version < CONFIG_VERSION) {
                needsUpdate = true;
            }
        } catch (Exception e) {
            needsUpdate = true;
        }

        if (needsUpdate) {
            try {
                String oldName = PATH.getFileName().toString().replace(".json", "_OLD.json");
                Path backupPath = PATH.resolveSibling(oldName);
                Files.move(PATH, backupPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[R3CT-Collector] Outdated rewards config detected! Backed up to: " + oldName);
                copyDefaultConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void load() {
        // Sprawdzamy wersję przed próbą ładowania!
        checkAndMigrate();

        try (FileReader reader = new FileReader(PATH.toFile())) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                rewardXpPerItem = data.rewardXpPerItem;
                milestoneInterval = data.milestoneInterval;
                if (data.milestoneRewards != null) milestoneRewards = data.milestoneRewards;
                if (data.categoryRewards != null) categoryRewards = data.categoryRewards;
            }
        } catch (Exception e) {
            System.err.println("[R3CT-Collector] Error loading rewards config!");
        }
    }

    public static void save() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            try (FileWriter writer = new FileWriter(PATH.toFile())) {
                GSON.toJson(new ConfigData(), writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ConfigData {
        int version = CONFIG_VERSION;
        int rewardXpPerItem = CollectorRewardsConfig.rewardXpPerItem;
        int milestoneInterval = CollectorRewardsConfig.milestoneInterval;
        List<LootEntry> milestoneRewards = CollectorRewardsConfig.milestoneRewards;
        Map<String, String> categoryRewards = CollectorRewardsConfig.categoryRewards;
    }

    public static class LootEntry {
        public String item;
        public int min_amount;
        public int max_amount;
        public int weight;

        public LootEntry(String item, int min, int max, int weight) {
            this.item = item; this.min_amount = min; this.max_amount = max; this.weight = weight;
        }
    }

    public static LootEntry getRandomMilestoneReward() {
        if (milestoneRewards.isEmpty()) return null;
        int totalWeight = milestoneRewards.stream().mapToInt(e -> e.weight).sum();
        if (totalWeight <= 0) return null;

        // Używamy zoptymalizowanego losowacza Javy (brak ostrzeżeń z IntelliJ)
        int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(totalWeight);

        for (LootEntry entry : milestoneRewards) {
            random -= entry.weight;
            if (random < 0) return entry;
        }
        return null;
    }
}
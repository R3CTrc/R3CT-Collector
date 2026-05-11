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
import java.util.HashSet;
import java.util.Set;

public class CollectorConfig {

    public static Set<String> blacklistedMods = new HashSet<>();
    public static Set<String> blacklistedTabs = new HashSet<>();
    public static Set<String> blacklistedItems = new HashSet<>();
    public static float catalogScale = 1.0f;

    private static final int CONFIG_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Paths.get("config", "r3ct_collector");
    private static final Path SERVER_CONFIG_PATH = CONFIG_DIR.resolve("r3ct_collector_items.json");
    private static final Path CLIENT_CONFIG_PATH = CONFIG_DIR.resolve("r3ct_collector_client.json");

    private static void copyDefaultConfig(Path target, String resourceName) {
        try {
            if (!Files.exists(target.getParent())) {
                Files.createDirectories(target.getParent());
            }
            InputStream is = CollectorConfig.class.getResourceAsStream("/assets/r3ct_collector/config/" + resourceName);
            if (is != null) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                is.close();
            }
        } catch (IOException e) {
            System.err.println("[R3CT-Collector] Could not copy default config: " + resourceName);
            e.printStackTrace();
        }
    }

    private static void checkAndMigrate(Path path, String resourceName) {
        if (!Files.exists(path)) {
            copyDefaultConfig(path, resourceName);
            return;
        }

        boolean needsUpdate = false;
        try (FileReader reader = new FileReader(path.toFile())) {
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
                String oldName = path.getFileName().toString().replace(".json", "_OLD.json");
                Path backupPath = path.resolveSibling(oldName);
                Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[R3CT-Collector] Outdated config detected! Backed up to: " + oldName);
                copyDefaultConfig(path, resourceName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void load() {
        checkAndMigrate(SERVER_CONFIG_PATH, "r3ct_collector_items.json");
        try (FileReader reader = new FileReader(SERVER_CONFIG_PATH.toFile())) {
            ServerConfigData data = GSON.fromJson(reader, ServerConfigData.class);
            if (data != null) {
                if (data.blacklistedMods != null) blacklistedMods = data.blacklistedMods;
                if (data.blacklistedTabs != null) blacklistedTabs = data.blacklistedTabs;
                if (data.blacklistedItems != null) blacklistedItems = data.blacklistedItems;
            }
        } catch (Exception e) {
            System.err.println("[R3CT-Collector] Error loading items config!");
        }

        checkAndMigrate(CLIENT_CONFIG_PATH, "r3ct_collector_client.json");
        try (FileReader reader = new FileReader(CLIENT_CONFIG_PATH.toFile())) {
            ClientConfigData data = GSON.fromJson(reader, ClientConfigData.class);
            if (data != null) {
                catalogScale = data.catalogScale;
            }
        } catch (Exception e) {
            System.err.println("[R3CT-Collector] Error loading client config!");
        }
    }

    private static class ServerConfigData {
        int version = CONFIG_VERSION;
        Set<String> blacklistedMods = CollectorConfig.blacklistedMods;
        Set<String> blacklistedTabs = CollectorConfig.blacklistedTabs;
        Set<String> blacklistedItems = CollectorConfig.blacklistedItems;
    }

    private static class ClientConfigData {
        int version = CONFIG_VERSION;
        float catalogScale = CollectorConfig.catalogScale;
    }

    public static void saveServer() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            try (FileWriter writer = new FileWriter(SERVER_CONFIG_PATH.toFile())) {
                GSON.toJson(new ServerConfigData(), writer);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void saveClient() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            try (FileWriter writer = new FileWriter(CLIENT_CONFIG_PATH.toFile())) {
                GSON.toJson(new ClientConfigData(), writer);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void save() {
        saveServer();
        saveClient();
    }
}
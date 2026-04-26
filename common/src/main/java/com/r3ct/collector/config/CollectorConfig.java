package com.r3ct.collector.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class CollectorConfig {

    public static Set<String> blacklistedMods = new HashSet<>(Set.of(

    ));

    public static Set<String> blacklistedTabs = new HashSet<>(Set.of(
            "minecraft:spawn_eggs"
    ));

    public static Set<String> blacklistedItems = new HashSet<>(Set.of(
            "minecraft:spawner",
            "minecraft:trial_spawner",
            "minecraft:player_head",
            "minecraft:end_portal_frame",
            "minecraft:vault",
            "minecraft:suspicious_sand",
            "minecraft:suspicious_gravel",
            "minecraft:infested_stone",
            "minecraft:infested_cobblestone",
            "minecraft:infested_stone_bricks",
            "minecraft:infested_mossy_stone_bricks",
            "minecraft:infested_cracked_stone_bricks",
            "minecraft:infested_chiseled_stone_bricks",
            "minecraft:infested_deepslate",
            "minecraft:bedrock",
            "minecraft:large_fern",
            "minecraft:frogspawn",
            "minecraft:budding_amethyst",
            "minecraft:dragon_egg"
    ));

    public static float catalogScale = 1.0f;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_DIR = Paths.get("config", "r3ct_collector");

    private static final Path SERVER_CONFIG_PATH = CONFIG_DIR.resolve("r3ct_collector_items.json");
    private static final Path CLIENT_CONFIG_PATH = CONFIG_DIR.resolve("r3ct_collector_client.json");

    private static class ServerConfigData {
        Set<String> blacklistedMods = CollectorConfig.blacklistedMods;
        Set<String> blacklistedTabs = CollectorConfig.blacklistedTabs;
        Set<String> blacklistedItems = CollectorConfig.blacklistedItems;
    }

    private static class ClientConfigData {
        float catalogScale = CollectorConfig.catalogScale;
    }

    public static void load() {
        File serverFile = SERVER_CONFIG_PATH.toFile();
        if (serverFile.exists()) {
            try (FileReader reader = new FileReader(serverFile)) {
                ServerConfigData data = GSON.fromJson(reader, ServerConfigData.class);
                if (data != null) {
                    if (data.blacklistedMods != null) blacklistedMods = data.blacklistedMods;
                    if (data.blacklistedTabs != null) blacklistedTabs = data.blacklistedTabs;
                    if (data.blacklistedItems != null) blacklistedItems = data.blacklistedItems;
                }
            } catch (Exception e) {
                System.err.println("[R3CT-Collector] Error loading server config!");
                e.printStackTrace();
            }
        } else {
            saveServer();
        }

        File clientFile = CLIENT_CONFIG_PATH.toFile();
        if (clientFile.exists()) {
            try (FileReader reader = new FileReader(clientFile)) {
                ClientConfigData data = GSON.fromJson(reader, ClientConfigData.class);
                if (data != null) {
                    catalogScale = data.catalogScale;
                }
            } catch (Exception e) {
                System.err.println("[R3CT-Collector] Error loading client config!");
                e.printStackTrace();
            }
        } else {
            saveClient();
        }
    }

    public static void saveServer() {
        File configDir = SERVER_CONFIG_PATH.getParent().toFile();
        if (!configDir.exists()) configDir.mkdirs();

        ServerConfigData data = new ServerConfigData();
        try (FileWriter writer = new FileWriter(SERVER_CONFIG_PATH.toFile())) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveClient() {
        File configDir = CLIENT_CONFIG_PATH.getParent().toFile();
        if (!configDir.exists()) configDir.mkdirs();

        ClientConfigData data = new ClientConfigData();
        try (FileWriter writer = new FileWriter(CLIENT_CONFIG_PATH.toFile())) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        saveServer();
        saveClient();
    }
}
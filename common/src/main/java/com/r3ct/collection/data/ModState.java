package com.r3ct.collection.data;

import com.mojang.serialization.Codec;
import com.r3ct.collection.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModState extends SavedData {
    public final Map<UUID, PlayerData> players = new HashMap<>();

    public static ModState get(MinecraftServer server) {
        Path dataDir = server.getWorldPath(LevelResource.ROOT).resolve("data");

        Path oldFile = dataDir.resolve("r3ct_collector_data.dat");
        Path newFile = dataDir.resolve("r3ct_collection_data.dat");

        if (Files.exists(oldFile) && !Files.exists(newFile)) {
            try {
                Files.move(oldFile, newFile);
                System.out.println("[R3CT-Collection] Successfully migrated old player data file to new name!");
            } catch (IOException e) {
                System.err.println("[R3CT-Collection] Failed to migrate old player data file!");
                e.printStackTrace();
            }
        }

        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static PlayerData getPlayerData(MinecraftServer server, UUID uuid) {
        return get(server).players.computeIfAbsent(uuid, k -> {
            get(server).setDirty();
            return new PlayerData();
        });
    }

    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag playersNbt = new CompoundTag();
        players.forEach((uuid, data) -> {
            playersNbt.put(uuid.toString(), data.toNbt());
        });
        nbt.put(Constants.NBT_PLAYERS_KEY, playersNbt);
        return nbt;
    }

    public static ModState load(CompoundTag nbt, HolderLookup.Provider registries) {
        ModState state = new ModState();

        nbt.getCompound(Constants.NBT_PLAYERS_KEY).ifPresent(playersNbt -> {
            for (String key : playersNbt.keySet()) {
                playersNbt.getCompound(key).ifPresent(playerDataNbt -> {
                    try {
                        PlayerData data = PlayerData.fromNbt(playerDataNbt);

                        Set<String> migratedItems = new HashSet<>();
                        for (String item : data.unlockedItems) {
                            if (item.startsWith("r3ct_collector:") || item.startsWith("r3ct:")) {
                                migratedItems.add(item.replace("r3ct_collector:", "r3ct_collection:")
                                        .replace("r3ct:", "r3ct_collection:"));
                            } else {
                                migratedItems.add(item);
                            }
                        }
                        data.unlockedItems = migratedItems;

                        Set<String> migratedCats = new HashSet<>();
                        for (String cat : data.rewardedCategories) {
                            if (cat.startsWith("r3ct_collector:") || cat.startsWith("r3ct:")) {
                                migratedCats.add(cat.replace("r3ct_collector:", "r3ct_collection:")
                                        .replace("r3ct:", "r3ct_collection:"));
                            } else {
                                migratedCats.add(cat);
                            }
                        }
                        data.rewardedCategories = migratedCats;

                        state.players.put(UUID.fromString(key), data);
                    } catch (IllegalArgumentException ignored) {
                    }
                });
            }
        });

        return state;
    }

    public static final Codec<ModState> CODEC = CompoundTag.CODEC.xmap(
            nbt -> load(nbt, null),
            state -> state.save(new CompoundTag(), null)
    );

    public static final SavedDataType<ModState> TYPE = new SavedDataType<>(
            Constants.DATA_NAME,
            ModState::new,
            CODEC,
            DataFixTypes.LEVEL
    );
}
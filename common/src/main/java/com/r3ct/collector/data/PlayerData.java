package com.r3ct.collector.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

public class PlayerData {
    public String lastKnownName = "Unknown"; // NOWE: Pamiętamy nick gracza!
    public Set<String> unlockedItems = new HashSet<>();
    public Set<String> rewardedCategories = new HashSet<>();

    public static final Codec<PlayerData> CODEC = CompoundTag.CODEC.xmap(PlayerData::fromNbt, PlayerData::toNbt);

    public PlayerData() {
        unlockedItems.clear();
        rewardedCategories.clear();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        // Zapisujemy nick
        nbt.putString("lastKnownName", lastKnownName);

        ListTag itemsList = new ListTag();
        for (String item : unlockedItems) itemsList.add(StringTag.valueOf(item != null ? item : ""));
        nbt.put("unlockedItems", itemsList);

        ListTag categoriesList = new ListTag();
        for (String cat : rewardedCategories) categoriesList.add(StringTag.valueOf(cat != null ? cat : ""));
        nbt.put("rewardedCategories", categoriesList);

        return nbt;
    }

    public static PlayerData fromNbt(CompoundTag nbt) {
        PlayerData data = new PlayerData();

        if (nbt.contains("lastKnownName")) {
            data.lastKnownName = nbt.getString("lastKnownName").orElse("Unknown");
        }

        if (nbt.contains("unlockedItems")) {
            Tag tag = nbt.get("unlockedItems");
            if (tag instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) list.getString(i).ifPresent(data.unlockedItems::add);
            }
        }
        if (nbt.contains("rewardedCategories")) {
            Tag tag = nbt.get("rewardedCategories");
            if (tag instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) list.getString(i).ifPresent(data.rewardedCategories::add);
            }
        }
        return data;
    }
}
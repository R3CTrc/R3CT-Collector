package com.r3ct.collector.logic;

import com.r3ct.collector.config.CollectorRewardsConfig;
import com.r3ct.collector.data.ModState;
import com.r3ct.collector.data.PlayerData;
import com.r3ct.collector.network.LeaderboardDataPayload;
import com.r3ct.collector.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ServerItemHandler {

    private static void grantAdvancement(ServerPlayer player, String advancementId) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        Identifier id = Identifier.parse(advancementId);
        net.minecraft.advancements.AdvancementHolder advancement = server.getAdvancements().get(id);

        if (advancement != null) {
            player.getAdvancements().award(advancement, "unlocked");
        }
    }

    public static void handleItemSubmit(ServerPlayer player, String itemId) {
        Identifier id = Identifier.parse(itemId);
        Item targetItem = BuiltInRegistries.ITEM.get(id).map(net.minecraft.core.Holder::value).orElse(Items.AIR);

        if (targetItem == Items.AIR) return;

        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());

        data.lastKnownName = player.getName().getString();
        ModState.get(player.level().getServer()).setDirty();

        if (data.unlockedItems.contains(itemId)) return;

        Inventory inv = player.getInventory();
        boolean foundAndRemoved = false;

        if (player.isCreative()) {
            foundAndRemoved = true;
        } else {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.is(targetItem)) {
                    stack.shrink(1);
                    foundAndRemoved = true;
                    break;
                }
            }
        }

        if (foundAndRemoved) {
            CollectorRewardsConfig.load();

            int sizeBefore = data.unlockedItems.size();
            data.unlockedItems.add(itemId);
            int sizeAfter = data.unlockedItems.size();

            int xpToGive = CollectorRewardsConfig.xpCommon;
            net.minecraft.world.item.Rarity rarity = new ItemStack(targetItem).getRarity();

            if (rarity == net.minecraft.world.item.Rarity.UNCOMMON) {
                xpToGive = CollectorRewardsConfig.xpUncommon;
            } else if (rarity == net.minecraft.world.item.Rarity.RARE) {
                xpToGive = CollectorRewardsConfig.xpRare;
            } else if (rarity == net.minecraft.world.item.Rarity.EPIC) {
                xpToGive = CollectorRewardsConfig.xpEpic;
            }

            player.giveExperiencePoints(xpToGive);

            if (sizeAfter >= 1) grantAdvancement(player, "r3ct_collector:first_item");
            if (sizeAfter >= 100) grantAdvancement(player, "r3ct_collector:items_100");
            if (sizeAfter >= 500) grantAdvancement(player, "r3ct_collector:items_500");
            if (sizeAfter >= 1000) grantAdvancement(player, "r3ct_collector:items_1000");

            int interval = CollectorRewardsConfig.milestoneInterval;
            if (interval > 0 && (sizeBefore / interval < sizeAfter / interval)) {
                CollectorRewardsConfig.LootEntry reward = CollectorRewardsConfig.getRandomMilestoneReward();
                if (reward != null) {
                    Item rewardItem = BuiltInRegistries.ITEM.get(Identifier.parse(reward.item)).map(net.minecraft.core.Holder::value).orElse(Items.AIR);
                    if (rewardItem != Items.AIR) {
                        int amount = reward.min_amount + player.getRandom().nextInt((reward.max_amount - reward.min_amount) + 1);
                        giveItemToPlayer(player, new ItemStack(rewardItem, amount));
                    }
                }
            }
            checkAndAwardCompletedCategories(player, data);

            ModState.get(player.level().getServer()).setDirty();
            Services.PLATFORM.sendSyncDataPacketToClient(player, data.unlockedItems, data.rewardedCategories);

            handleLeaderboardRequest(player);
        }
    }

    private static void checkAndAwardCompletedCategories(ServerPlayer player, PlayerData data) {
        if (com.r3ct.collector.scanner.CreativeTabScanner.SCANNED_SUBCATEGORIES.isEmpty()) {
            com.r3ct.collector.scanner.CreativeTabScanner.scanAllTabs();
        }

        int completedRealCategories = 0;

        for (com.r3ct.collector.scanner.CreativeTabScanner.SubCategory cat : com.r3ct.collector.scanner.CreativeTabScanner.SCANNED_SUBCATEGORIES.values()) {
            if (!data.rewardedCategories.contains(cat.tabId)) {

                int gathered = 0;
                for (ItemStack stack : cat.items) {
                    String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    if (data.unlockedItems.contains(id)) gathered++;
                }

                if (gathered > 0 && gathered == cat.items.size()) {
                    handleCategoryReward(player, cat.tabId);
                    player.level().playSound(null, player.blockPosition(),
                            net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_TWINKLE,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }

            if (data.rewardedCategories.contains(cat.tabId)) {
                completedRealCategories++;
            }
        }

        if (completedRealCategories >= com.r3ct.collector.scanner.CreativeTabScanner.SCANNED_SUBCATEGORIES.size()
                && !com.r3ct.collector.scanner.CreativeTabScanner.SCANNED_SUBCATEGORIES.isEmpty()) {
            if (!data.rewardedCategories.contains("ALL_COMPLETED")) {
                handleCategoryReward(player, "ALL_COMPLETED");
            }
        }
    }

    public static void handleCategoryReward(ServerPlayer player, String tabId) {
        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());
        data.lastKnownName = player.getName().getString();

        if (data.rewardedCategories.contains(tabId)) return;

        if (tabId.equals("ALL_COMPLETED")) {
            grantAdvancement(player, "r3ct_collector:all_completed");
            data.rewardedCategories.add("ALL_COMPLETED");
            ModState.get(player.level().getServer()).setDirty();
            return;
        }

        CollectorRewardsConfig.load();
        String rewardItemId = CollectorRewardsConfig.categoryRewards.getOrDefault(tabId, CollectorRewardsConfig.categoryRewards.get("modded_generic"));

        if (rewardItemId != null) {
            Item rewardItem = BuiltInRegistries.ITEM.get(Identifier.parse(rewardItemId)).map(net.minecraft.core.Holder::value).orElse(Items.AIR);
            if (rewardItem != Items.AIR) {
                ItemStack rewardStack = new ItemStack(rewardItem, 1);

                net.minecraft.network.chat.MutableComponent customName = net.minecraft.network.chat.Component.literal(player.getName().getString())
                        .withStyle(ChatFormatting.AQUA);

                customName.append(net.minecraft.network.chat.Component.literal(" - ").withStyle(ChatFormatting.LIGHT_PURPLE));

                customName.append(net.minecraft.network.chat.Component.translatable(rewardItem.getDescriptionId()).withStyle(ChatFormatting.LIGHT_PURPLE));

                if (rewardItemId.equals("r3ct_collector:trophy_mod")) {
                    net.minecraft.world.item.CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(Identifier.parse(tabId)).map(net.minecraft.core.Holder::value).orElse(null);
                    if (tab != null) {
                        customName.append(net.minecraft.network.chat.Component.literal(" - ").withStyle(ChatFormatting.LIGHT_PURPLE))
                                .append(tab.getDisplayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }

                rewardStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, customName);

                giveItemToPlayer(player, rewardStack);

                data.rewardedCategories.add(tabId);

                int catSize = data.rewardedCategories.size();
                if (data.rewardedCategories.contains("ALL_COMPLETED")) catSize--;

                if (catSize >= 1) grantAdvancement(player, "r3ct_collector:category_1");
                if (catSize >= 5) grantAdvancement(player, "r3ct_collector:category_5");

                ModState.get(player.level().getServer()).setDirty();
                Services.PLATFORM.sendSyncDataPacketToClient(player, data.unlockedItems, data.rewardedCategories);
            }
        }
    }

    public static void handleLeaderboardRequest(ServerPlayer player) {
        grantAdvancement(player, "r3ct_collector:root");

        net.minecraft.server.MinecraftServer server = player.level().getServer();
        ModState state = ModState.get(server);

        List<LeaderboardDataPayload.TopPlayerEntry> allEntries = new ArrayList<>();

        state.players.forEach((uuid, data) -> {
            String name = data.lastKnownName;
            if (!name.equals("Unknown") && !data.unlockedItems.isEmpty()) {
                allEntries.add(new LeaderboardDataPayload.TopPlayerEntry(name, data.unlockedItems.size(), new ArrayList<>(data.unlockedItems)));
            }
        });

        allEntries.sort((e1, e2) -> Integer.compare(e2.totalItems(), e1.totalItems()));

        List<LeaderboardDataPayload.TopPlayerEntry> top10 = allEntries.stream().limit(10).toList();
        Services.PLATFORM.sendLeaderboardDataPacketToClient(player, top10);
    }

    private static void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            ItemEntity drop = player.drop(stack, false);
            if (drop != null) drop.setNoPickUpDelay();
        }
    }
}
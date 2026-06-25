package com.r3ct.collection.logic;

import com.r3ct.collection.Constants;
import com.r3ct.collection.config.CollectionConfig;
import com.r3ct.collection.data.ModState;
import com.r3ct.collection.data.PlayerData;
import com.r3ct.collection.network.LeaderboardDataPayload;
import com.r3ct.collection.platform.Services;
import com.r3ct.collection.scanner.CreativeTabScanner;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

public class ServerItemHandler {

    private static MutableComponent getPrefix() {
        return Component.literal("[Collection] ").withStyle(ChatFormatting.AQUA);
    }

    private static MutableComponent buildTrophyName(String playerName, String descriptionId, CreativeModeTab tab) {
        MutableComponent name = Component.literal(playerName).withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" - ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.translatable(descriptionId).withStyle(ChatFormatting.LIGHT_PURPLE));

        if (tab != null) {
            name.append(Component.literal(" - ").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(tab.getDisplayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return name;
    }

    private static void grantAdvancement(ServerPlayer player, String advancementId) {
        MinecraftServer server = player.level().getServer();
        Identifier id = Identifier.parse(advancementId);
        AdvancementHolder advancement = server.getAdvancements().get(id);

        if (advancement != null) {
            player.getAdvancements().award(advancement, "unlocked");
        }
    }

    public static void handleItemSubmit(ServerPlayer player, String itemId, int slotId) {
        String[] parts = itemId.split("#");
        Identifier id = Identifier.parse(parts[0]);
        Item targetItem = BuiltInRegistries.ITEM.get(id).map(Holder::value).orElse(Items.AIR);

        if (targetItem == Items.AIR) return;

        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());

        data.lastKnownName = player.getName().getString();
        ModState.get(player.level().getServer()).setDirty();

        if (data.unlockedItems.contains(itemId)) return;

        boolean foundAndRemoved = false;

        if (player.isCreative()) {
            foundAndRemoved = true;
        } else {
            ItemStack stack = player.getInventory().getItem(slotId);
            if (!stack.isEmpty() && getUniqueItemId(stack).equals(itemId)) {
                stack.shrink(1);
                foundAndRemoved = true;
            }
        }

        if (foundAndRemoved) {

            int sizeBefore = data.unlockedItems.size();
            data.unlockedItems.add(itemId);
            int sizeAfter = data.unlockedItems.size();

            Rarity rarity = new ItemStack(targetItem).getRarity();
            int xpToGive = switch (rarity) {
                case UNCOMMON -> CollectionConfig.xpUncommon;
                case RARE -> CollectionConfig.xpRare;
                case EPIC -> CollectionConfig.xpEpic;
                default -> CollectionConfig.xpCommon;
            };

            player.giveExperiencePoints(xpToGive);

            if (sizeAfter >= 1) grantAdvancement(player, "r3ct_collection:first_item");
            if (sizeAfter >= 100) grantAdvancement(player, "r3ct_collection:items_100");
            if (sizeAfter >= 500) grantAdvancement(player, "r3ct_collection:items_500");
            if (sizeAfter >= 1000) grantAdvancement(player, "r3ct_collection:items_1000");

            int interval = CollectionConfig.milestoneInterval;
            if (interval > 0 && (sizeBefore / interval < sizeAfter / interval)) {
                CollectionConfig.LootEntry reward = CollectionConfig.getRandomMilestoneReward();
                if (reward != null) {
                    Item rewardItem = BuiltInRegistries.ITEM.get(Identifier.parse(reward.item)).map(Holder::value).orElse(Items.AIR);
                    if (rewardItem != Items.AIR) {
                        int amount = reward.min_amount + player.getRandom().nextInt((reward.max_amount - reward.min_amount) + 1);
                        ItemStack rewardStack = new ItemStack(rewardItem, amount);
                        var savedItemName = rewardStack.getHoverName().copy();

                        giveItemToPlayer(player, rewardStack);

                        player.level().playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.0F, 1.0F);

                        ChatFormatting rewardColor = ChatFormatting.AQUA;
                        if (reward.color != null && reward.color.length() >= 2 && reward.color.startsWith("&")) {
                            ChatFormatting parsedColor = ChatFormatting.getByCode(reward.color.charAt(1));
                            if (parsedColor != null) {
                                rewardColor = parsedColor;
                            }
                        }

                        var numberComp = Component.literal(String.valueOf(sizeAfter)).withStyle(ChatFormatting.YELLOW);
                        var rewardComp = Component.literal(amount + "x ")
                                .withStyle(rewardColor)
                                .append(savedItemName.withStyle(rewardColor));

                        player.sendSystemMessage(Component.empty()
                                .append(getPrefix())
                                .append(Component.translatable("chat.r3ct_collection.milestone_reward", numberComp, rewardComp).withStyle(ChatFormatting.GREEN))
                        );
                    }
                }
            }
            checkAndAwardCompletedCategories(player, data);

            ModState.get(player.level().getServer()).setDirty();
            Services.PLATFORM.sendSyncDataPacketToClient(player, data.unlockedItems, data.rewardedCategories);

            handleLeaderboardRequest(player);
        }
    }

    public static void checkAndAwardCompletedCategories(ServerPlayer player, PlayerData data) {
        if (CreativeTabScanner.SCANNED_SUBCATEGORIES.isEmpty()) {
            CreativeTabScanner.scanAllTabs(player.level().enabledFeatures(), player.level().registryAccess(), false);
        }

        int completedRealCategories = 0;

        for (CreativeTabScanner.SubCategory cat : CreativeTabScanner.SCANNED_SUBCATEGORIES.values()) {
            if (!data.rewardedCategories.contains(cat.tabId)) {
                int gathered = 0;
                for (ItemStack stack : cat.items) {
                    String id = getUniqueItemId(stack);
                    if (data.unlockedItems.contains(id)) gathered++;
                }

                if (gathered > 0 && gathered == cat.items.size()) {
                    handleCategoryReward(player, cat.tabId);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }

            if (data.rewardedCategories.contains(cat.tabId)) {
                completedRealCategories++;
            }
        }

        if (completedRealCategories >= CreativeTabScanner.SCANNED_SUBCATEGORIES.size() && !CreativeTabScanner.SCANNED_SUBCATEGORIES.isEmpty()) {
            if (!data.rewardedCategories.contains(Constants.ALL_COMPLETED_KEY)) {
                handleCategoryReward(player, Constants.ALL_COMPLETED_KEY);
            }
        }
    }

    public static void handleCategoryReward(ServerPlayer player, String tabId) {
        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());
        data.lastKnownName = player.getName().getString();

        if (data.rewardedCategories.contains(tabId)) return;

        if (tabId.equals(Constants.ALL_COMPLETED_KEY)) {
            grantAdvancement(player, "r3ct_collection:all_completed");
            data.rewardedCategories.add(Constants.ALL_COMPLETED_KEY);
            ModState.get(player.level().getServer()).setDirty();
            return;
        }

        Item trophyItem = BuiltInRegistries.ITEM.get(Identifier.parse(Constants.MOD_ID + ":trophy")).map(Holder::value).orElse(Items.AIR);

        if (trophyItem != Items.AIR) {
            ItemStack rewardStack = new ItemStack(trophyItem, 1);
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(Identifier.parse(tabId)).map(Holder::value).orElse(null);

            String displayItemId = "minecraft:nether_star";
            if (tab != null && !tab.getIconItem().isEmpty()) {
                displayItemId = BuiltInRegistries.ITEM.getKey(tab.getIconItem().getItem()).toString();
            }

            CompoundTag tag = new CompoundTag();
            tag.putString("DisplayItem", displayItemId);
            tag.putString("OwnerName", player.getName().getString());
            rewardStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            MutableComponent customName = buildTrophyName(player.getName().getString(), trophyItem.getDescriptionId(), tab);
            rewardStack.set(DataComponents.CUSTOM_NAME, customName);

            var savedTrophyName = rewardStack.getHoverName().copy();
            giveItemToPlayer(player, rewardStack);

            Component tabName = (tab != null) ? tab.getDisplayName() : Component.literal(tabId);
            var catNameComp = tabName.copy().withStyle(ChatFormatting.YELLOW);
            var trophyComp = savedTrophyName.withStyle(ChatFormatting.LIGHT_PURPLE);

            player.sendSystemMessage(Component.empty()
                    .append(getPrefix())
                    .append(Component.translatable("chat.r3ct_collection.category_complete", catNameComp, trophyComp).withStyle(ChatFormatting.GREEN))
            );

            data.rewardedCategories.add(tabId);

            int catSize = data.rewardedCategories.size();
            if (data.rewardedCategories.contains(Constants.ALL_COMPLETED_KEY)) catSize--;

            if (catSize >= 1) grantAdvancement(player, "r3ct_collection:category_1");
            if (catSize >= 5) grantAdvancement(player, "r3ct_collection:category_5");

            ModState.get(player.level().getServer()).setDirty();
            Services.PLATFORM.sendSyncDataPacketToClient(player, data.unlockedItems, data.rewardedCategories);
        }
    }

    public static void handleLeaderboardRequest(ServerPlayer player) {
        grantAdvancement(player, "r3ct_collection:root");

        MinecraftServer server = player.level().getServer();
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

    public static String getUniqueItemId(ItemStack stack) {
        String baseId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        if (stack.has(DataComponents.POTION_CONTENTS)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.potion().isPresent()) {
                String potionId = contents.potion().get().unwrapKey().map(key -> key.identifier().toString()).orElse("");
                if (!potionId.isEmpty()) {
                    return baseId + "#" + potionId;
                }
            }
        }
        return baseId;
    }

    public static void refundMigrationTrophies(ServerPlayer player, PlayerData data) {
        if (data.receivedMigrationRefund) return;

        if (data.rewardedCategories.isEmpty() || (data.rewardedCategories.size() == 1 && data.rewardedCategories.contains(Constants.ALL_COMPLETED_KEY))) {
            data.receivedMigrationRefund = true;
            ModState.get(player.level().getServer()).setDirty();
            return;
        }

        boolean gaveAny = false;
        Item trophyItem = BuiltInRegistries.ITEM.get(Identifier.parse(Constants.MOD_ID + ":trophy")).map(Holder::value).orElse(Items.AIR);

        for (String tabId : data.rewardedCategories) {
            if (tabId.equals(Constants.ALL_COMPLETED_KEY)) continue;

            if (trophyItem != Items.AIR) {
                ItemStack rewardStack = new ItemStack(trophyItem, 1);
                CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(Identifier.parse(tabId)).map(Holder::value).orElse(null);

                String displayItemId = "minecraft:nether_star";
                if (tab != null && !tab.getIconItem().isEmpty()) {
                    displayItemId = BuiltInRegistries.ITEM.getKey(tab.getIconItem().getItem()).toString();
                }

                CompoundTag tag = new CompoundTag();
                tag.putString("DisplayItem", displayItemId);
                tag.putString("OwnerName", player.getName().getString());
                rewardStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                MutableComponent customName = buildTrophyName(player.getName().getString(), trophyItem.getDescriptionId(), tab);
                rewardStack.set(DataComponents.CUSTOM_NAME, customName);

                giveItemToPlayer(player, rewardStack);
                gaveAny = true;
            }
        }

        data.receivedMigrationRefund = true;
        ModState.get(player.level().getServer()).setDirty();

        if (gaveAny) {
            var message = Component.translatable("chat.r3ct_collection.migration_refund").withStyle(ChatFormatting.GREEN);
            player.sendSystemMessage(Component.empty().append(getPrefix()).append(message));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
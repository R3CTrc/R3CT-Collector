package com.r3ct.collection;

import com.r3ct.collection.block.ModBlocks;
import com.r3ct.collection.config.CollectionConfig;
import com.r3ct.collection.data.ModState;
import com.r3ct.collection.data.PlayerData;
import com.r3ct.collection.logic.ServerItemHandler;
import com.r3ct.collection.network.*;
import com.r3ct.collection.platform.Services;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;

public class CollectionFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CollectionConfig.load();

        Identifier trophyId = Identifier.parse(Constants.MOD_ID + ":trophy");
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, trophyId);

        Registry.register(BuiltInRegistries.BLOCK, trophyId, ModBlocks.TROPHY);
        Registry.register(BuiltInRegistries.ITEM, trophyId, new BlockItem(ModBlocks.TROPHY, new Item.Properties()
                .setId(itemKey).stacksTo(1).rarity(Rarity.EPIC).fireResistant()
        ));

        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.parse(Constants.MOD_ID + ":trophy_be"), ModBlocks.TROPHY_BE_TYPE);

        ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.parse(Constants.MOD_ID + ":main_tab")
        );

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + Constants.MOD_ID + ".main_tab"))
                .icon(() -> new ItemStack(ModBlocks.TROPHY))
                .displayItems((context, output) -> {
                    output.accept(ModBlocks.TROPHY);
                })
                .build()
        );

        PayloadTypeRegistry.serverboundPlay().register(SubmitItemPayload.TYPE, SubmitItemPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClaimCategoryRewardPayload.TYPE, ClaimCategoryRewardPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncDataPayload.TYPE, SyncDataPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestLeaderboardPayload.TYPE, RequestLeaderboardPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LeaderboardDataPayload.TYPE, LeaderboardDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BulkSubmitPayload.TYPE, BulkSubmitPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SubmitItemPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerItemHandler.handleItemSubmit(context.player(), payload.itemId(), payload.slotId()));
        });

        ServerPlayNetworking.registerGlobalReceiver(ClaimCategoryRewardPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerItemHandler.handleCategoryReward(context.player(), payload.tabId()));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerData data = ModState.getPlayerData(server, handler.player.getUUID());
            ServerItemHandler.refundMigrationTrophies(handler.player, data);
            ServerItemHandler.checkAndAwardCompletedCategories(handler.player, data);
            Services.PLATFORM.sendSyncDataPacketToClient(handler.player, data.unlockedItems, data.rewardedCategories);

            String itemsJson = CollectionConfig.getConfigFileAsString("r3ct_collection_items.json");
            String rewardsJson = CollectionConfig.getConfigFileAsString("r3ct_collection_rewards.json");
            ServerPlayNetworking.send(handler.player, new ConfigSyncPayload(itemsJson, rewardsJson));
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestLeaderboardPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerItemHandler.handleLeaderboardRequest(context.player()));
        });

        ServerPlayNetworking.registerGlobalReceiver(BulkSubmitPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerItemHandler.handleBulkSubmit(context.player(), payload));
        });
    }
}
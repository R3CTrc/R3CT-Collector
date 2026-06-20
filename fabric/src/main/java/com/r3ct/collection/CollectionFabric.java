package com.r3ct.collection;

import com.r3ct.collection.config.CollectionConfig;
import com.r3ct.collection.logic.ServerItemHandler;
import com.r3ct.collection.network.SubmitItemPayload;
import com.r3ct.collection.network.SyncDataPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import com.r3ct.collection.block.ModBlocks;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

public class CollectionFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CollectionConfig.load();

        ModBlocks.TROPHIES.forEach(this::registerTrophy);

        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.parse(Constants.MOD_ID + ":trophy_building_be"), ModBlocks.TROPHY_BE_TYPE);

        ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.parse(Constants.MOD_ID + ":main_tab")
        );

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricItemGroup.builder()
                .title(net.minecraft.network.chat.Component.translatable("itemGroup." + Constants.MOD_ID + ".main_tab"))
                .icon(() -> new net.minecraft.world.item.ItemStack(ModBlocks.TROPHY_BUILDING))
                .displayItems((context, output) -> {
                    ModBlocks.TROPHIES.values().forEach(output::accept);
                })
                .build()
        );

        PayloadTypeRegistry.playC2S().register(SubmitItemPayload.TYPE, SubmitItemPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(com.r3ct.collection.network.ClaimCategoryRewardPayload.TYPE, com.r3ct.collection.network.ClaimCategoryRewardPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncDataPayload.TYPE, SyncDataPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(com.r3ct.collection.network.RequestLeaderboardPayload.TYPE, com.r3ct.collection.network.RequestLeaderboardPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(com.r3ct.collection.network.LeaderboardDataPayload.TYPE, com.r3ct.collection.network.LeaderboardDataPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SubmitItemPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerItemHandler.handleItemSubmit(context.player(), payload.itemId(), payload.slotId()));
        });

        ServerPlayNetworking.registerGlobalReceiver(com.r3ct.collection.network.ClaimCategoryRewardPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerItemHandler.handleCategoryReward(context.player(), payload.tabId()));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            com.r3ct.collection.data.PlayerData data = com.r3ct.collection.data.ModState.getPlayerData(server, handler.player.getUUID());
            com.r3ct.collection.logic.ServerItemHandler.refundMigrationTrophies(handler.player, data);
            com.r3ct.collection.platform.Services.PLATFORM.sendSyncDataPacketToClient(handler.player, data.unlockedItems, data.rewardedCategories);
        });

        ServerPlayNetworking.registerGlobalReceiver(com.r3ct.collection.network.RequestLeaderboardPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerItemHandler.handleLeaderboardRequest(context.player()));
        });
    }

    private void registerTrophy(String name, Block block) {
        Identifier id = Identifier.parse(Constants.MOD_ID + ":" + name);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()
                .setId(itemKey).stacksTo(1).rarity(Rarity.EPIC).fireResistant()
        ));
    }
}
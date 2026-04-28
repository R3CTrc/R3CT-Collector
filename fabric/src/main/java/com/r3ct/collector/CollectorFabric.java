package com.r3ct.collector;

import com.r3ct.collector.config.CollectorConfig;
import com.r3ct.collector.config.CollectorRewardsConfig;
import com.r3ct.collector.logic.ServerItemHandler;
import com.r3ct.collector.network.SubmitItemPayload;
import com.r3ct.collector.network.SyncDataPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class CollectorFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CollectorConfig.load();
        CollectorRewardsConfig.load();

        PayloadTypeRegistry.playC2S().register(SubmitItemPayload.TYPE, SubmitItemPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(com.r3ct.collector.network.ClaimCategoryRewardPayload.TYPE, com.r3ct.collector.network.ClaimCategoryRewardPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncDataPayload.TYPE, SyncDataPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(com.r3ct.collector.network.RequestLeaderboardPayload.TYPE, com.r3ct.collector.network.RequestLeaderboardPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(com.r3ct.collector.network.LeaderboardDataPayload.TYPE, com.r3ct.collector.network.LeaderboardDataPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SubmitItemPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerItemHandler.handleItemSubmit(context.player(), payload.itemId()));
        });

        ServerPlayNetworking.registerGlobalReceiver(com.r3ct.collector.network.ClaimCategoryRewardPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerItemHandler.handleCategoryReward(context.player(), payload.tabId()));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            com.r3ct.collector.data.PlayerData data = com.r3ct.collector.data.ModState.getPlayerData(server, handler.player.getUUID());
            com.r3ct.collector.platform.Services.PLATFORM.sendSyncDataPacketToClient(handler.player, data.unlockedItems, data.rewardedCategories);
        });

        ServerPlayNetworking.registerGlobalReceiver(com.r3ct.collector.network.RequestLeaderboardPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerItemHandler.handleLeaderboardRequest(context.player()));
        });
    }
}
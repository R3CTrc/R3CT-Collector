package com.r3ct.collector;

import com.r3ct.collector.client.data.ClientPlayerData;
import com.r3ct.collector.logic.ServerItemHandler;
import com.r3ct.collector.network.SubmitItemPayload;
import com.r3ct.collector.network.SyncDataPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashSet;

@Mod(Constants.MOD_ID)
public class CollectorNeoForge {

    public CollectorNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(this::registerPackets);
        // Rejestrujemy zdarzenie dołączania do gry na głównym busie NeoForge
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
    }

    private void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);

        registrar.playToServer(
                SubmitItemPayload.TYPE, SubmitItemPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ServerItemHandler.handleItemSubmit((ServerPlayer) context.player(), payload.itemId());
                })
        );

        // NOWE (NeoForge)
        registrar.playToServer(
                com.r3ct.collector.network.ClaimCategoryRewardPayload.TYPE, com.r3ct.collector.network.ClaimCategoryRewardPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ServerItemHandler.handleCategoryReward((ServerPlayer) context.player(), payload.tabId());
                })
        );

        registrar.playToClient(
                SyncDataPayload.TYPE, SyncDataPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ClientPlayerData.unlockedItems = new HashSet<>(payload.unlockedItems());
                    ClientPlayerData.rewardedCategories = new HashSet<>(payload.rewardedCategories());
                })
        );

        registrar.playToServer(
                com.r3ct.collector.network.RequestLeaderboardPayload.TYPE, com.r3ct.collector.network.RequestLeaderboardPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerItemHandler.handleLeaderboardRequest((net.minecraft.server.level.ServerPlayer) context.player()))
        );

        registrar.playToClient(
                com.r3ct.collector.network.LeaderboardDataPayload.TYPE, com.r3ct.collector.network.LeaderboardDataPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ClientPlayerData.leaderboardData = new java.util.ArrayList<>(payload.entries());
                })
        );
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            com.r3ct.collector.data.PlayerData data = com.r3ct.collector.data.ModState.getPlayerData(serverPlayer.level().getServer(), serverPlayer.getUUID());
            com.r3ct.collector.platform.Services.PLATFORM.sendSyncDataPacketToClient(serverPlayer, data.unlockedItems, data.rewardedCategories);
        }
    }
}
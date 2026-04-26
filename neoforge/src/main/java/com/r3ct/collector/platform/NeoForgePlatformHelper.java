package com.r3ct.collector.platform;

import com.r3ct.collector.network.SubmitItemPayload;
import com.r3ct.collector.network.SyncDataPayload;
import com.r3ct.collector.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public void sendSubmitItemPacketToServer(String itemId) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new SubmitItemPayload(itemId));
    }

    @Override
    public void sendClaimRewardPacketToServer(String tabId) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.r3ct.collector.network.ClaimCategoryRewardPayload(tabId));
    }

    @Override
    public void sendSyncDataPacketToClient(net.minecraft.server.level.ServerPlayer player, java.util.Set<String> unlockedItems, java.util.Set<String> rewardedCategories) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new SyncDataPayload(new java.util.ArrayList<>(unlockedItems), new java.util.ArrayList<>(rewardedCategories)));
    }

    @Override
    public void sendRequestLeaderboardPacketToServer() {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.r3ct.collector.network.RequestLeaderboardPayload());
    }

    @Override
    public void sendLeaderboardDataPacketToClient(net.minecraft.server.level.ServerPlayer player, java.util.List<com.r3ct.collector.network.LeaderboardDataPayload.TopPlayerEntry> entries) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new com.r3ct.collector.network.LeaderboardDataPayload(entries));
    }
}
package com.r3ct.collector.platform;

import com.r3ct.collector.network.SubmitItemPayload;
import com.r3ct.collector.network.SyncDataPayload;
import com.r3ct.collector.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public void sendSubmitItemPacketToServer(String itemId) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new SubmitItemPayload(itemId));
    }

    @Override
    public void sendClaimRewardPacketToServer(String tabId) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.r3ct.collector.network.ClaimCategoryRewardPayload(tabId));
    }

    @Override
    public void sendSyncDataPacketToClient(net.minecraft.server.level.ServerPlayer player, java.util.Set<String> unlockedItems, java.util.Set<String> rewardedCategories) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new SyncDataPayload(new java.util.ArrayList<>(unlockedItems), new java.util.ArrayList<>(rewardedCategories)));
    }

    @Override
    public void sendRequestLeaderboardPacketToServer() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.r3ct.collector.network.RequestLeaderboardPayload());
    }

    @Override
    public void sendLeaderboardDataPacketToClient(net.minecraft.server.level.ServerPlayer player, java.util.List<com.r3ct.collector.network.LeaderboardDataPayload.TopPlayerEntry> entries) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.r3ct.collector.network.LeaderboardDataPayload(entries));
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T> createBlockEntityType(java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, T> factory, net.minecraft.world.level.block.Block... blocks) {
        return net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(factory::apply, blocks).build();
    }
}
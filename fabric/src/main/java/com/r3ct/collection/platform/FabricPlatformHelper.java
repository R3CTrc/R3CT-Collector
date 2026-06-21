package com.r3ct.collection.platform;

import com.r3ct.collection.client.input.KeyMappings;
import com.r3ct.collection.network.*;
import com.r3ct.collection.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

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
    public void sendSubmitItemPacketToServer(String itemId, int slotId) {
        ClientPlayNetworking.send(new SubmitItemPayload(itemId, slotId));
    }

    @Override
    public void sendClaimRewardPacketToServer(String tabId) {
        ClientPlayNetworking.send(new ClaimCategoryRewardPayload(tabId));
    }

    @Override
    public void sendSyncDataPacketToClient(ServerPlayer player, Set<String> unlockedItems, Set<String> rewardedCategories) {
        ServerPlayNetworking.send(player, new SyncDataPayload(new ArrayList<>(unlockedItems), new ArrayList<>(rewardedCategories)));
    }

    @Override
    public void sendRequestLeaderboardPacketToServer() {
        ClientPlayNetworking.send(new RequestLeaderboardPayload());
    }

    @Override
    public void sendLeaderboardDataPacketToClient(ServerPlayer player, List<LeaderboardDataPayload.TopPlayerEntry> entries) {
        ServerPlayNetworking.send(player, new LeaderboardDataPayload(entries));
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return FabricBlockEntityTypeBuilder.create(factory::apply, blocks).build();
    }

    @Override
    public boolean isCatalogKey(Object event) {
        if (event instanceof net.minecraft.client.input.KeyEvent keyEvent) {
            return KeyMappings.openCatalogKey != null &&
                    KeyMappings.openCatalogKey.matches(keyEvent);
        }
        return false;
    }
}
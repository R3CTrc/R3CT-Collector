package com.r3ct.collection.platform;

import com.r3ct.collection.client.input.KeyMappings;
import com.r3ct.collection.network.*;
import com.r3ct.collection.platform.services.IPlatformHelper;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

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
    public void sendSubmitItemPacketToServer(String itemId, int slotId) {
        ClientPacketDistributor.sendToServer(new SubmitItemPayload(itemId, slotId));
    }

    @Override
    public void sendBulkSubmitPacketToServer(String tabId, List<String> itemIds, List<Integer> slotIds) {
        ClientPacketDistributor.sendToServer(new BulkSubmitPayload(tabId, itemIds, slotIds));
    }

    @Override
    public void sendClaimRewardPacketToServer(String tabId) {
        ClientPacketDistributor.sendToServer(new ClaimCategoryRewardPayload(tabId));
    }

    @Override
    public void sendSyncDataPacketToClient(ServerPlayer player, Set<String> unlockedItems, Set<String> rewardedCategories) {
        PacketDistributor.sendToPlayer(player, new SyncDataPayload(new ArrayList<>(unlockedItems), new ArrayList<>(rewardedCategories)));
    }

    @Override
    public void sendRequestLeaderboardPacketToServer() {
        ClientPacketDistributor.sendToServer(new RequestLeaderboardPayload());
    }

    @Override
    public void sendLeaderboardDataPacketToClient(ServerPlayer player, List<LeaderboardDataPayload.TopPlayerEntry> entries) {
        PacketDistributor.sendToPlayer(player, new LeaderboardDataPayload(entries));
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return new BlockEntityType<>(factory::apply, Set.of(blocks));
    }

    @Override
    public boolean isCatalogKey(Object event) {
        if (event instanceof KeyEvent keyEvent) {
            return KeyMappings.openCatalogKey != null &&
                    KeyMappings.openCatalogKey.matches(keyEvent);
        }
        return false;
    }
}
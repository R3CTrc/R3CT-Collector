package com.r3ct.collection.platform.services;

import com.r3ct.collection.network.LeaderboardDataPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {

        return isDevelopmentEnvironment() ? "development" : "production";
    }

    void sendSubmitItemPacketToServer(String itemId, int slotId);

    void sendClaimRewardPacketToServer(String tabId);

    void sendSyncDataPacketToClient(ServerPlayer player, Set<String> unlockedItems, Set<String> rewardedCategories);

    void sendRequestLeaderboardPacketToServer();

    void sendLeaderboardDataPacketToClient(ServerPlayer player, List<LeaderboardDataPayload.TopPlayerEntry> entries);

    <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks);

    boolean isCatalogKey(Object event);
}
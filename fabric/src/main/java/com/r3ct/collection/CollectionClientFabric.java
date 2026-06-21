package com.r3ct.collection;

import com.mojang.blaze3d.platform.InputConstants;
import com.r3ct.collection.block.ModBlocks;
import com.r3ct.collection.client.input.KeyMappings;
import com.r3ct.collection.client.data.ClientPlayerData;
import com.r3ct.collection.network.LeaderboardDataPayload;
import com.r3ct.collection.network.SyncDataPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;

public class CollectionClientFabric implements ClientModInitializer {

    private static final KeyMapping.Category R3CT_COLLECTOR_CATEGORY = KeyMapping.Category.register(Identifier.parse("r3ct_collection:main"));

    @Override
    public void onInitializeClient() {

        BlockRenderLayerMap.putBlocks(
                ChunkSectionLayer.TRANSLUCENT,
                ModBlocks.TROPHIES.values().toArray(new Block[0])
        );

        KeyMappings.openCatalogKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.r3ct.open_catalog",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                R3CT_COLLECTOR_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KeyMappings.handleKeyInput();
        });

        ClientPlayNetworking.registerGlobalReceiver(
                SyncDataPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        ClientPlayerData.unlockedItems = new HashSet<>(payload.unlockedItems());
                        ClientPlayerData.rewardedCategories = new HashSet<>(payload.rewardedCategories());
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                LeaderboardDataPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    ClientPlayerData.leaderboardData = new ArrayList<>(payload.entries());
                })
        );
    }
}
package com.r3ct.collection;

import com.mojang.blaze3d.platform.InputConstants;
import com.r3ct.collection.client.input.KeyMappings;
import com.r3ct.collection.client.data.ClientPlayerData;
import com.r3ct.collection.config.CollectionConfig;
import com.r3ct.collection.network.ConfigSyncPayload;
import com.r3ct.collection.network.LeaderboardDataPayload;
import com.r3ct.collection.network.SyncDataPayload;
import com.r3ct.collection.scanner.CreativeTabScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;

public class CollectionClientFabric implements ClientModInitializer {

    private static final KeyMapping.Category R3CT_COLLECTOR_CATEGORY = KeyMapping.Category.register(Identifier.parse("r3ct_collection:main"));

    @Override
    public void onInitializeClient() {

        KeyMappings.openCatalogKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
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

        ClientPlayNetworking.registerGlobalReceiver(
                ConfigSyncPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    CollectionConfig.syncFromServer(payload.itemsJson(), payload.rewardsJson());
                    CreativeTabScanner.SCANNED_SUBCATEGORIES.clear();
                })
        );

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CollectionConfig.load();
            CreativeTabScanner.SCANNED_SUBCATEGORIES.clear();
        });
    }
}
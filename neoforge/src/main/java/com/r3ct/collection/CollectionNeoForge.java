package com.r3ct.collection;

import com.r3ct.collection.block.ModBlocks;
import com.r3ct.collection.client.data.ClientPlayerData;
import com.r3ct.collection.config.CollectionConfig;
import com.r3ct.collection.data.ModState;
import com.r3ct.collection.data.PlayerData;
import com.r3ct.collection.logic.ServerItemHandler;
import com.r3ct.collection.network.*;
import com.r3ct.collection.platform.Services;
import com.r3ct.collection.scanner.CreativeTabScanner;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.HashSet;

@Mod(Constants.MOD_ID)
public class CollectionNeoForge {

    public CollectionNeoForge(IEventBus modEventBus) {
        CollectionConfig.load();

        modEventBus.addListener(this::registerPackets);
        modEventBus.addListener(this::onRegister);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
    }

    private void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);

        registrar.playToServer(
                SubmitItemPayload.TYPE, SubmitItemPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ServerItemHandler.handleItemSubmit((ServerPlayer) context.player(), payload.itemId(), payload.slotId());
                })
        );

        registrar.playToServer(
                ClaimCategoryRewardPayload.TYPE, ClaimCategoryRewardPayload.CODEC,
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
                RequestLeaderboardPayload.TYPE, RequestLeaderboardPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerItemHandler.handleLeaderboardRequest((ServerPlayer) context.player()))
        );

        registrar.playToClient(
                LeaderboardDataPayload.TYPE, LeaderboardDataPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ClientPlayerData.leaderboardData = new ArrayList<>(payload.entries());
                })
        );

        registrar.playToClient(
                ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    CollectionConfig.syncFromServer(payload.itemsJson(), payload.rewardsJson());
                    CreativeTabScanner.SCANNED_SUBCATEGORIES.clear();
                })
        );
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerData data = ModState.getPlayerData(serverPlayer.level().getServer(), serverPlayer.getUUID());
            ServerItemHandler.refundMigrationTrophies(serverPlayer, data);
            ServerItemHandler.checkAndAwardCompletedCategories(serverPlayer, data);
            Services.PLATFORM.sendSyncDataPacketToClient(serverPlayer, data.unlockedItems, data.rewardedCategories);

            String itemsJson = CollectionConfig.getConfigFileAsString("r3ct_collection_items.json");
            String rewardsJson = CollectionConfig.getConfigFileAsString("r3ct_collection_rewards.json");
            PacketDistributor.sendToPlayer(serverPlayer, new ConfigSyncPayload(itemsJson, rewardsJson));
        }
    }

    private void onRegister(RegisterEvent event) {

        Identifier trophyId = Identifier.parse(Constants.MOD_ID + ":trophy");
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, trophyId);

        event.register(BuiltInRegistries.BLOCK.key(), helper -> helper.register(trophyId, ModBlocks.TROPHY));
        event.register(BuiltInRegistries.ITEM.key(), helper -> helper.register(trophyId, new BlockItem(ModBlocks.TROPHY, new Item.Properties()
                .setId(itemKey).stacksTo(1).rarity(Rarity.EPIC).fireResistant()
        )));

        event.register(BuiltInRegistries.BLOCK_ENTITY_TYPE.key(), helper -> {
            helper.register(Identifier.parse(Constants.MOD_ID + ":trophy_be"), ModBlocks.TROPHY_BE_TYPE);
        });

        event.register(Registries.CREATIVE_MODE_TAB, helper -> {
            helper.register(Identifier.parse(Constants.MOD_ID + ":main_tab"),
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup." + Constants.MOD_ID + ".main_tab"))
                            .icon(() -> new ItemStack(ModBlocks.TROPHY))
                            .displayItems((context, output) -> {
                                output.accept(ModBlocks.TROPHY);
                            })
                            .build()
            );
        });
    }
}
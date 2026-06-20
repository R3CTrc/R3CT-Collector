package com.r3ct.collection;

import com.r3ct.collection.client.data.ClientPlayerData;
import com.r3ct.collection.config.CollectionConfig;
import com.r3ct.collection.logic.ServerItemHandler;
import com.r3ct.collection.network.SubmitItemPayload;
import com.r3ct.collection.network.SyncDataPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.RegisterEvent;
import com.r3ct.collection.block.ModBlocks;

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
                com.r3ct.collection.network.ClaimCategoryRewardPayload.TYPE, com.r3ct.collection.network.ClaimCategoryRewardPayload.CODEC,
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
                com.r3ct.collection.network.RequestLeaderboardPayload.TYPE, com.r3ct.collection.network.RequestLeaderboardPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> ServerItemHandler.handleLeaderboardRequest((net.minecraft.server.level.ServerPlayer) context.player()))
        );

        registrar.playToClient(
                com.r3ct.collection.network.LeaderboardDataPayload.TYPE, com.r3ct.collection.network.LeaderboardDataPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ClientPlayerData.leaderboardData = new java.util.ArrayList<>(payload.entries());
                })
        );
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            com.r3ct.collection.data.PlayerData data = com.r3ct.collection.data.ModState.getPlayerData(serverPlayer.level().getServer(), serverPlayer.getUUID());
            com.r3ct.collection.logic.ServerItemHandler.refundMigrationTrophies(serverPlayer, data);
            com.r3ct.collection.platform.Services.PLATFORM.sendSyncDataPacketToClient(serverPlayer, data.unlockedItems, data.rewardedCategories);
        }
    }

    private void onRegister(RegisterEvent event) {

        java.util.function.BiConsumer<String, Block> registerTrophy = (name, block) -> {
            Identifier id = Identifier.parse(Constants.MOD_ID + ":" + name);
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

            event.register(BuiltInRegistries.BLOCK.key(), helper -> helper.register(id, block));
            event.register(BuiltInRegistries.ITEM.key(), helper -> helper.register(id, new BlockItem(block, new Item.Properties()
                    .setId(itemKey).stacksTo(1).rarity(Rarity.EPIC).fireResistant()
            )));
        };

        ModBlocks.TROPHIES.forEach(registerTrophy::accept);

        event.register(BuiltInRegistries.BLOCK_ENTITY_TYPE.key(), helper -> {
            helper.register(Identifier.parse(Constants.MOD_ID + ":trophy_building_be"), ModBlocks.TROPHY_BE_TYPE);
        });

        event.register(Registries.CREATIVE_MODE_TAB, helper -> {
            helper.register(Identifier.parse(Constants.MOD_ID + ":main_tab"),
                    net.minecraft.world.item.CreativeModeTab.builder()
                            .title(net.minecraft.network.chat.Component.translatable("itemGroup." + Constants.MOD_ID + ".main_tab"))
                            .icon(() -> new net.minecraft.world.item.ItemStack(ModBlocks.TROPHY_BUILDING))
                            .displayItems((context, output) -> {
                                ModBlocks.TROPHIES.values().forEach(output::accept);
                            })
                            .build()
            );
        });
    }
}
package com.r3ct.collection;

import com.mojang.blaze3d.platform.InputConstants;
import com.r3ct.collection.client.input.KeyMappings;
import com.r3ct.collection.client.screen.CollectionConfigScreen;
import com.r3ct.collection.config.CollectionConfig;
import com.r3ct.collection.scanner.CreativeTabScanner;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.lwjgl.glfw.GLFW;

public class CollectionClientNeoForge {

    @EventBusSubscriber(modid = "r3ct_collection", value = Dist.CLIENT)
    public static class ClientModEvents {

        public static final KeyMapping.Category R3CT_COLLECTOR_CATEGORY = KeyMapping.Category.register(Identifier.parse("r3ct_collection:main"));

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            KeyMappings.openCatalogKey = new KeyMapping(
                    "key.r3ct.open_catalog",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_K,
                    R3CT_COLLECTOR_CATEGORY
            );

            event.register(KeyMappings.openCatalogKey);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ModLoadingContext.get().registerExtensionPoint(
                    IConfigScreenFactory.class,
                    () -> (minecraft, parentScreen) -> new CollectionConfigScreen(parentScreen)
            );
        }
    }

    @EventBusSubscriber(modid = "r3ct_collection", value = Dist.CLIENT)
    public static class ClientGameEvents {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            KeyMappings.handleKeyInput();
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            CollectionConfig.load();
            CreativeTabScanner.SCANNED_SUBCATEGORIES.clear();
        }
    }
}
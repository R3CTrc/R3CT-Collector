package com.r3ct.collector;

import com.mojang.blaze3d.platform.InputConstants;
import com.r3ct.collector.client.input.KeyMappings;
import com.r3ct.collector.client.screen.CollectorConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class CollectorClientNeoForge {

    @EventBusSubscriber(modid = "r3ct_collector", value = Dist.CLIENT)
    public static class ClientModEvents {

        public static final KeyMapping.Category R3CT_COLLECTOR_CATEGORY = KeyMapping.Category.register(Identifier.parse("r3ct_collector:main"));

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            KeyMappings.openCatalogKey = new KeyMapping(
                    "key.r3ct.open_catalog",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_C,
                    R3CT_COLLECTOR_CATEGORY
            );

            event.register(KeyMappings.openCatalogKey);
        }

        @SubscribeEvent
        public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
            net.neoforged.fml.ModLoadingContext.get().registerExtensionPoint(
                    net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                    () -> (minecraft, parentScreen) -> new CollectorConfigScreen(parentScreen)
            );
        }
    }

    @EventBusSubscriber(modid = "r3ct_collector", value = Dist.CLIENT)
    public static class ClientGameEvents {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            KeyMappings.handleKeyInput();
        }
    }
}
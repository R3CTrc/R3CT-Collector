package com.r3ct.collector.client.input;

import com.r3ct.collector.client.screen.CatalogScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class KeyMappings {

    // Zmienna będzie zainicjowana przez Fabric lub NeoForge
    public static KeyMapping openCatalogKey;

    public static void handleKeyInput() {
        if (openCatalogKey == null) return;

        Minecraft mc = Minecraft.getInstance();

        while (openCatalogKey.consumeClick()) {
            if (mc.screen == null && mc.level != null) {
                mc.setScreen(new CatalogScreen());
            }
        }
    }
}
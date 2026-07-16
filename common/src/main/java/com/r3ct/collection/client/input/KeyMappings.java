package com.r3ct.collection.client.input;

import com.r3ct.collection.client.screen.CatalogScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class KeyMappings {

    public static KeyMapping openCatalogKey;

    public static void handleKeyInput() {
        if (openCatalogKey == null) return;

        Minecraft mc = Minecraft.getInstance();

        while (openCatalogKey.consumeClick()) {
            if (mc.gui.screen() == null && mc.level != null) {
                mc.gui.setScreen(new CatalogScreen());
            }
        }
    }
}
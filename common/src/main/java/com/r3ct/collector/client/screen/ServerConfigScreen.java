package com.r3ct.collector.client.screen;

import com.r3ct.collector.config.CollectorConfig;
import com.r3ct.collector.config.CollectorRewardsConfig;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.nio.file.Paths;

public class ServerConfigScreen extends Screen {
    private final Screen parent;

    public ServerConfigScreen(Screen parent) {
        super(Component.translatable("gui.r3ct_collector.config.server.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 2 - 30;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.r3ct_collector.config.server.items_button"), button -> {
                    File configFile = Paths.get("config", "r3ct_collector", "r3ct_collector_items.json").toFile();
                    if (!configFile.exists()) CollectorConfig.saveServer();
                    Util.getPlatform().openUri(configFile.toURI());
                })
                .bounds(centerX, startY, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.translatable("gui.r3ct_collector.config.server.items_tooltip")))
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.r3ct_collector.config.server.rewards_button"), button -> {
                    File configFile = Paths.get("config", "r3ct_collector", "r3ct_collector_rewards.json").toFile();
                    if (!configFile.exists()) CollectorRewardsConfig.save();
                    Util.getPlatform().openUri(configFile.toURI());
                })
                .bounds(centerX, startY + 25, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.translatable("gui.r3ct_collector.config.server.rewards_tooltip")))
                .build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centerX, startY + 60, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
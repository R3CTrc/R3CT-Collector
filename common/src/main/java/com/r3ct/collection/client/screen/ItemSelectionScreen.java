package com.r3ct.collection.client.screen;

import com.r3ct.collection.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

public class ItemSelectionScreen extends Screen {
    private final Screen parent;
    private final List<CatalogScreen.SlotItem> availableItems;
    private final String itemId;
    private final Runnable onNextInQueue;

    public ItemSelectionScreen(Screen parent, List<CatalogScreen.SlotItem> availableItems, String itemId) {
        this(parent, availableItems, itemId, null);
    }

    public ItemSelectionScreen(Screen parent, List<CatalogScreen.SlotItem> availableItems, String itemId, Runnable onNextInQueue) {
        super(Component.translatable("gui.r3ct_collection.catalog.select_title"));
        this.parent = parent;
        this.availableItems = availableItems;
        this.itemId = itemId;
        this.onNextInQueue = onNextInQueue;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.onClose();
        }).bounds(this.width / 2 - 50, this.height / 2 + 50, 100, 20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xD9000000);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFFFF);

        int slotSize = 24;
        int spacing = 4;
        int step = slotSize + spacing;

        int totalWidth = (availableItems.size() * step) - spacing;

        int startX = (this.width - totalWidth) / 2;
        int startY = this.height / 2 - (slotSize / 2);

        for (int i = 0; i < availableItems.size(); i++) {
            CatalogScreen.SlotItem slotItem = availableItems.get(i);
            int slotX = startX + (i * step);
            int slotY = startY;

            guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0x66000000);

            int itemX = slotX + 4;
            int itemY = slotY + 4;

            guiGraphics.item(slotItem.stack, itemX, itemY);
            guiGraphics.itemDecorations(this.font, slotItem.stack, itemX, itemY);

            if (mouseX >= slotX && mouseX <= slotX + slotSize && mouseY >= slotY && mouseY <= slotY + slotSize) {
                guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0x44FFFFFF);
                guiGraphics.setTooltipForNextFrame(this.font, slotItem.stack.getTooltipLines(Item.TooltipContext.of(this.minecraft.level), this.minecraft.player, TooltipFlag.NORMAL), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int slotSize = 24;
        int spacing = 4;
        int step = slotSize + spacing;
        int totalWidth = (availableItems.size() * step) - spacing;
        int startX = (this.width - totalWidth) / 2;
        int startY = this.height / 2 - (slotSize / 2);

        for (int i = 0; i < availableItems.size(); i++) {
            int slotX = startX + (i * step);
            int slotY = startY;

            if (mouseX >= slotX && mouseX <= slotX + slotSize && mouseY >= slotY && mouseY <= slotY + slotSize) {
                CatalogScreen.SlotItem selected = availableItems.get(i);

                if (this.onNextInQueue != null) {
                    this.minecraft.setScreen(new ConfirmSubmitScreen(
                            this,
                            selected.stack,
                            selected.slotId,
                            this.itemId,
                            () -> {
                                Services.PLATFORM.sendSubmitItemPacketToServer(this.itemId, selected.slotId);
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
                                this.onNextInQueue.run();
                            },
                            () -> this.minecraft.setScreen(this)
                    ));
                } else {
                    if (CatalogScreen.isValuable(selected.stack)) {
                        this.minecraft.setScreen(new ConfirmSubmitScreen(this.parent, selected.stack, selected.slotId, this.itemId));
                    } else {
                        Services.PLATFORM.sendSubmitItemPacketToServer(this.itemId, selected.slotId);
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
                        this.minecraft.setScreen(this.parent);
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        if (this.onNextInQueue != null) {
            this.onNextInQueue.run();
        } else if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (Services.PLATFORM.isCatalogKey(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
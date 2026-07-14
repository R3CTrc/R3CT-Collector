package com.r3ct.collection.client.screen;

import com.r3ct.collection.logic.BulkSubmitHelper;
import com.r3ct.collection.logic.ServerItemHandler;
import com.r3ct.collection.network.BulkSubmitPayload;
import com.r3ct.collection.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public class BulkSubmitScreen extends Screen {
    private final Screen parent;
    private final String tabId;
    private final BulkSubmitHelper.ScanResult scanResult;

    public BulkSubmitScreen(Screen parent, String tabId, BulkSubmitHelper.ScanResult scanResult) {
        super(Component.translatable("gui.r3ct_collection.catalog.bulk.title"));
        this.parent = parent;
        this.tabId = tabId;
        this.scanResult = scanResult;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int bottomY = this.height / 2 + 50;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.r3ct_collection.catalog.yes").withStyle(ChatFormatting.GREEN), btn -> {
            submitSafeItems();
        }).bounds(centerX - 105, bottomY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.r3ct_collection.catalog.no").withStyle(ChatFormatting.RED), btn -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX + 5, bottomY, 100, 20).build());
    }

    private void submitSafeItems() {
        if (!scanResult.safeItemIds.isEmpty()) {
            Services.PLATFORM.sendBulkSubmitPacketToServer(tabId, scanResult.safeItemIds, scanResult.safeSlots);
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
        }

        if (!scanResult.conflicts.isEmpty()) {
            processConflictQueue(0);
        } else {
            this.minecraft.setScreen(parent);
        }
    }

    private void processConflictQueue(int index) {
        if (index >= scanResult.conflicts.size()) {
            this.minecraft.setScreen(parent);
            return;
        }

        BulkSubmitHelper.ConflictGroup conflict = scanResult.conflicts.get(index);

        if (conflict.slotItems.size() == 1) {
            CatalogScreen.SlotItem singleItem = conflict.slotItems.get(0);

            this.minecraft.setScreen(new ConfirmSubmitScreen(
                    this.parent,
                    singleItem.stack,
                    singleItem.slotId,
                    conflict.itemId,
                    () -> {
                        Services.PLATFORM.sendSubmitItemPacketToServer(conflict.itemId, singleItem.slotId);
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
                        processConflictQueue(index + 1);
                    },
                    () -> {
                        processConflictQueue(index + 1);
                    }
            ));
        } else {
            this.minecraft.setScreen(new ItemSelectionScreen(
                    this.parent,
                    conflict.slotItems,
                    conflict.itemId,
                    () -> processConflictQueue(index + 1)
            ));
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xD9000000);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFFFF);

        Inventory inv = this.minecraft.player.getInventory();
        int maxPreview = Math.min(scanResult.safeSlots.size(), 10);

        int slotSize = 24;
        int spacing = 4;
        int step = slotSize + spacing;

        int totalWidth = (maxPreview * step) - spacing;
        if (scanResult.safeSlots.size() > 10) {
            totalWidth += this.font.width("...") + spacing;
        }

        int startX = (this.width - totalWidth) / 2;
        int startY = this.height / 2 - (slotSize / 2);

        for (int i = 0; i < maxPreview; i++) {
            int slotX = startX + (i * step);
            int slotY = startY;

            guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0x66000000);

            int itemX = slotX + 4;
            int itemY = slotY + 4;

            ItemStack stack = inv.getItem(scanResult.safeSlots.get(i));
            guiGraphics.item(stack, itemX, itemY);
            guiGraphics.itemDecorations(this.font, stack, itemX, itemY);

            if (mouseX >= slotX && mouseX <= slotX + slotSize && mouseY >= slotY && mouseY <= slotY + slotSize) {
                guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0x44FFFFFF);
                guiGraphics.setTooltipForNextFrame(this.font, stack.getTooltipLines(Item.TooltipContext.of(this.minecraft.level), this.minecraft.player, TooltipFlag.NORMAL), Optional.empty(), mouseX, mouseY);
            }
        }

        if (scanResult.safeSlots.size() > 10) {
            guiGraphics.text(this.font, Component.literal("..."), startX + (maxPreview * step), startY + 8, 0xFFFFFFFF, false);
        }
    }
}
package com.r3ct.collection.client.screen;

import com.r3ct.collection.logic.BulkSubmitHelper;
import com.r3ct.collection.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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

    private int currentRowScroll = 0;
    private boolean isScrolling = false;
    private double scrollGrabOffset = 0.0;

    private static final int COLUMNS = 9;
    private static final int VISIBLE_ROWS = 5;
    private static final int SLOT_SIZE = 24;
    private static final int SPACING = 4;
    private static final int STEP = SLOT_SIZE + SPACING;

    public BulkSubmitScreen(Screen parent, String tabId, BulkSubmitHelper.ScanResult scanResult) {
        super(Component.translatable("gui.r3ct_collection.catalog.bulk.title"));
        this.parent = parent;
        this.tabId = tabId;
        this.scanResult = scanResult;
    }

    @Override
    protected void init() {
        if (this.scanResult.safeSlots.isEmpty() && !this.scanResult.conflicts.isEmpty()) {
            processConflictQueue(0);
            return;
        }

        int centerX = this.width / 2;
        int gridHeight = (VISIBLE_ROWS * STEP) - SPACING;
        int startY = (this.height - gridHeight) / 2 - 10;
        int bottomY = startY + gridHeight + 20;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.r3ct_collection.catalog.yes").withStyle(ChatFormatting.GREEN), btn -> {
            submitSafeItems();
        }).bounds(centerX - 105, bottomY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.r3ct_collection.catalog.no").withStyle(ChatFormatting.RED), btn -> {
            this.onClose();
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
                    () -> processConflictQueue(index + 1)
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
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xD9000000);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        Inventory inv = this.minecraft.player.getInventory();
        int totalItems = scanResult.safeSlots.size();

        int totalWidth = (COLUMNS * STEP) - SPACING;
        int gridHeight = (VISIBLE_ROWS * STEP) - SPACING;

        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - gridHeight) / 2 - 10;

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, startY - 20, 0xFFFFFFFF);

        int totalRows = (int) Math.ceil((double) totalItems / COLUMNS);
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);

        if (maxScroll > 0) {
            int trackX = startX + totalWidth + 10;
            int trackY = startY;
            int trackH = gridHeight;

            guiGraphics.fill(trackX, trackY, trackX + 4, trackY + trackH, 0xFF1A0A04);
            float scrollFraction = (float) currentRowScroll / maxScroll;
            int thumbH = Math.max(12, (int) (((float) VISIBLE_ROWS / totalRows) * trackH));
            int thumbY = trackY + (int) (scrollFraction * (trackH - thumbH));
            guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, isScrolling ? 0xFFA07A5A : 0xFF8A5A3A);
        }

        int startIndex = currentRowScroll * COLUMNS;
        int endIndex = Math.min(startIndex + (COLUMNS * VISIBLE_ROWS), totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            int indexOnScreen = i - startIndex;
            int col = indexOnScreen % COLUMNS;
            int row = indexOnScreen / COLUMNS;

            int slotX = startX + (col * STEP);
            int slotY = startY + (row * STEP);

            guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x66000000);

            int itemX = slotX + 4;
            int itemY = slotY + 4;

            ItemStack originalStack = inv.getItem(scanResult.safeSlots.get(i));
            ItemStack displayStack = originalStack.copyWithCount(1);

            guiGraphics.renderItem(displayStack, itemX, itemY);
            guiGraphics.renderItemDecorations(this.font, displayStack, itemX, itemY);

            if (mouseX >= slotX && mouseX <= slotX + SLOT_SIZE && mouseY >= slotY && mouseY <= slotY + SLOT_SIZE) {
                guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x44FFFFFF);
                guiGraphics.setTooltipForNextFrame(this.font, displayStack.getTooltipLines(Item.TooltipContext.of(this.minecraft.level), this.minecraft.player, TooltipFlag.NORMAL), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int totalWidth = (COLUMNS * STEP) - SPACING;
        int gridHeight = (VISIBLE_ROWS * STEP) - SPACING;
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - gridHeight) / 2 - 10;
        int trackX = startX + totalWidth + 10;
        int trackY = startY;
        int trackH = gridHeight;

        int totalItems = scanResult.safeSlots.size();
        int totalRows = (int) Math.ceil((double) totalItems / COLUMNS);
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);

        if (maxScroll > 0 && mouseX >= trackX - 2 && mouseX <= trackX + 6 && mouseY >= trackY && mouseY <= trackY + trackH) {
            isScrolling = true;
            int thumbH = Math.max(12, (int) (((float) VISIBLE_ROWS / totalRows) * trackH));
            float scrollFraction = (float) currentRowScroll / maxScroll;
            int thumbY = trackY + (int) (scrollFraction * (trackH - thumbH));

            if (mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                scrollGrabOffset = mouseY - thumbY;
            } else {
                scrollGrabOffset = thumbH / 2.0;
            }
            updateScrollbar(mouseY);
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (isScrolling) {
            updateScrollbar(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) isScrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalItems = scanResult.safeSlots.size();
        int totalRows = (int) Math.ceil((double) totalItems / COLUMNS);
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);

        if (maxScroll > 0) {
            if (scrollY > 0 && currentRowScroll > 0) currentRowScroll--;
            else if (scrollY < 0 && currentRowScroll < maxScroll) currentRowScroll++;
        }
        return true;
    }

    private void updateScrollbar(double mouseY) {
        int gridHeight = (VISIBLE_ROWS * STEP) - SPACING;
        int startY = (this.height - gridHeight) / 2 - 10;
        int trackY = startY;
        int trackH = gridHeight;

        int totalItems = scanResult.safeSlots.size();
        int totalRows = (int) Math.ceil((double) totalItems / COLUMNS);
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);

        if (maxScroll > 0) {
            int thumbH = Math.max(12, (int) (((float) VISIBLE_ROWS / totalRows) * trackH));
            float fraction = Mth.clamp((float)(mouseY - scrollGrabOffset - trackY) / (trackH - thumbH), 0.0f, 1.0f);
            currentRowScroll = Math.round(fraction * maxScroll);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
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
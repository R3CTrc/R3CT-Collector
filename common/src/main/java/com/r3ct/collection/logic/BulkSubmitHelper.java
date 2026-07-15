package com.r3ct.collection.logic;

import com.r3ct.collection.client.data.ClientPlayerData;
import com.r3ct.collection.client.screen.CatalogScreen;
import com.r3ct.collection.scanner.CreativeTabScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BulkSubmitHelper {

    public static class ConflictGroup {
        public final String itemId;
        public final List<CatalogScreen.SlotItem> slotItems;

        public ConflictGroup(String itemId, List<CatalogScreen.SlotItem> slotItems) {
            this.itemId = itemId;
            this.slotItems = slotItems;
        }
    }

    public static class ScanResult {
        public final List<Integer> safeSlots = new ArrayList<>();
        public final List<String> safeItemIds = new ArrayList<>();
        public final List<ConflictGroup> conflicts = new ArrayList<>();
    }

    public static ScanResult scanInventory(String tabId) {
        ScanResult result = new ScanResult();
        if (!CreativeTabScanner.SCANNED_SUBCATEGORIES.containsKey(tabId)) return result;

        CreativeTabScanner.SubCategory cat = CreativeTabScanner.SCANNED_SUBCATEGORIES.get(tabId);
        Inventory inv = Minecraft.getInstance().player.getInventory();

        List<String> missingIds = new ArrayList<>();
        for (ItemStack catStack : cat.items) {
            String uId = ServerItemHandler.getUniqueItemId(catStack);
            if (!ClientPlayerData.unlockedItems.contains(uId)) {
                missingIds.add(uId);
            }
        }

        for (String expectedId : missingIds) {
            List<CatalogScreen.SlotItem> foundItems = new ArrayList<>();

            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack invStack = inv.getItem(i);
                if (!invStack.isEmpty() && ServerItemHandler.getUniqueItemId(invStack).equals(expectedId)) {
                    boolean isDuplicate = false;
                    for (CatalogScreen.SlotItem existing : foundItems) {
                        if (ItemStack.isSameItemSameComponents(existing.stack, invStack)) {
                            existing.stack.setCount(existing.stack.getCount() + invStack.getCount());
                            isDuplicate = true;
                            break;
                        }
                    }
                    if (!isDuplicate) {
                        foundItems.add(new CatalogScreen.SlotItem(invStack, i));
                    }
                }
            }

            if (foundItems.isEmpty()) continue;

            boolean hasConflicts = foundItems.size() > 1;

            if (!hasConflicts) {
                ItemStack stack = foundItems.get(0).stack;
                if (CatalogScreen.isValuable(stack) || stack.isDamaged()) {
                    hasConflicts = true;
                }
            }

            if (hasConflicts) {
                result.conflicts.add(new ConflictGroup(expectedId, foundItems));
            } else {
                result.safeSlots.add(foundItems.get(0).slotId);
                result.safeItemIds.add(expectedId);
            }
        }

        return result;
    }
}
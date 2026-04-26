package com.r3ct.collector.scanner;

import com.r3ct.collector.config.CollectorConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class CreativeTabScanner {

    public static final Map<String, SubCategory> SCANNED_SUBCATEGORIES = new LinkedHashMap<>();

    public static class SubCategory {
        public String tabId;
        public Component displayName; // Gotowy, przetłumaczony tekst od gry!
        public ItemStack icon;
        public List<ItemStack> items = new ArrayList<>();

        public SubCategory(String tabId, Component displayName, ItemStack icon) {
            this.tabId = tabId;
            this.displayName = displayName;
            this.icon = icon;
        }
    }

    public static void scanAllTabs() {
        SCANNED_SUBCATEGORIES.clear();
        Minecraft mc = Minecraft.getInstance();

        CollectorConfig.load();

        if (mc.level == null) {
            System.out.println("[R3CT-Collector] Attempted to scan outside of a world, aborting!");
            return;
        }

        FeatureFlagSet features = mc.level.enabledFeatures();
        boolean hasOp = mc.options.operatorItemsTab().get();
        var registryAccess = mc.level.registryAccess();
        CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(features, hasOp, registryAccess);

        List<Map.Entry<ResourceKey<CreativeModeTab>, CreativeModeTab>> sortedTabs = new ArrayList<>(BuiltInRegistries.CREATIVE_MODE_TAB.entrySet());
        sortedTabs.sort(Comparator.comparingInt(entry -> BuiltInRegistries.CREATIVE_MODE_TAB.getId(entry.getValue())));

        // Lista pamiętająca, co już dodaliśmy (Unikalny klucz = ID + Nazwa)
        Set<String> processedItems = new HashSet<>();

        // --- ETAP 1: IDEALNE ZAKŁADKI VANILLA I INNYCH MODÓW ---
        for (Map.Entry<ResourceKey<CreativeModeTab>, CreativeModeTab> entry : sortedTabs) {
            CreativeModeTab tab = entry.getValue();
            String tabId = entry.getKey().identifier().toString();
            String tabNamespace = tabId.split(":")[0];

            if (CollectorConfig.blacklistedMods.contains(tabNamespace) || CollectorConfig.blacklistedTabs.contains(tabId) || tab.getType() != CreativeModeTab.Type.CATEGORY) {
                continue;
            }

            tab.buildContents(params);
            var displayItems = tab.getDisplayItems();

            if (displayItems != null && !displayItems.isEmpty()) {
                SubCategory category = new SubCategory(tabId, tab.getDisplayName(), tab.getIconItem());

                for (ItemStack stack : displayItems) {
                    String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    String itemNamespace = itemId.split(":")[0];

                    if (!CollectorConfig.blacklistedItems.contains(itemId) && !CollectorConfig.blacklistedMods.contains(itemNamespace)) {

                        // SPRYTNY KLUCZ: Łączymy ID przedmiotu z jego wygenerowaną nazwą.
                        String uniqueKey = itemId + stack.getHoverName().getString();

                        if (!processedItems.contains(uniqueKey)) {
                            category.items.add(stack);
                            processedItems.add(uniqueKey); // Zabezpieczamy przed duplikatem w innej zakładce
                        }
                    }
                }

                if (!category.items.isEmpty()) {
                    SCANNED_SUBCATEGORIES.put(tabId, category);
                }
            }
        }

        System.out.println("[R3CT-Collector] Built and loaded filtered tabs: " + SCANNED_SUBCATEGORIES.size());
    }
}
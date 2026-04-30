package com.r3ct.collector.scanner;

import com.r3ct.collector.config.CollectorConfig;
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
        public Component displayName;
        public ItemStack icon;
        public List<ItemStack> items = new ArrayList<>();

        public SubCategory(String tabId, Component displayName, ItemStack icon) {
            this.tabId = tabId;
            this.displayName = displayName;
            this.icon = icon;
        }
    }

    // Dodano parametry, by metoda działała zarówno u klienta, jak i na serwerze!
    public static void scanAllTabs(FeatureFlagSet features, net.minecraft.core.RegistryAccess registryAccess, boolean hasOp) {
        SCANNED_SUBCATEGORIES.clear();

        CollectorConfig.load();

        CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(features, hasOp, registryAccess);

        List<Map.Entry<ResourceKey<CreativeModeTab>, CreativeModeTab>> sortedTabs = new ArrayList<>(BuiltInRegistries.CREATIVE_MODE_TAB.entrySet());
        sortedTabs.sort(Comparator.comparingInt(entry -> BuiltInRegistries.CREATIVE_MODE_TAB.getId(entry.getValue())));

        Set<String> processedItems = new HashSet<>();

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

                        // NAPRAWA: Zamiast nazwy wyświetlanej, używamy naszego Unikalnego ID!
                        String uniqueKey = com.r3ct.collector.logic.ServerItemHandler.getUniqueItemId(stack);

                        if (!processedItems.contains(uniqueKey)) {
                            category.items.add(stack);
                            processedItems.add(uniqueKey);
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
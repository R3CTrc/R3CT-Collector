package com.r3ct.collection.compat;

import com.r3ct.collection.client.screen.CollectionConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new CollectionConfigScreen(parent);
    }
}
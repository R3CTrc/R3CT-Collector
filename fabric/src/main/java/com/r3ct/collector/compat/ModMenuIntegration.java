package com.r3ct.collector.compat;

import com.r3ct.collector.client.screen.CollectorConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Gdy gracz kliknie przycisk "Config" w Mod Menu, otwieramy nasz główny ekran ustawień
        return parent -> new CollectorConfigScreen(parent);
    }
}
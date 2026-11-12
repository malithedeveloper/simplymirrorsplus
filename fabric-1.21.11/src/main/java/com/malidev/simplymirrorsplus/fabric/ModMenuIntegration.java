package com.malidev.simplymirrorsplus.fabric;

import com.malidev.simplymirrorsplus.client.MirrorSettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Adds a config button to Mod Menu's mod list; only loaded when Mod Menu is installed. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() { return MirrorSettingsScreen::new; }
}

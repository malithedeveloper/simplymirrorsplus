package com.malidev.simplymirrorsplus.fabric;

import com.malidev.simplymirrorsplus.client.MirrorSettingsScreen;
//? if >=1.17 {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
//?} else if >=1.15 {
/*import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
*///?} else {
/*import io.github.prospector.modmenu.api.ModMenuApi;
*///?}

/** Adds a config button to Mod Menu's mod list; only loaded when Mod Menu is installed. */
public final class ModMenuIntegration implements ModMenuApi {
    //? if >=1.15 {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() { return MirrorSettingsScreen::new; }
    //?} else {
    /*@Override public String getModId() { return "simplymirrorsplus"; }
    @Override public java.util.function.Function<net.minecraft.client.gui.screens.Screen,? extends net.minecraft.client.gui.screens.Screen> getConfigScreenFactory() { return MirrorSettingsScreen::new; }
    *///?}
}

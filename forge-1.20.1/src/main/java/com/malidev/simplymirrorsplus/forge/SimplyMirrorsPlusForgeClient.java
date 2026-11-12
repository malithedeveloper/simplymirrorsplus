package com.malidev.simplymirrorsplus.forge;

import com.malidev.simplymirrorsplus.client.MirrorSettingsScreen;

/** Client-only, so dedicated servers never load the screen classes. */
final class SimplyMirrorsPlusForgeClient {
    private SimplyMirrorsPlusForgeClient() { }

    static void register() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new MirrorSettingsScreen(parent)));
    }
}

package com.malidev.simplymirrorsplus.neoforge;

import com.malidev.simplymirrorsplus.client.MirrorSettingsScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only, so dedicated servers never load the screen classes. */
final class SimplyMirrorsPlusNeoforgeClient {
    private SimplyMirrorsPlusNeoforgeClient() { }

    static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new MirrorSettingsScreen(parent));
    }
}

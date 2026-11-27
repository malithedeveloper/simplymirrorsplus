/*!stonecutter-disabled
package com.malidev.simplymirrorsplus.neoforge;

import com.malidev.simplymirrorsplus.client.MirrorSettingsScreen;
import net.neoforged.fml.ModContainer;

/^* Client-only, so dedicated servers never load the screen classes. ^/
final class SimplyMirrorsPlusNeoforgeClient {
    private SimplyMirrorsPlusNeoforgeClient() { }

    static void register(ModContainer container) {
        //? if >=1.20.5 {
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,(mod,parent)->new MirrorSettingsScreen(parent));
        //?} else {
        /^net.neoforged.fml.ModLoadingContext.get().registerExtensionPoint(net.neoforged.neoforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
            ()->new net.neoforged.neoforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft,parent)->new MirrorSettingsScreen(parent)));
        ^///?}
    }
}
*/
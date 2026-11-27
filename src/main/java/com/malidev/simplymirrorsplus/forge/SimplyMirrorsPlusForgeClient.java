/*!stonecutter-disabled
package com.malidev.simplymirrorsplus.forge;

import com.malidev.simplymirrorsplus.client.MirrorSettingsScreen;
import net.minecraftforge.fml.ModLoadingContext;

/^* Client-only, so dedicated servers never load the screen classes. ^/
final class SimplyMirrorsPlusForgeClient {
    private SimplyMirrorsPlusForgeClient() { }

    static void register() {
        //? if >=1.19 {
        ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
            ()->new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft,parent)->new MirrorSettingsScreen(parent)));
        //?} else if >=1.18 {
        /^ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.client.ConfigGuiHandler.ConfigGuiFactory.class,
            ()->new net.minecraftforge.client.ConfigGuiHandler.ConfigGuiFactory((minecraft,parent)->new MirrorSettingsScreen(parent)));
        ^///?} else if >=1.17 {
        /^ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.fmlclient.ConfigGuiHandler.ConfigGuiFactory.class,
            ()->new net.minecraftforge.fmlclient.ConfigGuiHandler.ConfigGuiFactory((minecraft,parent)->new MirrorSettingsScreen(parent)));
        ^///?} else {
        /^ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.fml.ExtensionPoint.CONFIGGUIFACTORY,()->(minecraft,parent)->new MirrorSettingsScreen(parent));
        ^///?}
    }
}
*/
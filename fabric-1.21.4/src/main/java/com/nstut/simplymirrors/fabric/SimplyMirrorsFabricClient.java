package com.nstut.simplymirrors.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.nstut.simplymirrors.client.MirrorConfig;
import com.nstut.simplymirrors.client.MirrorSettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class SimplyMirrorsFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        MirrorConfig.load(FabricLoader.getInstance().getConfigDir().resolve("simply_mirrors.properties"));
        KeyMapping openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.simply_mirrors.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.simply_mirrors"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettings.consumeClick()) client.setScreen(new MirrorSettingsScreen());
        });
    }
}

package com.malidev.simplymirrorsplus.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.malidev.simplymirrorsplus.client.MirrorConfig;
import com.malidev.simplymirrorsplus.client.MirrorSettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class SimplyMirrorsPlusFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        MirrorConfig.load(FabricLoader.getInstance().getConfigDir().resolve("simplymirrorsplus.properties"));
        KeyMapping openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.simplymirrorsplus.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.simplymirrorsplus"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettings.consumeClick()) client.setScreen(new MirrorSettingsScreen());
        });
    }
}

package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.MirrorConfig;
import com.malidev.simplymirrorsplus.client.MirrorSettingsScreen;
import java.io.File;
import java.util.Arrays;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Loader-neutral key registration; it must run before options.txt is read so rebinds persist. */
@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow @Mutable public KeyMapping[] keyMappings;
    @Shadow private File optionsFile;

    @Inject(method="load()V",at=@At("HEAD"))
    private void simplymirrorsplus$registerSettingsKey(CallbackInfo ci) {
        MirrorConfig.load(optionsFile.toPath().resolveSibling("config").resolve("simplymirrorsplus.properties"));
        if(!Arrays.asList(keyMappings).contains(MirrorSettingsScreen.KEY))keyMappings=ArrayUtils.add(keyMappings,MirrorSettingsScreen.KEY);
    }
}

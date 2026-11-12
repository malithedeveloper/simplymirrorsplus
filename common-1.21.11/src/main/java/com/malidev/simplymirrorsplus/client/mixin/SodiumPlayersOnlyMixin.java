package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sodium draws terrain from its own section lists, so clearing the extracted vanilla sections does not hide it. */
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer",remap=false)
public abstract class SodiumPlayersOnlyMixin {
    @Inject(method="drawChunkLayer",at=@At("HEAD"),cancellable=true,require=0)
    private void simplymirrorsplus$noTerrainInPlayersOnly(CallbackInfo ci) {
        if(MirrorRenderer.skipWorldEffects())ci.cancel();
    }
}

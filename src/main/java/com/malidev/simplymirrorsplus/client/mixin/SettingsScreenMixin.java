package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The mirror settings screen draws itself once the game has finished the frame, on top of everything.
 * Every return is hooked: on 26.3 the frame does not end at the last one.
 */
@Mixin(GameRenderer.class)
public abstract class SettingsScreenMixin {
    //? if >=26.3 {
    /*@Inject(method="render()V",at=@At("RETURN"))
    *///?} else if >=1.21 {
    @Inject(method="render(Lnet/minecraft/client/DeltaTracker;Z)V",at=@At("RETURN"))
    //?} else {
    /*@Inject(method="render(FJZ)V",at=@At("RETURN"))
    *///?}
    private void simplymirrorsplus$drawSettings(CallbackInfo ci) { MirrorRenderer.drawScreen(); }
}

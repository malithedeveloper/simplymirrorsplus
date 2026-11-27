package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import com.malidev.simplymirrorsplus.client.MirrorSettingsScreen;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    //? if <26.2 {
    /** Everything that draws to "the screen" during a reflection lands in the mirror's image instead. */
    @Inject(method="getMainRenderTarget",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$target(CallbackInfoReturnable<RenderTarget> ci) {
        if(MirrorRenderer.redirect!=null)ci.setReturnValue(MirrorRenderer.redirect);
    }
    //?}

    @Inject(method="runTick",at=@At("HEAD"))
    private void simplymirrorsplus$unload(CallbackInfo ci) { if(Minecraft.getInstance().level==null)MirrorRenderer.release(); }

    @Inject(method="close",at=@At("HEAD"),remap=false)
    private void simplymirrorsplus$close(CallbackInfo ci) { MirrorRenderer.release(); }

    @Inject(method="tick",at=@At("TAIL"))
    private void simplymirrorsplus$openSettings(CallbackInfo ci) {
        //? if >=26.2 {
        /*while(MirrorSettingsScreen.KEY.consumeClick())((Minecraft)(Object)this).gui.setScreen(new MirrorSettingsScreen());
        *///?} else {
        while(MirrorSettingsScreen.KEY.consumeClick())((Minecraft)(Object)this).setScreen(new MirrorSettingsScreen());
        //?}
    }

    @Inject(method="tick",at=@At("TAIL"))
    private void simplymirrorsplus$nameAimedMirror(CallbackInfo ci) { MirrorSettingsScreen.showAimedName((Minecraft)(Object)this); }

    /** Sneaking and using an empty hand on a mirror opens that mirror's settings instead of using the block. */
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$openMirror(CallbackInfo ci) { if(MirrorSettingsScreen.openFor((Minecraft)(Object)this))ci.cancel(); }

    //? if >=1.16 <1.21.2 {
    /*/^* Keeps render types on the redirected target while a reflection uses the Fancy path. ^/
    @Inject(method="useShaderTransparency",at=@At("HEAD"),cancellable=true)
    private static void simplymirrorsplus$fancyInReflection(CallbackInfoReturnable<Boolean> ci) { if(MirrorRenderer.redirect!=null)ci.setReturnValue(false); }
    *///?}
}

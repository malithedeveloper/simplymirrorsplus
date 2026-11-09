package com.malidev.simplymirrorsplus.client.mixin;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method="getMainRenderTarget",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$target(CallbackInfoReturnable<RenderTarget> ci) {
        if(MirrorRenderer.redirect!=null)ci.setReturnValue(MirrorRenderer.redirect);
    }
    @Inject(method="runTick",at=@At("HEAD"))
    private void simplymirrorsplus$unload(CallbackInfo ci) { if(Minecraft.getInstance().level==null)MirrorRenderer.release(); }
    @Inject(method="close",at=@At("HEAD"),remap=false)
    private void simplymirrorsplus$close(CallbackInfo ci) { MirrorRenderer.release(); }
    @Inject(method="tick",at=@At("TAIL"))
    private void simplymirrorsplus$openSettings(CallbackInfo ci) {
        while(com.malidev.simplymirrorsplus.client.MirrorSettingsScreen.KEY.consumeClick())((Minecraft)(Object)this).setScreen(new com.malidev.simplymirrorsplus.client.MirrorSettingsScreen());
    }
    /** Keeps render types on the redirected target while a reflection uses the Fancy path. */
    @Inject(method="useShaderTransparency",at=@At("HEAD"),cancellable=true)
    private static void simplymirrorsplus$fancyInReflection(CallbackInfoReturnable<Boolean> ci) { if(MirrorRenderer.redirect!=null)ci.setReturnValue(false); }
}

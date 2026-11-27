//? if >=26.1 {
/*package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.ExtractRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    // Capture the actual world projection, including view bobbing, hurt tilt, and nausea.
    @ModifyArg(method="renderLevel", at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;", ordinal=0), index=0)
    private Matrix4f simplymirrorsplus$captureProjection(Matrix4f projection) {
        ExtractRenderer.mainProjection.set(projection);
        return projection;
    }

    //? if >=26.2 {
    /^/^* Every pass reads the main target through here, so reflections draw into the mirror image. ^/
    @org.spongepowered.asm.mixin.injection.Inject(method="mainRenderTarget",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$target(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<com.mojang.blaze3d.pipeline.RenderTarget> ci) {
        if(com.malidev.simplymirrorsplus.client.MirrorRenderer.redirect!=null)ci.setReturnValue(com.malidev.simplymirrorsplus.client.MirrorRenderer.redirect);
    }
    ^///?}
}
*///?}

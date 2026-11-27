//? if <1.15 {
/*package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.LegacyRenderer;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^* The hooks a reflection needs inside the 1.14 world render, which sets its camera, projection and viewport itself. ^/
@Mixin(GameRenderer.class)
public abstract class LegacyGameRendererMixin {
    @Inject(method="render(FJ)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",shift=At.Shift.AFTER))
    private void simplymirrorsplus$afterCamera(float partial,long deadline,CallbackInfo ci) {
        LegacyRenderer.afterCamera((GameRenderer)(Object)this,partial,deadline);
    }

    @Inject(method="render(FJ)V",at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z"))
    private void simplymirrorsplus$beforeHand(float partial,long deadline,CallbackInfo ci) { LegacyRenderer.beforeHand(); }

    /^* The sky and the clouds each load a projection of their own, which would drop the mirror's clip plane and crop. ^/
    @Redirect(method={"render(FJ)V","setupCamera","prepareAndRenderClouds"},at=@At(value="INVOKE",target="Lcom/mojang/math/Matrix4f;perspective(DFFF)Lcom/mojang/math/Matrix4f;"))
    private Matrix4f simplymirrorsplus$projection(double fov,float aspect,float near,float far) {
        Matrix4f reflection=LegacyRenderer.projection();
        return reflection!=null?reflection:Matrix4f.perspective(fov,aspect,near,far);
    }

    /^* The world render sets a window-sized viewport; a mirror image is usually smaller. ^/
    @Redirect(method="render(FJ)V",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/GlStateManager;viewport(IIII)V"))
    private void simplymirrorsplus$viewport(int x,int y,int width,int height) {
        RenderTarget target=MirrorRenderer.redirect;
        if(target!=null)GlStateManager.viewport(0,0,target.width,target.height);else GlStateManager.viewport(x,y,width,height);
    }
}
*///?}

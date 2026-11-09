package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.ShaderReflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Iris 1.8 can leave a packed stencil image attached when replacing its
 * gbuffer depth attachment with a depth-only mirror texture. This is rejected
 * by OpenGL before the next uniform upload (some draws and readbacks have
 * already begun), so handle binding and attachment updates, not just programs.
 *
 * No Iris API is linked in the production mod; this optional mixin does
 * nothing unless Iris exists and a mirror render is actively in progress.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.gl.framebuffer.GlFramebuffer", remap = false)
public abstract class IrisFramebufferParityMixin {
    @Shadow public abstract int getId();

    @Inject(method = "addDepthAttachment", at = @At("RETURN"), require = 0)
    private void simplymirrorsplus$depthAttached(int depthTexture, CallbackInfo ci) {
        ShaderReflection.onIrisFramebufferBound(getId());
    }

    @Inject(method = "bind", at = @At("RETURN"), require = 0)
    private void simplymirrorsplus$framebufferBound(CallbackInfo ci) {
        ShaderReflection.onIrisFramebufferBound(getId());
    }

    @Inject(method = "bindAsDrawBuffer", at = @At("RETURN"), require = 0)
    private void simplymirrorsplus$drawBound(CallbackInfo ci) {
        ShaderReflection.onIrisFramebufferBound(getId());
    }

    @Inject(method = "bindAsReadBuffer", at = @At("RETURN"), require = 0)
    private void simplymirrorsplus$readBound(CallbackInfo ci) {
        ShaderReflection.onIrisFramebufferBound(getId());
    }
}

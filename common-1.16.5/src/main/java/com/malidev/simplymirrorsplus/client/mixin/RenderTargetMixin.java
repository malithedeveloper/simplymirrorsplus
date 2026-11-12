package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Before drawing entities Minecraft clears the window-sized outline target and rebinds the main target
 * without restoring the viewport. A mirror image smaller than the window then draws its entities off-image.
 */
@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {
    @ModifyVariable(method = "bindWrite", at = @At("HEAD"), argsOnly = true)
    private boolean simplymirrorsplus$fullViewport(boolean setViewport) {
        return setViewport || (Object) this == MirrorRenderer.redirect;
    }
}

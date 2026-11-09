package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fitting the camera cube loops forever for some mirrored cameras, and only matters around the real camera. */
@Mixin(Frustum.class)
public abstract class FrustumMixin {
    @Inject(method="offsetToFullyIncludeCameraCube",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$noCameraCubeInReflection(int size,CallbackInfoReturnable<Frustum> ci) {
        if(MirrorRenderer.redirect!=null)ci.setReturnValue((Frustum)(Object)this);
    }
}

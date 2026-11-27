//? if >=26.3 {
/*package com.malidev.simplymirrorsplus.client.mixin;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/^* Clouds keep a mesh built around the player's camera; rebuilding it for each reflection is costly. ^/
@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    @Inject(method={"prepare(ILnet/minecraft/client/CloudStatus;FILnet/minecraft/world/phys/Vec3;JF)V","render","renderOit"},at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$skipReflectionClouds(CallbackInfo ci) { if(MirrorRenderer.redirect!=null)ci.cancel(); }
}
*///?}

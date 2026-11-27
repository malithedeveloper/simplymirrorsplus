//? if >=26.1 {
/*package com.malidev.simplymirrorsplus.client.mixin;
import com.malidev.simplymirrorsplus.client.ExtractRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/^* Sodium obtains projection from GameRenderer instead of the reflected renderLevel argument. ^/
@Mixin(value=GameRenderer.class,priority=900)
public abstract class SodiumProjectionMixin {
    @Inject(method="sodium$getProjectionMatrix",at=@At("HEAD"),cancellable=true,remap=false,require=0)
    private void simplymirrorsplus$projection(CallbackInfoReturnable<Matrix4fc> ci){if(ExtractRenderer.reflectionProjection!=null)ci.setReturnValue(ExtractRenderer.reflectionProjection);}
}
*///?}

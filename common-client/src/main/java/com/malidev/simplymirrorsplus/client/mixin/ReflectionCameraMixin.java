package com.malidev.simplymirrorsplus.client.mixin;
import com.malidev.simplymirrorsplus.client.ShaderReflection;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(GameRenderer.class)
public abstract class ReflectionCameraMixin {
    @Inject(method="getMainCamera",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$camera(CallbackInfoReturnable<Camera> ci){if(ShaderReflection.camera!=null)ci.setReturnValue((Camera)ShaderReflection.camera);}
}

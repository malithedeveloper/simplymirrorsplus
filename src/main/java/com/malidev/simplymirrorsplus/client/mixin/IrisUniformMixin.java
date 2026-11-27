package com.malidev.simplymirrorsplus.client.mixin;
import com.malidev.simplymirrorsplus.client.ShaderReflection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Upload every draw: Iris's once-per-frame cache cannot observe a reflected camera. */
@Pseudo
@Mixin(targets={"net.irisshaders.iris.gl.program.ProgramUniforms","net.coderbot.iris.gl.program.ProgramUniforms"},remap=false)
public abstract class IrisUniformMixin {
    @Inject(method="update",at=@At("HEAD"),require=0)
    private void simplymirrorsplus$mainCamera(CallbackInfo ci){ShaderReflection.beforeUniforms();}
    @Inject(method="update",at=@At("RETURN"),require=0)
    private void simplymirrorsplus$plane(CallbackInfo ci){ShaderReflection.afterUniforms();}
}

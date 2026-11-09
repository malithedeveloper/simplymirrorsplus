package com.malidev.simplymirrorsplus.client.mixin;
import com.malidev.simplymirrorsplus.client.ShaderReflection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** FallbackShader binds programs directly and never calls ProgramUniforms.update. */
@Pseudo
@Mixin(targets="net.irisshaders.iris.pipeline.programs.FallbackShader",remap=false)
public abstract class IrisFallbackMixin {
    @Inject(method={"apply","iris$setupState"},at=@At("RETURN"),require=0)
    private void simplymirrorsplus$unpatchedProgram(CallbackInfo ci){ShaderReflection.upload();}
}

package com.malidev.simplymirrorsplus.client.mixin;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import com.malidev.simplymirrorsplus.client.ShaderReflection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static org.lwjgl.opengl.GL33.*;
/** Shadow maps use the light's camera and ordinary winding, without the mirror clip plane. */
@Pseudo
@Mixin(targets={"net.irisshaders.iris.pipeline.IrisRenderingPipeline","net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline","net.coderbot.iris.pipeline.DeferredWorldRenderingPipeline"},remap=false)
public abstract class IrisShadowMixin {
    @Inject(method="beginLevelRendering",at=@At("RETURN"),require=0)
    private void simplymirrorsplus$terrainVisibility(CallbackInfo ci){
        if(MirrorRenderer.redirect!=null)net.minecraft.client.Minecraft.getInstance().smartCull=false;
    }
    @Unique private boolean simplymirrorsplus$clip;
    @Unique private int simplymirrorsplus$front;
    @Inject(method="renderShadows",at=@At("HEAD"),require=0)
    private void simplymirrorsplus$shadow(CallbackInfo ci){if(MirrorRenderer.redirect!=null){simplymirrorsplus$clip=ShaderReflection.suspend();simplymirrorsplus$front=glGetInteger(GL_FRONT_FACE);glFrontFace(GL_CCW);}}
    @Inject(method="renderShadows",at=@At("RETURN"),require=0)
    private void simplymirrorsplus$restore(CallbackInfo ci){if(MirrorRenderer.redirect!=null){ShaderReflection.resume(simplymirrorsplus$clip);glFrontFace(simplymirrorsplus$front);}}
}

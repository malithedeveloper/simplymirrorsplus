package com.malidev.simplymirrorsplus.client.mixin;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import com.malidev.simplymirrorsplus.client.ShaderReflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static org.lwjgl.opengl.GL33.*;
/** Shader screen-space passes retain ordinary winding in a reflected world. */
@Pseudo
@Mixin(targets={"net.irisshaders.iris.pathways.FullScreenQuadRenderer","net.coderbot.iris.pathways.FullScreenQuadRenderer","net.coderbot.iris.postprocess.FullScreenQuadRenderer",
    "net.irisshaders.iris.pipeline.CompositeRenderer","net.irisshaders.iris.pipeline.FinalPassRenderer",
    "net.irisshaders.iris.pathways.HorizonRenderer"},remap=false)
public abstract class IrisFullscreenMixin {
    @Unique private int simplymirrorsplus$frontFace;
    @Unique private boolean simplymirrorsplus$clip;
    @Inject(method={"renderQuad","renderAll","renderFinalPass","renderHorizon"},at=@At("HEAD"),require=0)
    private void simplymirrorsplus$screenWinding(CallbackInfo ci) {
        if(MirrorRenderer.redirect!=null){simplymirrorsplus$frontFace=glGetInteger(GL_FRONT_FACE);glFrontFace(GL_CCW);simplymirrorsplus$clip=ShaderReflection.suspend();}
    }
    @Inject(method={"renderQuad","renderAll","renderFinalPass","renderHorizon"},at=@At("RETURN"),require=0)
    private void simplymirrorsplus$restoreWinding(CallbackInfo ci) {
        if(MirrorRenderer.redirect!=null){glFrontFace(simplymirrorsplus$frontFace);ShaderReflection.resume(simplymirrorsplus$clip);}
    }
}

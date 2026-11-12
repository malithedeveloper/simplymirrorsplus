package com.malidev.simplymirrorsplus.client.mixin;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=LevelRenderer.class,priority=900)
public abstract class LevelRendererMixin {
    @Shadow @Final private LevelRenderState levelRenderState;
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(GraphicsResourceAllocator allocator,DeltaTracker delta,boolean outline,Camera camera,Matrix4f view,Matrix4f projection,
            Matrix4f cullProjection,GpuBufferSlice fog,Vector4f fogColor,boolean sky,CallbackInfo ci) {
        MirrorRenderer.render(allocator,delta,camera,view,projection,fog,fogColor,sky);
    }
    /** Extraction is finished once the frame graph starts; a Players Only reflection trims the world out here. */
    @Inject(method="renderLevel",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/systems/RenderSystem;getModelViewStack()Lorg/joml/Matrix4fStack;",ordinal=0))
    private void simplymirrorsplus$afterExtract(CallbackInfo ci) { MirrorRenderer.afterExtract(levelRenderState); }
    @Inject(method={"addCloudsPass*","addParticlesPass*","addWeatherPass*"},at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$noWorldEffects(CallbackInfo ci) { if(MirrorRenderer.skipWorldEffects())ci.cancel(); }
    /** Post passes keep the cull state, so the flipped winding of a reflection discards their fullscreen quad and leaves the image black. */
    @Inject(method="getTransparencyChain",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$noFabulousInReflection(CallbackInfoReturnable<PostChain> ci) { if(MirrorRenderer.redirect!=null)ci.setReturnValue(null); }
}

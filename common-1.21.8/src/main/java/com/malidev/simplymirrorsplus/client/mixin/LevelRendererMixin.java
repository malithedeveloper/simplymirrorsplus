package com.malidev.simplymirrorsplus.client.mixin;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=LevelRenderer.class,priority=900)
public abstract class LevelRendererMixin {
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(GraphicsResourceAllocator allocator, DeltaTracker delta, boolean outline, Camera camera, Matrix4f view, Matrix4f projection, GpuBufferSlice fog, Vector4f fogColor, boolean sky, CallbackInfo ci) { MirrorRenderer.render(allocator, delta, camera, view, projection, fog, fogColor, sky); }
    /** Post passes keep the cull state, so the flipped winding of a reflection discards their fullscreen quad and leaves the image black. */
    @Inject(method="getTransparencyChain",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$noFabulousInReflection(CallbackInfoReturnable<PostChain> ci) { if(MirrorRenderer.redirect!=null)ci.setReturnValue(null); }
}

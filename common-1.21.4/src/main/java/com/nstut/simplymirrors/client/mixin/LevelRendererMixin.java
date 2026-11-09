package com.nstut.simplymirrors.client.mixin;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.nstut.simplymirrors.client.MirrorRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=LevelRenderer.class,priority=900)
public abstract class LevelRendererMixin {
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void simplymirrors$reflect(GraphicsResourceAllocator allocator, DeltaTracker delta, boolean outline, Camera camera, GameRenderer game, Matrix4f view, Matrix4f projection, CallbackInfo ci) { MirrorRenderer.render(allocator, delta, camera, game, view, projection); }
    /** Post passes keep the cull state, so the flipped winding of a reflection discards their fullscreen quad and leaves the image black. */
    @Inject(method="getTransparencyChain",at=@At("HEAD"),cancellable=true)
    private void simplymirrors$noFabulousInReflection(CallbackInfoReturnable<PostChain> ci) { if(MirrorRenderer.redirect!=null)ci.setReturnValue(null); }
}

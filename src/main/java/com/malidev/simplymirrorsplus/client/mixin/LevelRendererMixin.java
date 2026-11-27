package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.15 <1.21.9 {
import com.malidev.simplymirrorsplus.client.DirectRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
//?}

/** Puts the reflections on the mirrors once the player's own view of the world has been drawn. */
@Mixin(value=LevelRenderer.class,priority=900)
public abstract class LevelRendererMixin {
    //? if >=26.3 {
    /*@Inject(method="render",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,boolean outline,
            net.minecraft.client.renderer.state.level.CameraRenderState camera,com.mojang.blaze3d.buffers.GpuBufferSlice fog,org.joml.Vector4f color,boolean sky,
            boolean consistentDepth,CallbackInfo ci) {
        com.malidev.simplymirrorsplus.client.ExtractRenderer.render(allocator,net.minecraft.client.Minecraft.getInstance().getDeltaTracker(),camera,camera.viewRotationMatrix,fog,color,sky);
    }
    *///?} else if >=26.2 {
    /*@Inject(method="render",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,net.minecraft.client.DeltaTracker delta,boolean outline,
            net.minecraft.client.renderer.state.level.CameraRenderState camera,org.joml.Matrix4fc view,com.mojang.blaze3d.buffers.GpuBufferSlice fog,org.joml.Vector4f color,boolean sky,CallbackInfo ci) {
        com.malidev.simplymirrorsplus.client.ExtractRenderer.render(allocator,delta,camera,view,fog,color,sky);
    }
    *///?} else if >=26.1 {
    /*@Inject(method="renderLevel",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,net.minecraft.client.DeltaTracker delta,boolean outline,
            net.minecraft.client.renderer.state.level.CameraRenderState camera,org.joml.Matrix4fc view,com.mojang.blaze3d.buffers.GpuBufferSlice fog,org.joml.Vector4f color,boolean sky,
            net.minecraft.client.renderer.chunk.ChunkSectionsToRender sections,CallbackInfo ci) {
        com.malidev.simplymirrorsplus.client.ExtractRenderer.render(allocator,delta,camera,view,fog,color,sky);
    }
    *///?} else if >=1.21.9 {
    /*@org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.client.renderer.state.LevelRenderState levelRenderState;

    @Inject(method="renderLevel",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,net.minecraft.client.DeltaTracker delta,boolean outline,
            net.minecraft.client.Camera camera,org.joml.Matrix4f view,org.joml.Matrix4f projection,org.joml.Matrix4f cullProjection,
            com.mojang.blaze3d.buffers.GpuBufferSlice fog,org.joml.Vector4f fogColor,boolean sky,CallbackInfo ci) {
        com.malidev.simplymirrorsplus.client.StateRenderer.render(allocator,delta,camera,view,projection,fog,fogColor,sky,false);
    }

    /^* Extraction is finished once the frame graph starts; a Players Only reflection trims the world out here. ^/
    @Inject(method="renderLevel",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/systems/RenderSystem;getModelViewStack()Lorg/joml/Matrix4fStack;",ordinal=0))
    private void simplymirrorsplus$afterExtract(CallbackInfo ci) { com.malidev.simplymirrorsplus.client.StateRenderer.afterExtract(levelRenderState); }

    @Inject(method={"addCloudsPass*","addParticlesPass*","addWeatherPass*"},at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$noWorldEffects(CallbackInfo ci) { if(MirrorRenderer.playersOnlyPass())ci.cancel(); }
    *///?} else if >=1.21.6 {
    /*@Inject(method="renderLevel",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,net.minecraft.client.DeltaTracker delta,boolean outline,Camera camera,
            org.joml.Matrix4f view,org.joml.Matrix4f projection,com.mojang.blaze3d.buffers.GpuBufferSlice fog,org.joml.Vector4f fogColor,boolean sky,CallbackInfo ci) {
        DirectRenderer.render(allocator,delta,camera,view,projection,fog,fogColor,sky,false);
    }
    *///?} else if >=1.21.4 {
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,net.minecraft.client.DeltaTracker delta,boolean outline,Camera camera,
            GameRenderer game,org.joml.Matrix4f view,org.joml.Matrix4f projection,CallbackInfo ci) {
        DirectRenderer.render(allocator,delta,camera,game,view,projection,false);
    }
    //?} else if >=1.21.2 {
    /*@Inject(method="renderLevel",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,net.minecraft.client.DeltaTracker delta,boolean outline,Camera camera,
            GameRenderer game,net.minecraft.client.renderer.LightTexture light,org.joml.Matrix4f view,org.joml.Matrix4f projection,CallbackInfo ci) {
        DirectRenderer.render(allocator,delta,camera,game,view,projection,false);
    }
    *///?} else if >=1.21 {
    /*@Inject(method="renderLevel",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(net.minecraft.client.DeltaTracker delta,boolean outline,Camera camera,GameRenderer game,
            net.minecraft.client.renderer.LightTexture light,org.joml.Matrix4f view,org.joml.Matrix4f projection,CallbackInfo ci) {
        DirectRenderer.render(delta,camera,game,light,view,projection,false);
    }
    *///?} else if >=1.20.5 {
    /*@Inject(method="renderLevel",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(float partial,long deadline,boolean outline,Camera camera,GameRenderer game,
            net.minecraft.client.renderer.LightTexture light,org.joml.Matrix4f view,org.joml.Matrix4f projection,CallbackInfo ci) {
        DirectRenderer.render(partial,deadline,camera,game,light,view,projection,false);
    }
    *///?} else if >=1.19.3 {
    /*@Inject(method="renderLevel",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(com.mojang.blaze3d.vertex.PoseStack view,float partial,long deadline,boolean outline,Camera camera,GameRenderer game,
            net.minecraft.client.renderer.LightTexture light,org.joml.Matrix4f projection,CallbackInfo ci) {
        DirectRenderer.render(view,partial,deadline,camera,game,light,projection,false);
    }
    *///?} else if >=1.15 {
    /*@Inject(method="renderLevel",at=@At("TAIL"))
    private void simplymirrorsplus$reflect(com.mojang.blaze3d.vertex.PoseStack view,float partial,long deadline,boolean outline,Camera camera,GameRenderer game,
            net.minecraft.client.renderer.LightTexture light,com.mojang.math.Matrix4f projection,CallbackInfo ci) {
        DirectRenderer.render(view,partial,deadline,camera,game,light,projection,false);
    }
    *///?}

    //? if >=26.2 {
    /*/^* Section compilation follows the main camera; a reflection must not reorder it. ^/
    @Inject(method="compileSections",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$noCompileInReflection(CallbackInfo ci) { if(MirrorRenderer.redirect!=null)ci.cancel(); }
    *///?}

    //? if >=26.1 <26.3 {
    /*@Inject(method="addCloudsPass*",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$skipReflectionClouds(CallbackInfo ci) { if(MirrorRenderer.redirect!=null)ci.cancel(); }
    *///?}

    //? if >=1.21.2 <26.3 {
    /** Post passes keep the cull state, so the flipped winding of a reflection discards their fullscreen quad and leaves the image black. */
    @Inject(method="getTransparencyChain",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$noFabulousInReflection(CallbackInfoReturnable<net.minecraft.client.renderer.PostChain> ci) { if(MirrorRenderer.redirect!=null)ci.setReturnValue(null); }
    //?}
}

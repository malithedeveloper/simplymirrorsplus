//? if >=1.15 <26.1 {
package com.malidev.simplymirrorsplus.client.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <1.21.9 {
import com.malidev.simplymirrorsplus.client.DirectRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
//?}

/**
 * Draws the reflections before the player's own view. Minimap and waypoint mods remember the camera of the last
 * world render in a frame, so that render has to be the player's; the high priority puts this ahead of their hooks.
 */
@Mixin(value=LevelRenderer.class,priority=1500)
public abstract class LevelRendererBeforeMixin {
    //? if >=1.21.9 {
        /*@Inject(method="renderLevel",at=@At("HEAD"))
    private void simplymirrorsplus$capture(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,net.minecraft.client.DeltaTracker delta,boolean outline,
            net.minecraft.client.Camera camera,org.joml.Matrix4f view,org.joml.Matrix4f projection,org.joml.Matrix4f cullProjection,
            com.mojang.blaze3d.buffers.GpuBufferSlice fog,org.joml.Vector4f fogColor,boolean sky,CallbackInfo ci) {
        com.malidev.simplymirrorsplus.client.StateRenderer.render(allocator,delta,camera,view,projection,fog,fogColor,sky,true);
    }
    *///?} else if >=1.21.6 {
    /*@Inject(method="renderLevel",at=@At("HEAD"))
    private void simplymirrorsplus$capture(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,net.minecraft.client.DeltaTracker delta,boolean outline,Camera camera,
            org.joml.Matrix4f view,org.joml.Matrix4f projection,com.mojang.blaze3d.buffers.GpuBufferSlice fog,org.joml.Vector4f fogColor,boolean sky,CallbackInfo ci) {
        DirectRenderer.render(allocator,delta,camera,view,projection,fog,fogColor,sky,true);
    }
    *///?} else if >=1.21.4 {
    @Inject(method="renderLevel",at=@At("HEAD"))
    private void simplymirrorsplus$capture(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,net.minecraft.client.DeltaTracker delta,boolean outline,Camera camera,
            GameRenderer game,org.joml.Matrix4f view,org.joml.Matrix4f projection,CallbackInfo ci) {
        DirectRenderer.render(allocator,delta,camera,game,view,projection,true);
    }
    //?} else if >=1.21.2 {
    /*@Inject(method="renderLevel",at=@At("HEAD"))
    private void simplymirrorsplus$capture(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,net.minecraft.client.DeltaTracker delta,boolean outline,Camera camera,
            GameRenderer game,net.minecraft.client.renderer.LightTexture light,org.joml.Matrix4f view,org.joml.Matrix4f projection,CallbackInfo ci) {
        DirectRenderer.render(allocator,delta,camera,game,view,projection,true);
    }
    *///?} else if >=1.21 {
    /*@Inject(method="renderLevel",at=@At("HEAD"))
    private void simplymirrorsplus$capture(net.minecraft.client.DeltaTracker delta,boolean outline,Camera camera,GameRenderer game,
            net.minecraft.client.renderer.LightTexture light,org.joml.Matrix4f view,org.joml.Matrix4f projection,CallbackInfo ci) {
        DirectRenderer.render(delta,camera,game,light,view,projection,true);
    }
    *///?} else if >=1.20.5 {
    /*@Inject(method="renderLevel",at=@At("HEAD"))
    private void simplymirrorsplus$capture(float partial,long deadline,boolean outline,Camera camera,GameRenderer game,
            net.minecraft.client.renderer.LightTexture light,org.joml.Matrix4f view,org.joml.Matrix4f projection,CallbackInfo ci) {
        DirectRenderer.render(partial,deadline,camera,game,light,view,projection,true);
    }
    *///?} else if >=1.19.3 {
    /*@Inject(method="renderLevel",at=@At("HEAD"))
    private void simplymirrorsplus$capture(com.mojang.blaze3d.vertex.PoseStack view,float partial,long deadline,boolean outline,Camera camera,GameRenderer game,
            net.minecraft.client.renderer.LightTexture light,org.joml.Matrix4f projection,CallbackInfo ci) {
        DirectRenderer.render(view,partial,deadline,camera,game,light,projection,true);
    }
    *///?} else {
    /*@Inject(method="renderLevel",at=@At("HEAD"))
    private void simplymirrorsplus$capture(com.mojang.blaze3d.vertex.PoseStack view,float partial,long deadline,boolean outline,Camera camera,GameRenderer game,
            net.minecraft.client.renderer.LightTexture light,com.mojang.math.Matrix4f projection,CallbackInfo ci) {
        DirectRenderer.render(view,partial,deadline,camera,game,light,projection,true);
    }
    *///?}
}
//?}

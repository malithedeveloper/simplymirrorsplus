//? if <26.1 {
package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import com.malidev.simplymirrorsplus.client.ShaderReflection;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Picks the terrain a reflection draws. Vanilla walks its occlusion graph from the camera, which for a camera
 * behind the mirror wall means a full walk on the render thread and a visible hitch. A reflection takes every
 * loaded section inside its view instead; the mirror plane clips what lies behind the wall.
 * The priority runs this before terrain replacement hooks can cancel the vanilla method.
 */
@Mixin(value=LevelRenderer.class,priority=1100)
public abstract class ReflectionCullingMixin {
    @Shadow private ViewArea viewArea;
    //? if >=1.20.2 {
    @Shadow @Final private it.unimi.dsi.fastutil.objects.ObjectArrayList<net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection> visibleSections;
    //?} else if >=1.18 {
    /*@Shadow @Final private it.unimi.dsi.fastutil.objects.ObjectArrayList<?> renderChunksInFrustum;
    *///?} else if >=1.17 {
    /*@Shadow @Final private it.unimi.dsi.fastutil.objects.ObjectArrayList<?> renderChunks;
    *///?} else if >=1.15 {
    /*@Shadow @Final private it.unimi.dsi.fastutil.objects.ObjectList<?> renderChunks;
    *///?}
    //? if >=1.15 <1.18
    //@Shadow private net.minecraft.client.renderer.chunk.ChunkRenderDispatcher chunkRenderDispatcher;

    //? if >=1.21.9 {
    /*@Inject(method="cullTerrain",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$reflectionCulling(Camera camera,Frustum frustum,boolean spectator,CallbackInfo ci) {
    *///?} else if >=1.18 {
    @Inject(method="setupRender",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$reflectionCulling(Camera camera,Frustum frustum,boolean capturedFrustum,boolean spectator,CallbackInfo ci) {
    //?} else if >=1.15 {
    /*@Inject(method="setupRender",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$reflectionCulling(Camera camera,Frustum frustum,boolean capturedFrustum,int frame,boolean spectator,CallbackInfo ci) {
    *///?} else {
    /*@Inject(method="setupRender",at=@At("HEAD"))
    private void simplymirrorsplus$reflectionCulling(Camera camera,net.minecraft.client.renderer.culling.Culler culler,int frame,boolean spectator,CallbackInfo ci) {
    *///?}
        if(MirrorRenderer.redirect==null)return;
        // Iris and Oculus reset this at the start of renderLevel. Terrain renderers that cull by themselves read it.
        Minecraft.getInstance().smartCull=false;
        // ponytail: 1.14 keeps vanilla's walk for reflections; give it the frustum fill if the hitch shows there too.
        //? if >=1.15 {
        // Sodium and its forks keep their own section lists and never read the vanilla one.
        if(ShaderReflection.terrainRendererPresent()||viewArea==null)return;
        //? if >=1.20.2 {
        visibleSections.clear();
        for(net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection section:viewArea.sections)
            if(frustum.isVisible(section.getBoundingBox()))visibleSections.add(section);
        //?} else if >=1.18 {
        /*com.malidev.simplymirrorsplus.client.ReflectionSections.fill((LevelRenderer)(Object)this,viewArea,frustum,renderChunksInFrustum);
        *///?} else {
        /*com.malidev.simplymirrorsplus.client.ReflectionSections.fill((LevelRenderer)(Object)this,viewArea,frustum,renderChunks,chunkRenderDispatcher);
        *///?}
        ci.cancel();
        //?}
    }

    //? if >=1.21.2 {
    /** Fitting the camera cube loops forever for some mirrored cameras, and only matters around the real camera. */
    @Inject(method="offsetFrustum",at=@At("HEAD"),cancellable=true)
    private static void simplymirrorsplus$noCameraCubeInReflection(Frustum frustum,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Frustum> ci) {
        if(MirrorRenderer.redirect!=null)ci.setReturnValue(new Frustum(frustum));
    }
    //?}
}
//?}

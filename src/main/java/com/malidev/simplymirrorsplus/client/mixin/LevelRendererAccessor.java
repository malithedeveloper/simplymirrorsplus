package com.malidev.simplymirrorsplus.client.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    //? if >=1.15 <1.21.9 {
    /** The terrain LevelRenderer draws, as records before 1.20.2 and as sections from then on. */
    //? if >=1.20.2 {
    @Accessor("visibleSections") it.unimi.dsi.fastutil.objects.ObjectArrayList<?> simplymirrorsplus$visible();
    //?} else if >=1.18 {
    /*@Accessor("renderChunksInFrustum") it.unimi.dsi.fastutil.objects.ObjectArrayList<?> simplymirrorsplus$visible();
    *///?} else if >=1.17 {
    /*@Accessor("renderChunks") it.unimi.dsi.fastutil.objects.ObjectArrayList<?> simplymirrorsplus$visible();
    *///?} else {
    /*@Accessor("renderChunks") it.unimi.dsi.fastutil.objects.ObjectList<?> simplymirrorsplus$visible();
    *///?}
    //?}

    //? if >=1.18 <1.21.9 {
    /** The frustum the game culls the player's own view with; reflections replace it and must put it back. */
    @Accessor("cullingFrustum") net.minecraft.client.renderer.culling.Frustum simplymirrorsplus$cullingFrustum();
    @Accessor("cullingFrustum") void simplymirrorsplus$setCullingFrustum(net.minecraft.client.renderer.culling.Frustum frustum);
    //?}

    //? if >=26.2 {
    /*@Accessor("levelRenderState") net.minecraft.client.renderer.state.level.LevelRenderState simplymirrorsplus$state();
    *///?} else if >=26.1 {
    /*@Accessor("levelRenderState") net.minecraft.client.renderer.state.level.LevelRenderState simplymirrorsplus$state();
    @Accessor("viewArea") net.minecraft.client.renderer.ViewArea simplymirrorsplus$viewArea();
    *///?} else if >=1.21.9 {
    /*@Accessor("levelRenderState") net.minecraft.client.renderer.state.LevelRenderState simplymirrorsplus$state();
    *///?} else if >=1.21.2 {
    @Accessor("skyRenderer") net.minecraft.client.renderer.SkyRenderer simplymirrorsplus$skyRenderer();
    //?} else {
    /*// Fabulous graphics, and with it this chain, arrived in 1.16.
    //? if >=1.16 {
    @Accessor("transparencyChain") net.minecraft.client.renderer.PostChain simplymirrorsplus$transparencyChain();
    @Accessor("transparencyChain") void simplymirrorsplus$setTransparencyChain(net.minecraft.client.renderer.PostChain chain);
    //?}
    //? if >=1.15 {
    @Invoker("renderEndSky") void simplymirrorsplus$renderEndSky(com.mojang.blaze3d.vertex.PoseStack pose);
    //?} else {
    /^@Invoker("renderEndSky") void simplymirrorsplus$renderEndSky();
    ^///?}
    *///?}
}

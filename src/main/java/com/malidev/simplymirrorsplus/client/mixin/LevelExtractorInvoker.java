//? if >=26.2 {
/*package com.malidev.simplymirrorsplus.client.mixin;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(LevelExtractor.class)
public interface LevelExtractorInvoker {
    @Invoker("extractVisibleEntities") void simplymirrorsplus$entities(Camera camera,Frustum frustum,DeltaTracker delta,LevelRenderState output);
    @Invoker("extractVisibleBlockEntities") void simplymirrorsplus$blockEntities(Camera camera,float partial,LevelRenderState output);
}
*///?}

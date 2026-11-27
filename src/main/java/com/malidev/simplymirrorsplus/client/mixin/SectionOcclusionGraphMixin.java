//? if >=26.1 {
/*package com.malidev.simplymirrorsplus.client.mixin;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/^* The occlusion graph belongs to the player's camera; reflections choose their sections themselves. ^/
@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphMixin {
    @Inject(method="update",at=@At("HEAD"),cancellable=true)
    private void simplymirrorsplus$keepMainGraph(CallbackInfo ci) { if(MirrorRenderer.redirect!=null)ci.cancel(); }
}
*///?}

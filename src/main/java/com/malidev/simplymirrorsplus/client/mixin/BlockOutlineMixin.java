package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.block.MirrorBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The black selection box drawn over a mirror the player looks at would sit on top of the reflection, and a
 * reflection has no business showing the box around any block. Both steps of drawing it are stopped, because Fabric API calls the inner one by itself.
 */
@Mixin(LevelRenderer.class)
public abstract class BlockOutlineMixin {
    //? if >=26.2 {
    /*@Inject(method={"submitBlockOutline","submitHitOutline"},at=@At("HEAD"),cancellable=true)
    *///?} else if >=1.21.2 {
    @Inject(method={"renderBlockOutline","renderHitOutline"},at=@At("HEAD"),cancellable=true)
    //?} else {
    /*@Inject(method="renderHitOutline",at=@At("HEAD"),cancellable=true)
    *///?}
    private void simplymirrorsplus$noOutlineOnMirror(CallbackInfo ci) {
        if(com.malidev.simplymirrorsplus.client.MirrorRenderer.redirect!=null) { ci.cancel();return; }
        Minecraft mc=Minecraft.getInstance();
        HitResult hit=mc.hitResult;
        if(mc.level!=null&&hit instanceof BlockHitResult&&mc.level.getBlockState(((BlockHitResult)hit).getBlockPos()).getBlock() instanceof MirrorBlock)ci.cancel();
    }
}

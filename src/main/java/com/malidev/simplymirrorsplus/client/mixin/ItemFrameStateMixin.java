//? if >=1.21.4 {
package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.block.MirrorBlock;
import com.malidev.simplymirrorsplus.client.FramedItemState;
import com.malidev.simplymirrorsplus.client.ItemFrames;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carries a framed mirror's frame from the stack to the moment its layers are drawn. */
@Mixin(ItemStackRenderState.class)
public abstract class ItemFrameStateMixin implements FramedItemState {
    @Unique private String simplymirrorsplus$frameId;
    @Unique private MirrorBlock.Form simplymirrorsplus$frameForm;

    @Override public void simplymirrorsplus$frame(String frame,MirrorBlock.Form form) { simplymirrorsplus$frameId=frame;simplymirrorsplus$frameForm=form; }

    //? if >=1.21.9 {
    /*@Inject(method="submit",at=@At("HEAD"))
    *///?} else {
    @Inject(method="render",at=@At("HEAD"))
    //?}
    private void simplymirrorsplus$offerFrame(CallbackInfo ci) { ItemFrames.frame=simplymirrorsplus$frameId;ItemFrames.form=simplymirrorsplus$frameForm; }

    //? if >=1.21.9 {
    /*@Inject(method="submit",at=@At("RETURN"))
    *///?} else {
    @Inject(method="render",at=@At("RETURN"))
    //?}
    private void simplymirrorsplus$withdrawFrame(CallbackInfo ci) { ItemFrames.frame=null; }
}
//?}

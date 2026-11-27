//? if >=1.21.4 {
package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.block.MirrorBlock;
import com.malidev.simplymirrorsplus.client.ItemFrames;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the frame to a framed mirror item, right after the first layer of its model and under the same pose. */
@Mixin(targets="net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public abstract class ItemFrameLayerMixin {
    //? if >=1.21.9 {
    /*@Inject(method="submit",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void simplymirrorsplus$frame(PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,int light,int overlay,int outline,CallbackInfo ci) {
        final String frame=ItemFrames.frame;
        if(frame==null)return;
        final MirrorBlock.Form form=ItemFrames.form;
        ItemFrames.frame=null;
        //? if >=26.1 {
        /^collector.submitCustomGeometry(pose,net.minecraft.client.renderer.Sheets.translucentBlockItemSheet(),(last,consumer)->ItemFrames.draw(last,consumer,frame,form,light,overlay));
        ^///?} else {
        collector.submitCustomGeometry(pose,net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),(last,consumer)->ItemFrames.draw(last,consumer,frame,form,light,overlay));
        //?}
    }
    *///?} else {
    @Inject(method="render",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void simplymirrorsplus$frame(PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay,CallbackInfo ci) {
        String frame=ItemFrames.frame;
        if(frame==null)return;
        ItemFrames.frame=null;
        ItemFrames.draw(pose.last(),buffers.getBuffer(net.minecraft.client.renderer.Sheets.cutoutBlockSheet()),frame,ItemFrames.form,light,overlay);
    }
    //?}
}
//?}

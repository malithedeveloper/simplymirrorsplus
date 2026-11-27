//? if <1.21.4 {
/*package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.ItemFrames;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^* Adds the frame to a framed mirror item, right after the item's own model and under the same pose. ^/
@Mixin(ItemRenderer.class)
public abstract class ItemFrameMixin {
    //? if >=1.21.2 {
    // In 1.21.2 and 1.21.3 the pose is set up one method further in.
    @Inject(method="renderItemModelRaw",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void simplymirrorsplus$frame(ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,boolean leftHand,com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay,BakedModel model,boolean special,float depth,CallbackInfo ci) {
        String frame=ItemFrames.frameOf(stack);
        if(frame!=null)ItemFrames.draw(pose.last(),buffers.getBuffer(net.minecraft.client.renderer.Sheets.cutoutBlockSheet()),frame,ItemFrames.formOf(stack),light,overlay);
    }
    //?} else if >=1.19.4 {
    /^@Inject(method="render",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void simplymirrorsplus$frame(ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,boolean leftHand,com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay,BakedModel model,CallbackInfo ci) {
        String frame=ItemFrames.frameOf(stack);
        if(frame!=null)ItemFrames.draw(pose.last(),buffers.getBuffer(net.minecraft.client.renderer.Sheets.cutoutBlockSheet()),frame,ItemFrames.formOf(stack),light,overlay);
    }
    ^///?} else if >=1.15 {
    /^@Inject(method="render",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void simplymirrorsplus$frame(ItemStack stack,net.minecraft.client.renderer.block.model.ItemTransforms.TransformType context,boolean leftHand,com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay,BakedModel model,CallbackInfo ci) {
        String frame=ItemFrames.frameOf(stack);
        if(frame!=null)ItemFrames.draw(pose.last(),buffers.getBuffer(net.minecraft.client.renderer.Sheets.cutoutBlockSheet()),frame,ItemFrames.formOf(stack),light,overlay);
    }
    ^///?} else {
    /^@Inject(method="render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/BakedModel;)V",at=@At("TAIL"))
    private void simplymirrorsplus$frame(ItemStack stack,BakedModel model,CallbackInfo ci) { ItemFrames.drawLegacy(stack); }
    ^///?}
}
*///?}

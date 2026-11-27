//? if >=1.21.4 {
package com.malidev.simplymirrorsplus.client.mixin;

import com.malidev.simplymirrorsplus.client.FramedItemState;
import com.malidev.simplymirrorsplus.client.ItemFrames;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** From 1.21.4 an item is drawn from a state that no longer knows its stack, so the frame is noted on the state here. */
@Mixin(ItemModelResolver.class)
public abstract class ItemFrameResolverMixin {
    @Inject(method="appendItemLayers",at=@At("HEAD"))
    //? if >=1.21.9 {
    /*private void simplymirrorsplus$noteFrame(ItemStackRenderState state,ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,net.minecraft.world.level.Level level,
            net.minecraft.world.entity.ItemOwner owner,int seed,CallbackInfo ci) {
    *///?} else {
    private void simplymirrorsplus$noteFrame(ItemStackRenderState state,ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,net.minecraft.world.level.Level level,
            net.minecraft.world.entity.LivingEntity owner,int seed,CallbackInfo ci) {
    //?}
        String frame=ItemFrames.frameOf(stack);
        ((FramedItemState)state).simplymirrorsplus$frame(frame,frame==null?null:ItemFrames.formOf(stack));
        //? if >=1.21.6 {
        /*// Pictures of items in the inventory are kept and reused; the frame makes this one a picture of its own.
        if(frame!=null)state.appendModelIdentityElement("simplymirrorsplus:frame/"+frame);
        *///?}
    }
}
//?}

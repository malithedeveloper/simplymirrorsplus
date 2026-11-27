//? if <1.15 {
/*package com.malidev.simplymirrorsplus.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface LegacyGameRendererAccessor {
    /^* Draws the world without the picking and light updates that renderLevel does once per frame. ^/
    @Invoker("render") void simplymirrorsplus$render(float partial,long deadline);
    @Invoker("setupCamera") void simplymirrorsplus$setupCamera(float partial);
    @Accessor("renderHand") boolean simplymirrorsplus$renderHand();
    @Accessor("renderHand") void simplymirrorsplus$renderHand(boolean render);
}
*///?}

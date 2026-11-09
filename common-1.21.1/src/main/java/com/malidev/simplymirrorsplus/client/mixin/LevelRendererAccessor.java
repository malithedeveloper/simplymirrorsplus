package com.malidev.simplymirrorsplus.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    @Accessor("transparencyChain") PostChain simplymirrorsplus$transparencyChain();
    @Accessor("transparencyChain") void simplymirrorsplus$setTransparencyChain(PostChain chain);
    @Invoker("renderEndSky") void simplymirrorsplus$renderEndSky(PoseStack pose);
}

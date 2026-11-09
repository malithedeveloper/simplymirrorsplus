package com.nstut.simplymirrors.client.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    @Accessor("skyRenderer") SkyRenderer simplymirrors$skyRenderer();
}

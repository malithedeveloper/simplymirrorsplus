//? if >=26.2 {
/*package com.malidev.simplymirrorsplus.client.mixin;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
/^* The sky renderer keeps the target it was built with; reflections need their sky in the mirror image. ^/
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @Redirect(method="*",at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/SkyRenderer;renderTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;",opcode=org.objectweb.asm.Opcodes.GETFIELD),require=1,allow=64)
    private RenderTarget simplymirrorsplus$target(SkyRenderer sky) {
        return MirrorRenderer.redirect!=null?MirrorRenderer.redirect:((SkyRendererAccessor)sky).simplymirrorsplus$renderTarget();
    }
}
*///?}

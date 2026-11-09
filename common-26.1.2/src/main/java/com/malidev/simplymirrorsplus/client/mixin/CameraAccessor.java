package com.malidev.simplymirrorsplus.client.mixin;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPosition") void simplymirrorsplus$position(Vec3 position);
    @Invoker("setRotation") void simplymirrorsplus$rotation(float yaw,float pitch);
    @Accessor("detached") void simplymirrorsplus$detached(boolean detached);
    @Invoker("prepareCullFrustum") void simplymirrorsplus$frustum(Matrix4fc view,Matrix4f projection,Vec3 position);
}

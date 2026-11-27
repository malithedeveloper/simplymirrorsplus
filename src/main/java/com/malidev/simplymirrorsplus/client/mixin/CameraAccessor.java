package com.malidev.simplymirrorsplus.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPosition") void simplymirrorsplus$position(Vec3 position);
    @Invoker("setRotation") void simplymirrorsplus$rotation(float yaw,float pitch);
    //? if <1.15
    //@org.spongepowered.asm.mixin.gen.Accessor("detached") void simplymirrorsplus$detached(boolean detached);
    //? if >=26.1 {
    /*@org.spongepowered.asm.mixin.gen.Accessor("detached") void simplymirrorsplus$detached(boolean detached);
    @Invoker("prepareCullFrustum") void simplymirrorsplus$frustum(org.joml.Matrix4fc view,org.joml.Matrix4f projection,Vec3 position);
    *///?}
}

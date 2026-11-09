package com.malidev.simplymirrorsplus.client.mixin;
import com.mojang.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Matrix4f.class)
public interface MatrixAccessor {
    @Accessor("m00") void simplymirrorsplus$m00(float value);
    @Accessor("m01") void simplymirrorsplus$m01(float value);
    @Accessor("m02") void simplymirrorsplus$m02(float value);
    @Accessor("m03") void simplymirrorsplus$m03(float value);
    @Accessor("m10") void simplymirrorsplus$m10(float value);
    @Accessor("m11") void simplymirrorsplus$m11(float value);
    @Accessor("m12") void simplymirrorsplus$m12(float value);
    @Accessor("m13") void simplymirrorsplus$m13(float value);
    @Accessor("m20") void simplymirrorsplus$m20(float value);
    @Accessor("m21") void simplymirrorsplus$m21(float value);
    @Accessor("m22") void simplymirrorsplus$m22(float value);
    @Accessor("m23") void simplymirrorsplus$m23(float value);
    @Accessor("m30") void simplymirrorsplus$m30(float value);
    @Accessor("m31") void simplymirrorsplus$m31(float value);
    @Accessor("m32") void simplymirrorsplus$m32(float value);
    @Accessor("m33") void simplymirrorsplus$m33(float value);
}

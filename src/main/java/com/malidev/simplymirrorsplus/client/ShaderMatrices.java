package com.malidev.simplymirrorsplus.client;

final class ShaderMatrices implements MatrixAccess {
    //? if >=1.19.3 {
    @Override public Object copy(Object value) { return value==null?null:new org.joml.Matrix4f((org.joml.Matrix4fc)value); }
    @Override public float[] array(Object value) { return value==null?null:((org.joml.Matrix4fc)value).get(new float[16]); }
    //?} else if <1.15 {
    /*@Override public Object copy(Object value) { return value==null?null:MirrorRenderer.matrix(MirrorRenderer.array((com.mojang.math.Matrix4f)value)); }
    @Override public float[] array(Object value) { return value==null?null:MirrorRenderer.array((com.mojang.math.Matrix4f)value); }
    *///?} else {
    /*@Override public Object copy(Object value) { return value==null?null:new com.mojang.math.Matrix4f((com.mojang.math.Matrix4f)value); }
    @Override public float[] array(Object value) { return value==null?null:MirrorRenderer.array((com.mojang.math.Matrix4f)value); }
    *///?}
}

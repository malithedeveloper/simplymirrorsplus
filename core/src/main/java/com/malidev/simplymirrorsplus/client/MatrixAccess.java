package com.malidev.simplymirrorsplus.client;

/** Reads the matrix class of the running Minecraft version: Mojang's own before 1.19.3, JOML since. */
public interface MatrixAccess {
    Object copy(Object matrix);
    float[] array(Object matrix);
}

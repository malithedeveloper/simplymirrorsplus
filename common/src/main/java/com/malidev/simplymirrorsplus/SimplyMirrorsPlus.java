package com.malidev.simplymirrorsplus;

/** Loader-neutral identity. Keep this class free of client-only and Minecraft classes. */
public final class SimplyMirrorsPlus {
    public static final String MOD_ID = "simplymirrorsplus";
    public static final String NAME = "Simply Mirrors Plus";

    private SimplyMirrorsPlus() { }

    public static void initialize() {
        java.util.logging.Logger.getLogger(MOD_ID).info(NAME + " loaded");
    }
}

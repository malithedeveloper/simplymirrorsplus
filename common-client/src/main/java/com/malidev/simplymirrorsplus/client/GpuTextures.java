package com.malidev.simplymirrorsplus.client;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** NeoForge's GPU validation layer wraps textures; the OpenGL texture is the wrapped one. */
public final class GpuTextures {
    private static final Method NONE;
    private static final Map<Class<?>,Method> UNWRAP=new ConcurrentHashMap<Class<?>,Method>();
    static {
        try { NONE=Object.class.getMethod("hashCode"); } catch(NoSuchMethodException exception) { throw new ExceptionInInitializerError(exception); }
    }

    private GpuTextures() { }

    public static Object unwrap(Object texture) {
        while(texture!=null) {
            Method method=UNWRAP.get(texture.getClass());
            if(method==null) {
                try { method=texture.getClass().getMethod("getRealTexture"); } catch(NoSuchMethodException exception) { method=NONE; }
                UNWRAP.put(texture.getClass(),method);
            }
            if(method==NONE)return texture;
            try { texture=method.invoke(texture); } catch(ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
        }
        return null;
    }
}

//? if >=1.15 <1.20.2 {
/*package com.malidev.simplymirrorsplus.client;

import java.lang.reflect.Constructor;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.Direction;

/^*
 * Fills the list of terrain chunks LevelRenderer draws with every loaded chunk inside a reflection's view.
 * Before 1.20.2 that list holds a private record per chunk, so the records are made through reflection and kept.
 ^/
public final class ReflectionSections {
    private static final Map<Object,Object> INFOS=new IdentityHashMap<Object,Object>();
    private static final int MAX_BUILDS=8;
    private static Constructor<?> constructor;
    private static Object area;

    private ReflectionSections() {}

    @SuppressWarnings({"unchecked","rawtypes"})
    //? if >=1.18 {
    public static void fill(LevelRenderer renderer,ViewArea viewArea,Frustum frustum,List into) {
    //?} else {
    /^public static void fill(LevelRenderer renderer,ViewArea viewArea,Frustum frustum,List into,ChunkRenderDispatcher dispatcher) {
    ^///?}
        if(area!=viewArea) { INFOS.clear();area=viewArea; }
        into.clear();
        int builds=0;
        for(ChunkRenderDispatcher.RenderChunk chunk:viewArea.chunks) {
            //? if >=1.18 {
            if(!frustum.isVisible(chunk.getBoundingBox()))continue;
            //?} else {
            /^if(!frustum.isVisible(chunk.bb))continue;
            // These versions queue terrain builds inside the method this replaces.
            // Starting a build copies the chunk's blocks on this thread, so a view full of new terrain is spread over frames.
            if(chunk.isDirty()&&chunk.hasAllNeighbors()&&builds++<MAX_BUILDS) { chunk.rebuildChunkAsync(dispatcher);chunk.setNotDirty(); }
            ^///?}
            Object info=INFOS.get(chunk);
            if(info==null) { info=create(renderer,chunk);INFOS.put(chunk,info); }
            into.add(info);
        }
    }

    public static void clear() { INFOS.clear();area=null; }

    private static Object create(LevelRenderer renderer,ChunkRenderDispatcher.RenderChunk chunk) {
        try {
            if(constructor==null)constructor=find();
            return constructor.getParameterCount()==4?constructor.newInstance(renderer,chunk,null,0):constructor.newInstance(chunk,null,0);
        } catch(ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot list terrain for a reflection",exception);
        }
    }

    /^* The record class is private and its name differs between mappings, so it is found by its constructor. ^/
    private static Constructor<?> find() throws NoSuchMethodException {
        for(Class<?> inner:LevelRenderer.class.getDeclaredClasses())for(Constructor<?> candidate:inner.getDeclaredConstructors()) {
            Class<?>[] types=candidate.getParameterTypes();
            int n=types.length;
            if((n==3||n==4&&types[0]==LevelRenderer.class)&&types[n-3]==ChunkRenderDispatcher.RenderChunk.class&&types[n-2]==Direction.class&&types[n-1]==int.class) {
                candidate.setAccessible(true);
                return candidate;
            }
        }
        throw new NoSuchMethodException("LevelRenderer chunk record");
    }
}
*///?}

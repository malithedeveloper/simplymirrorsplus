/*!stonecutter-disabled
package com.malidev.simplymirrorsplus.client;

import com.malidev.simplymirrorsplus.block.MirrorBlock;
import com.malidev.simplymirrorsplus.math.MirrorBits;
import com.malidev.simplymirrorsplus.math.MirrorSurface;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/^*
 * Optional link to Chisels & Bits: mirror bits in a chiselled block reflect. The other mod is reached by name
 * at run time, so nothing here needs it to be installed, and without it this class does nothing.
 ^/
final class ChiselledMirrors {
    private static final String API="mod.chiselsandbits.api.";
    /^* How many searches a block's surfaces are kept for when its bit counts have not changed. ^/
    private static final int KEEP=5;
    private static final Map<BlockEntity,Kept> KNOWN=new WeakHashMap<BlockEntity,Kept>();
    private static Class<?> chiselled;
    private static Method statistics,counts,stream,information,start,end,state;
    private static boolean absent;
    private static int search;

    private static final class Kept {
        int counts,search;
        List<MirrorSurface> surfaces;
    }

    private ChiselledMirrors() { }

    /^* Call once per search of the surroundings, before asking about its block entities. ^/
    static void nextSearch() { search++; }

    /^* The reflecting surfaces of this block entity, or null when it is not a chiselled block with mirror bits. ^/
    static List<MirrorSurface> surfaces(BlockEntity entity) {
        if(absent)return null;
        try {
            if(chiselled==null)link();
            if(!chiselled.isInstance(entity))return null;
            Map<?,?> bits=(Map<?,?>)counts.invoke(statistics.invoke(entity));
            boolean mirror=false;
            for(Object kind:bits.keySet())mirror|=state(kind).getBlock() instanceof MirrorBlock;
            if(!mirror)return null;
            Kept kept=KNOWN.get(entity);
            if(kept==null) { kept=new Kept();KNOWN.put(entity,kept); }
            // The counts change with nearly every cut. Bits that only moved are caught by the periodic rebuild.
            int now=bits.hashCode();
            if(kept.surfaces==null||kept.counts!=now||search-kept.search>=KEEP) {
                kept.surfaces=build(entity);
                kept.counts=now;
                kept.search=search;
            }
            return kept.surfaces;
        } catch(ReflectiveOperationException|LinkageError|RuntimeException changed) {
            // No Chisels & Bits, or a version whose API this does not know: mirrors work as without it.
            absent=true;
            return null;
        }
    }

    private static void link() throws ReflectiveOperationException {
        ClassLoader loader=ChiselledMirrors.class.getClassLoader();
        chiselled=Class.forName(API+"block.entity.IMultiStateBlockEntity",false,loader);
        Class<?> entry=Class.forName(API+"multistate.accessor.IStateEntryInfo",false,loader);
        statistics=chiselled.getMethod("getStatistics");
        counts=Class.forName(API+"multistate.statistics.IMultiStateObjectStatistics",false,loader).getMethod("getStateCounts");
        stream=chiselled.getMethod("stream");
        // The first versions for 1.16 and 1.17 hand out the block state itself, later ones wrap it.
        // ponytail: that older form is written from the mod's history and has not been run against it.
        try { information=entry.getMethod("getBlockInformation"); }
        catch(NoSuchMethodException first) { state=entry.getMethod("getState");information=null; }
        start=entry.getMethod("getStartPoint");
        end=entry.getMethod("getEndPoint");
        // A bit's block state: a record component in newer versions of the mod, a getter in older ones.
        if(information==null)return;
        try { state=information.getReturnType().getMethod("blockState"); }
        catch(NoSuchMethodException older) { state=information.getReturnType().getMethod("getBlockState"); }
    }

    /^* The block state of one kind of bit, as listed in a block's counts. ^/
    private static BlockState state(Object kind) throws ReflectiveOperationException {
        return kind instanceof BlockState?(BlockState)kind:(BlockState)state.invoke(kind);
    }

    private static List<MirrorSurface> build(BlockEntity entity) throws ReflectiveOperationException {
        int size=MirrorBits.SIZE;
        boolean[] mirror=new boolean[size*size*size],solid=new boolean[size*size*size];
        try(Stream<?> entries=(Stream<?>)stream.invoke(entity)) {
            for(Iterator<?> each=entries.iterator();each.hasNext();) {
                Object entry=each.next();
                BlockState bit=information==null?(BlockState)state.invoke(entry):state(information.invoke(entry));
                if(bit.isAir())continue;
                Vec3 from=(Vec3)start.invoke(entry),to=(Vec3)end.invoke(entry);
                // The points may be given inside the block or in the world; the part after the block's corner is the same.
                int x0=cell(from.x),y0=cell(from.y),z0=cell(from.z);
                int x1=Math.min(size,x0+span(from.x,to.x)),y1=Math.min(size,y0+span(from.y,to.y)),z1=Math.min(size,z0+span(from.z,to.z));
                boolean reflects=bit.getBlock() instanceof MirrorBlock;
                for(int x=x0;x<x1;x++)for(int y=y0;y<y1;y++)for(int z=z0;z<z1;z++) {
                    solid[MirrorBits.index(x,y,z)]=true;
                    if(reflects)mirror[MirrorBits.index(x,y,z)]=true;
                }
            }
        }
        BlockPos pos=entity.getBlockPos();
        // Bits lie at any depth; flattened, the faces of one direction share a reflection instead of needing one each.
        List<MirrorSurface> flat=new java.util.ArrayList<MirrorSurface>();
        for(MirrorSurface surface:MirrorBits.surfaces(pos.getX(),pos.getY(),pos.getZ(),mirror,solid))flat.add(surface.flattened());
        return flat;
    }

    private static int cell(double point) { return Math.min(MirrorBits.SIZE-1,(int)Math.round((point-Math.floor(point+1e-6))*MirrorBits.SIZE)); }

    private static int span(double from,double to) { return Math.max(1,(int)Math.round((to-from)*MirrorBits.SIZE)); }
}
*/
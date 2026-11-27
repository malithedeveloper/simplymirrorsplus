package com.malidev.simplymirrorsplus.block;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class MirrorContent {
    public static Supplier<BlockEntityType<MirrorBlockEntity>> blockEntityType;
    public static Supplier<net.minecraft.world.item.crafting.RecipeSerializer<FrameRecipe>> frameRecipe;
    private MirrorContent() { }

    /** The block with this id, or null when there is none, for example after the mod that added it was removed. */
    public static net.minecraft.world.level.block.Block block(String id) {
        net.minecraft.resources.ResourceLocation key = net.minecraft.resources.ResourceLocation.tryParse(id);
        if (key == null) return null;
        //? if >=1.21.2 {
        net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(key);
        //?} else if >=1.19.3 {
        /*net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(key);
        *///?} else {
        /*net.minecraft.world.level.block.Block block = net.minecraft.core.Registry.BLOCK.get(key);
        *///?}
        // The registry answers an unknown id with air.
        return block == net.minecraft.world.level.block.Blocks.AIR ? null : block;
    }

    /** The item of a form of the mirror. */
    public static net.minecraft.world.item.Item item(MirrorBlock.Form form) {
        net.minecraft.resources.ResourceLocation key = net.minecraft.resources.ResourceLocation.tryParse("simplymirrorsplus:" + form.item());
        //? if >=1.21.2 {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key);
        //?} else if >=1.19.3 {
        /*return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key);
        *///?} else {
        /*return net.minecraft.core.Registry.ITEM.get(key);
        *///?}
    }

    /**
     * Chisel Reborn keeps its chisel groups in code and reads none from data, so when it is installed the forms
     * are added to it as one group through the method it adds its own blocks with.
     * ponytail: found by name through reflection; a changed method only costs the group.
     */
    public static void joinChiselReborn() {
        try {
            Class<?> lookup = Class.forName("com.periut.chisel.block.ChiselGroupLookup");
            for (java.lang.reflect.Method method : lookup.getMethods()) {
                // The method comes in two forms; this is the one that takes a single id, not a list of them.
                if (!method.getName().equals("addItemToGroup") || method.getParameterCount() != 2 || java.util.List.class.isAssignableFrom(method.getParameterTypes()[1])) continue;
                for (MirrorBlock.Form form : MirrorBlock.Form.items())
                    method.invoke(null, "simplymirrorsplus_mirror", net.minecraft.resources.ResourceLocation.tryParse("simplymirrorsplus:" + form.item()));
                return;
            }
        } catch (ClassNotFoundException absent) {
            // Chisel Reborn is not installed.
        } catch (ReflectiveOperationException | RuntimeException | LinkageError changed) {
            java.util.logging.Logger.getLogger("simplymirrorsplus").log(java.util.logging.Level.WARNING, "Could not add the mirrors to Chisel Reborn", changed);
        }
    }

    public static String id(net.minecraft.world.level.block.Block block) {
        //? if >=1.19.3 {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
        //?} else {
        /*return net.minecraft.core.Registry.BLOCK.getKey(block).toString();
        *///?}
    }
}

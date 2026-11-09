package com.malidev.simplymirrorsplus.fabric;
import com.malidev.simplymirrorsplus.SimplyMirrorsPlus;
import com.malidev.simplymirrorsplus.block.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;

public final class SimplyMirrorsPlusFabric implements ModInitializer {
    public static final Block MIRROR = new MirrorBlock(BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.METAL).strength(0.8F).sound(SoundType.METAL));
    public static final CreativeModeTab TAB = FabricItemGroupBuilder.create(id("main")).icon(() -> new ItemStack(SimplyMirrorsPlusFabric.MIRROR_ITEM)).build();
    public static final Item MIRROR_ITEM = new BlockItem(MIRROR, new Item.Properties().tab(TAB));
    @Override public void onInitialize() {
        Registry.register(Registry.BLOCK, id("mirror"), MIRROR);
        Registry.register(Registry.ITEM, id("mirror"), MIRROR_ITEM);
        BlockEntityType<MirrorBlockEntity> type = Registry.register(Registry.BLOCK_ENTITY_TYPE, id("mirror"), BlockEntityType.Builder.of(MirrorBlockEntity::new, MIRROR).build(null));
        MirrorContent.blockEntityType = () -> type;
        
        SimplyMirrorsPlus.initialize();
    }
    private static ResourceLocation id(String path) { return new ResourceLocation(SimplyMirrorsPlus.MOD_ID, path); }
}

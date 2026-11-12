package com.malidev.simplymirrorsplus.forge;
import com.malidev.simplymirrorsplus.SimplyMirrorsPlus;
import com.malidev.simplymirrorsplus.block.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.network.chat.TranslatableComponent;

@Mod(SimplyMirrorsPlus.MOD_ID)
public final class SimplyMirrorsPlusForge {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SimplyMirrorsPlus.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SimplyMirrorsPlus.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, SimplyMirrorsPlus.MOD_ID);
    public static final RegistryObject<Block> MIRROR = BLOCKS.register("mirror", () -> new MirrorBlock(BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.METAL).strength(0.8F).sound(SoundType.METAL)));
    public static final CreativeModeTab TAB = new CreativeModeTab("simplymirrorsplus") { @Override public ItemStack makeIcon() { return new ItemStack(SimplyMirrorsPlusForge.MIRROR_ITEM.get()); } };
    public static final RegistryObject<BlockItem> MIRROR_ITEM = ITEMS.register("mirror", () -> new BlockItem(MIRROR.get(), new Item.Properties().tab(TAB)));
    public static final RegistryObject<BlockEntityType<MirrorBlockEntity>> MIRROR_ENTITY = ENTITIES.register("mirror", () -> BlockEntityType.Builder.of(MirrorBlockEntity::new, MIRROR.get()).build(null));
    public SimplyMirrorsPlusForge() {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) SimplyMirrorsPlusForgeClient.register();
        net.minecraftforge.eventbus.api.IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MirrorContent.blockEntityType = () -> MIRROR_ENTITY.get();
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        SimplyMirrorsPlus.initialize();
    }
}

package com.malidev.simplymirrorsplus.neoforge;
import com.malidev.simplymirrorsplus.SimplyMirrorsPlus;
import com.malidev.simplymirrorsplus.block.*;
import net.neoforged.fml.common.Mod;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;

@Mod(SimplyMirrorsPlus.MOD_ID)
public final class SimplyMirrorsPlusNeoforge {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, SimplyMirrorsPlus.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, SimplyMirrorsPlus.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SimplyMirrorsPlus.MOD_ID);
    public static final DeferredHolder<Block, Block> MIRROR = BLOCKS.register("mirror", () -> new MirrorBlock(BlockBehaviour.Properties.of().setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(SimplyMirrorsPlus.MOD_ID, "mirror"))).strength(0.8F).sound(SoundType.METAL)));
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimplyMirrorsPlus.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
        .title(Component.translatable("itemGroup.simplymirrorsplus")).icon(() -> new ItemStack(SimplyMirrorsPlusNeoforge.MIRROR_ITEM.get()))
        .displayItems((parameters, output) -> output.accept(SimplyMirrorsPlusNeoforge.MIRROR_ITEM.get())).build());
    public static final DeferredHolder<Item, BlockItem> MIRROR_ITEM = ITEMS.register("mirror", () -> new BlockItem(MIRROR.get(), new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(SimplyMirrorsPlus.MOD_ID, "mirror"))).useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MirrorBlockEntity>> MIRROR_ENTITY = ENTITIES.register("mirror", () -> new BlockEntityType<>(MirrorBlockEntity::new, MIRROR.get()));
    public SimplyMirrorsPlusNeoforge(net.neoforged.bus.api.IEventBus bus, net.neoforged.fml.ModContainer container, net.neoforged.api.distmarker.Dist dist) {
        if (dist.isClient()) SimplyMirrorsPlusNeoforgeClient.register(container);
        MirrorContent.blockEntityType = () -> MIRROR_ENTITY.get();
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TABS.register(bus);
        SimplyMirrorsPlus.initialize();
    }
}

/*!stonecutter-disabled
package com.malidev.simplymirrorsplus.neoforge;

import com.malidev.simplymirrorsplus.SimplyMirrorsPlus;
import com.malidev.simplymirrorsplus.block.MirrorBlock;
import com.malidev.simplymirrorsplus.block.MirrorBlockEntity;
import com.malidev.simplymirrorsplus.block.MirrorContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
//? if >=1.21.2 {
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
//?}

@Mod(SimplyMirrorsPlus.MOD_ID)
public final class SimplyMirrorsPlusNeoforge {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,SimplyMirrorsPlus.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,SimplyMirrorsPlus.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES=DeferredRegister.create(Registries.RECIPE_SERIALIZER,SimplyMirrorsPlus.MOD_ID);
    private static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>,net.minecraft.world.item.crafting.RecipeSerializer<com.malidev.simplymirrorsplus.block.FrameRecipe>> FRAME_RECIPE=RECIPES.register("frame",com.malidev.simplymirrorsplus.block.FrameRecipe::serializer);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,SimplyMirrorsPlus.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,SimplyMirrorsPlus.MOD_ID);

    //? if >=1.21.2 {
    public static final DeferredHolder<Block,Block> MIRROR=BLOCKS.register("mirror",()->new MirrorBlock(BlockBehaviour.Properties.of()
        .setId(ResourceKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath(SimplyMirrorsPlus.MOD_ID,"mirror"))).strength(0.8F).sound(SoundType.GLASS)));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<MirrorBlockEntity>> MIRROR_ENTITY=ENTITIES.register("mirror",()->new BlockEntityType<>(MirrorBlockEntity::new,MIRROR.get()));
    //?} else {
    /^public static final DeferredHolder<Block,Block> MIRROR=BLOCKS.register("mirror",()->new MirrorBlock(BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.GLASS)));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<MirrorBlockEntity>> MIRROR_ENTITY=ENTITIES.register("mirror",()->BlockEntityType.Builder.of(MirrorBlockEntity::new,MIRROR.get()).build(null));
    ^///?}
    /^* One item per form of the mirror. The full block comes last, so it is the item the block itself stands for. ^/
    public static final java.util.Map<MirrorBlock.Form,DeferredHolder<Item,BlockItem>> FORM_ITEMS=items();
    public static final DeferredHolder<Item,BlockItem> MIRROR_ITEM=FORM_ITEMS.get(MirrorBlock.Form.FULL);

    private static java.util.Map<MirrorBlock.Form,DeferredHolder<Item,BlockItem>> items() {
        java.util.Map<MirrorBlock.Form,DeferredHolder<Item,BlockItem>> items=new java.util.EnumMap<MirrorBlock.Form,DeferredHolder<Item,BlockItem>>(MirrorBlock.Form.class);
        for(int i=1;i<=MirrorBlock.Form.items().length;i++) {
            MirrorBlock.Form form=MirrorBlock.Form.items()[i%MirrorBlock.Form.items().length];
            //? if >=1.21.2 {
            items.put(form,ITEMS.register(form.item(),()->new com.malidev.simplymirrorsplus.block.MirrorItem(MIRROR.get(),form,new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath(SimplyMirrorsPlus.MOD_ID,form.item()))).useBlockDescriptionPrefix())));
            //?} else {
            /^items.put(form,ITEMS.register(form.item(),()->new com.malidev.simplymirrorsplus.block.MirrorItem(MIRROR.get(),form,new Item.Properties())));
            ^///?}
        }
        return items;
    }

    //? if >=1.21.2 {
    public static final DeferredHolder<Item,Item> WRENCH=ITEMS.register("mirror_wrench",()->new com.malidev.simplymirrorsplus.block.MirrorWrenchItem(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath(SimplyMirrorsPlus.MOD_ID,"mirror_wrench"))).stacksTo(1)));
    //?} else {
    /^public static final DeferredHolder<Item,Item> WRENCH=ITEMS.register("mirror_wrench",()->new com.malidev.simplymirrorsplus.block.MirrorWrenchItem(new Item.Properties().stacksTo(1)));
    ^///?}

    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder(CreativeModeTab.Row.TOP,0)
        .title(Component.translatable("itemGroup.simplymirrorsplus")).icon(()->new ItemStack(MIRROR_ITEM.get()))
        .displayItems((parameters,output)->{ for(MirrorBlock.Form form:MirrorBlock.Form.items())output.accept(FORM_ITEMS.get(form).get());output.accept(WRENCH.get()); }).build());

    public SimplyMirrorsPlusNeoforge(IEventBus bus,ModContainer container,Dist dist) {
        if(dist.isClient())SimplyMirrorsPlusNeoforgeClient.register(container);
        MirrorContent.blockEntityType=()->MIRROR_ENTITY.get();
        BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);TABS.register(bus);RECIPES.register(bus);
        MirrorContent.frameRecipe=()->FRAME_RECIPE.get();
        SimplyMirrorsPlus.initialize();
        MirrorContent.joinChiselReborn();
    }
}
*/
package com.malidev.simplymirrorsplus.fabric;

import com.malidev.simplymirrorsplus.SimplyMirrorsPlus;
import com.malidev.simplymirrorsplus.block.MirrorBlock;
import com.malidev.simplymirrorsplus.block.MirrorBlockEntity;
import com.malidev.simplymirrorsplus.block.MirrorContent;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
//? if >=1.19.3 {
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
//?}
//? if >=1.21.2 {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//?}

public final class SimplyMirrorsPlusFabric implements ModInitializer {
    //? if >=1.21.2 {
    public static final Block MIRROR=new MirrorBlock(Block.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id("mirror"))).strength(0.8F).sound(SoundType.GLASS));
    //?} else if >=1.20 {
    /*public static final Block MIRROR=new MirrorBlock(Block.Properties.of().strength(0.8F).sound(SoundType.GLASS));
    *///?} else if >=1.19.3 {
    /*public static final Block MIRROR=new MirrorBlock(Block.Properties.of(net.minecraft.world.level.material.Material.METAL).strength(0.8F).sound(SoundType.GLASS));
    *///?} else {
    /*//? if >=1.16 {
    public static final Block MIRROR=new MirrorBlock(Block.Properties.of(net.minecraft.world.level.material.Material.METAL).strength(0.8F).sound(SoundType.GLASS));
    //?} else {
    /^// Before 1.16 the vanilla setters are protected.
    public static final Block MIRROR=new MirrorBlock(net.fabricmc.fabric.api.block.FabricBlockSettings.of(net.minecraft.world.level.material.Material.METAL).strength(0.8F,0.8F).sounds(SoundType.GLASS).build());
    ^///?}
    public static final CreativeModeTab TAB=net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder.create(id("main")).icon(()->new ItemStack(SimplyMirrorsPlusFabric.MIRROR_ITEM)).build();
    *///?}

    /** One item per form of the mirror. The full block comes last, so it is the item the block itself stands for. */
    public static final java.util.Map<MirrorBlock.Form,Item> ITEMS=items();
    public static final Item MIRROR_ITEM=ITEMS.get(MirrorBlock.Form.FULL);

    private static java.util.Map<MirrorBlock.Form,Item> items() {
        java.util.Map<MirrorBlock.Form,Item> items=new java.util.LinkedHashMap<MirrorBlock.Form,Item>();
        for(int i=1;i<=MirrorBlock.Form.items().length;i++) {
            MirrorBlock.Form form=MirrorBlock.Form.items()[i%MirrorBlock.Form.items().length];
            //? if >=1.21.2 {
            Item.Properties properties=new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id(form.item()))).useBlockDescriptionPrefix();
            //?} else if >=1.19.3 {
            /*Item.Properties properties=new Item.Properties();
            *///?} else {
            /*Item.Properties properties=new Item.Properties().tab(TAB);
            *///?}
            items.put(form,new com.malidev.simplymirrorsplus.block.MirrorItem(MIRROR,form,properties));
        }
        return items;
    }

    //? if >=1.21.2 {
    public static final Item WRENCH=new com.malidev.simplymirrorsplus.block.MirrorWrenchItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id("mirror_wrench"))).stacksTo(1));
    //?} else if >=1.19.3 {
    /*public static final Item WRENCH=new com.malidev.simplymirrorsplus.block.MirrorWrenchItem(new Item.Properties().stacksTo(1));
    *///?} else {
    /*public static final Item WRENCH=new com.malidev.simplymirrorsplus.block.MirrorWrenchItem(new Item.Properties().stacksTo(1).tab(TAB));
    *///?}

    /** Fills the creative tab: the full block first, then the forms in their order, then the wrench. */
    private static void show(java.util.function.Consumer<Item> output) {
        for(MirrorBlock.Form form:MirrorBlock.Form.items())output.accept(ITEMS.get(form));
        output.accept(WRENCH);
    }

    @Override public void onInitialize() {
        //? if >=1.19.3 {
        Registry.register(BuiltInRegistries.BLOCK,id("mirror"),MIRROR);
        for(java.util.Map.Entry<MirrorBlock.Form,Item> item:ITEMS.entrySet())Registry.register(BuiltInRegistries.ITEM,id(item.getKey().item()),item.getValue());
        Registry.register(BuiltInRegistries.ITEM,id("mirror_wrench"),WRENCH);
        Registry<BlockEntityType<?>> blockEntities=BuiltInRegistries.BLOCK_ENTITY_TYPE;
        //?} else {
        /*Registry.register(Registry.BLOCK,id("mirror"),MIRROR);
        for(java.util.Map.Entry<MirrorBlock.Form,Item> item:ITEMS.entrySet())Registry.register(Registry.ITEM,id(item.getKey().item()),item.getValue());
        Registry.register(Registry.ITEM,id("mirror_wrench"),WRENCH);
        Registry<BlockEntityType<?>> blockEntities=Registry.BLOCK_ENTITY_TYPE;
        *///?}
        // From 1.17 the vanilla builder's factory type is private; Fabric API has its own.
        //? if >=1.17 {
        BlockEntityType<MirrorBlockEntity> type=net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(MirrorBlockEntity::new,MIRROR).build();
        //?} else {
        /*BlockEntityType<MirrorBlockEntity> type=BlockEntityType.Builder.of(MirrorBlockEntity::new,MIRROR).build(null);
        *///?}
        Registry.register(blockEntities,id("mirror"),type);
        net.minecraft.world.item.crafting.RecipeSerializer<com.malidev.simplymirrorsplus.block.FrameRecipe> frameRecipe=com.malidev.simplymirrorsplus.block.FrameRecipe.serializer();
        //? if >=1.19.3 {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,id("frame"),frameRecipe);
        //?} else {
        /*Registry.register(Registry.RECIPE_SERIALIZER,id("frame"),frameRecipe);
        *///?}
        MirrorContent.frameRecipe=()->frameRecipe;
        MirrorContent.blockEntityType=()->type;
        //? if >=26.1 {
        /*Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,id("main"),net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.simplymirrorsplus")).icon(()->new ItemStack(MIRROR_ITEM))
            .displayItems((parameters,output)->show(output::accept)).build());
        *///?} else if >=1.20 {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,id("main"),net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.simplymirrorsplus")).icon(()->new ItemStack(MIRROR_ITEM))
            .displayItems((parameters,output)->show(output::accept)).build());
        //?} else if >=1.19.4 {
        /*net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder(id("main"))
            .title(Component.translatable("itemGroup.simplymirrorsplus")).icon(()->new ItemStack(MIRROR_ITEM))
            .displayItems((parameters,output)->show(output::accept)).build();
        *///?} else if >=1.19.3 {
        /*net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder(id("main"))
            .title(Component.translatable("itemGroup.simplymirrorsplus")).icon(()->new ItemStack(MIRROR_ITEM))
            .displayItems((features,output,operator)->show(output::accept)).build();
        *///?}
        SimplyMirrorsPlus.initialize();
        MirrorContent.joinChiselReborn();
    }

    private static ResourceLocation id(String path) {
        //? if >=1.21 {
        return ResourceLocation.fromNamespaceAndPath(SimplyMirrorsPlus.MOD_ID,path);
        //?} else {
        /*return new ResourceLocation(SimplyMirrorsPlus.MOD_ID,path);
        *///?}
    }
}

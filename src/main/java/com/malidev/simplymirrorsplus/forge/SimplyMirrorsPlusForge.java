/*!stonecutter-disabled
package com.malidev.simplymirrorsplus.forge;

import com.malidev.simplymirrorsplus.SimplyMirrorsPlus;
import com.malidev.simplymirrorsplus.block.MirrorBlock;
import com.malidev.simplymirrorsplus.block.MirrorBlockEntity;
import com.malidev.simplymirrorsplus.block.MirrorContent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
//? if >=1.18 {
import net.minecraftforge.registries.RegistryObject;
//?} else if >=1.17 {
/^import net.minecraftforge.fmllegacy.RegistryObject;
^///?} else {
/^import net.minecraftforge.fml.RegistryObject;
^///?}

@Mod(SimplyMirrorsPlus.MOD_ID)
public final class SimplyMirrorsPlusForge {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,SimplyMirrorsPlus.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,SimplyMirrorsPlus.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,SimplyMirrorsPlus.MOD_ID);
    private static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<com.malidev.simplymirrorsplus.block.FrameRecipe>> FRAME_RECIPE=RECIPES.register("frame",com.malidev.simplymirrorsplus.block.FrameRecipe::serializer);
    //? if >=1.19 {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,SimplyMirrorsPlus.MOD_ID);
    //?} else if >=1.17 {
    /^private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES,SimplyMirrorsPlus.MOD_ID);
    ^///?} else {
    /^private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.TILE_ENTITIES,SimplyMirrorsPlus.MOD_ID);
    ^///?}

    //? if >=1.20 {
    public static final RegistryObject<Block> MIRROR=BLOCKS.register("mirror",()->new MirrorBlock(Block.Properties.of().strength(0.8F).sound(SoundType.GLASS)));
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,SimplyMirrorsPlus.MOD_ID);
    public static final RegistryObject<CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder(CreativeModeTab.Row.TOP,0)
        .title(net.minecraft.network.chat.Component.translatable("itemGroup.simplymirrorsplus")).icon(()->new ItemStack(SimplyMirrorsPlusForge.MIRROR_ITEM.get()))
        .displayItems((parameters,output)->show(output::accept)).build());
    //?} else if >=1.19.3 {
    /^public static final RegistryObject<Block> MIRROR=BLOCKS.register("mirror",()->new MirrorBlock(Block.Properties.of(net.minecraft.world.level.material.Material.METAL).strength(0.8F).sound(SoundType.GLASS)));

    /^* Between 1.19.3 and 1.20 creative tabs are made in an event. ^/
    private static void registerTab(net.minecraftforge.event.CreativeModeTabEvent.Register event) {
        event.registerCreativeModeTab(new net.minecraft.resources.ResourceLocation(SimplyMirrorsPlus.MOD_ID,"main"),builder->builder
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.simplymirrorsplus")).icon(()->new ItemStack(MIRROR_ITEM.get()))
            //? if >=1.19.4 {
            .displayItems((parameters,output)->show(output::accept)));
            //?} else {
            /^.displayItems((features,output,operator)->show(output::accept)));
            ^///?}
    }
    ^///?} else {
    /^public static final RegistryObject<Block> MIRROR=BLOCKS.register("mirror",()->new MirrorBlock(Block.Properties.of(net.minecraft.world.level.material.Material.METAL).strength(0.8F).sound(SoundType.GLASS)));
    public static final CreativeModeTab TAB=new CreativeModeTab("simplymirrorsplus") { @Override public ItemStack makeIcon() { return new ItemStack(SimplyMirrorsPlusForge.MIRROR_ITEM.get()); } };
    ^///?}
    /^* One item per form of the mirror. The full block comes last, so it is the item the block itself stands for. ^/
    public static final java.util.Map<MirrorBlock.Form,RegistryObject<BlockItem>> FORM_ITEMS=items();
    public static final RegistryObject<BlockItem> MIRROR_ITEM=FORM_ITEMS.get(MirrorBlock.Form.FULL);

    private static java.util.Map<MirrorBlock.Form,RegistryObject<BlockItem>> items() {
        java.util.Map<MirrorBlock.Form,RegistryObject<BlockItem>> items=new java.util.EnumMap<MirrorBlock.Form,RegistryObject<BlockItem>>(MirrorBlock.Form.class);
        for(int i=1;i<=MirrorBlock.Form.items().length;i++) {
            MirrorBlock.Form form=MirrorBlock.Form.items()[i%MirrorBlock.Form.items().length];
            //? if >=1.19.3 {
            items.put(form,ITEMS.register(form.item(),()->new com.malidev.simplymirrorsplus.block.MirrorItem(MIRROR.get(),form,new Item.Properties())));
            //?} else {
            /^items.put(form,ITEMS.register(form.item(),()->new com.malidev.simplymirrorsplus.block.MirrorItem(MIRROR.get(),form,new Item.Properties().tab(TAB))));
            ^///?}
        }
        return items;
    }

    //? if >=1.19.3 {
    public static final RegistryObject<Item> WRENCH=ITEMS.register("mirror_wrench",()->new com.malidev.simplymirrorsplus.block.MirrorWrenchItem(new Item.Properties().stacksTo(1)));

    /^* Fills the creative tab: the full block first, then the forms in their order, then the wrench. ^/
    private static void show(java.util.function.Consumer<Item> output) {
        for(MirrorBlock.Form form:MirrorBlock.Form.items())output.accept(FORM_ITEMS.get(form).get());
        output.accept(WRENCH.get());
    }
    //?} else {
    /^public static final RegistryObject<Item> WRENCH=ITEMS.register("mirror_wrench",()->new com.malidev.simplymirrorsplus.block.MirrorWrenchItem(new Item.Properties().stacksTo(1).tab(TAB)));
    ^///?}

    public static final RegistryObject<BlockEntityType<MirrorBlockEntity>> MIRROR_ENTITY=ENTITIES.register("mirror",()->BlockEntityType.Builder.of(MirrorBlockEntity::new,MIRROR.get()).build(null));

    public SimplyMirrorsPlusForge() {
        if(FMLEnvironment.dist.isClient())SimplyMirrorsPlusForgeClient.register();
        IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus();
        MirrorContent.blockEntityType=()->MIRROR_ENTITY.get();
        BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);RECIPES.register(bus);
        MirrorContent.frameRecipe=()->FRAME_RECIPE.get();
        //? if >=1.20
        TABS.register(bus);
        //? if >=1.19.3 <1.20
        //bus.addListener(SimplyMirrorsPlusForge::registerTab);
        SimplyMirrorsPlus.initialize();
        MirrorContent.joinChiselReborn();
    }
}
*/
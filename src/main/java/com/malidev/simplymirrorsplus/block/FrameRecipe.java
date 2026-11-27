package com.malidev.simplymirrorsplus.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A mirror and any one block, anywhere in the crafting grid, give the mirror framed with that block. It works
 * for blocks of other mods too, because the frame is stored as the block's id and not chosen from a list.
 */
public final class FrameRecipe extends CustomRecipe {
    //? if >=26.1 {
    /*public FrameRecipe() { }
    *///?} else if >=1.20.2 {
    public FrameRecipe(net.minecraft.world.item.crafting.CraftingBookCategory category) { super(category); }
    //?} else if >=1.19.3 {
    /*public FrameRecipe(net.minecraft.resources.ResourceLocation id,net.minecraft.world.item.crafting.CraftingBookCategory category) { super(id,category); }
    *///?} else {
    /*public FrameRecipe(net.minecraft.resources.ResourceLocation id) { super(id); }
    *///?}

    /** The serializer every loader registers under simplymirrorsplus:frame. */
    public static RecipeSerializer<FrameRecipe> serializer() {
        //? if >=26.1 {
        /*// The recipe has no settings, so one instance stands for every copy read from a file or the network.
        FrameRecipe recipe=new FrameRecipe();
        return new RecipeSerializer<FrameRecipe>(com.mojang.serialization.MapCodec.unit(recipe),net.minecraft.network.codec.StreamCodec.unit(recipe));
        *///?} else if >=1.21.2 {
        return new CustomRecipe.Serializer<FrameRecipe>(FrameRecipe::new);
        //?} else if >=1.19.3 {
        /*return new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<FrameRecipe>(FrameRecipe::new);
        *///?} else {
        /*return new net.minecraft.world.item.crafting.SimpleRecipeSerializer<FrameRecipe>(FrameRecipe::new);
        *///?}
    }

    //? if >=1.21 {
    @Override public boolean matches(net.minecraft.world.item.crafting.CraftingInput grid,Level level) { return !result(grid).isEmpty(); }
    //? if >=26.1 {
    /*@Override public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput grid) { return result(grid); }
    *///?} else {
    @Override public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput grid,net.minecraft.core.HolderLookup.Provider registries) { return result(grid); }
    //?}
    //?} else {
    /*@Override public boolean matches(net.minecraft.world.inventory.CraftingContainer grid,Level level) { return !result(grid).isEmpty(); }
    //? if >=1.20.5 {
    @Override public ItemStack assemble(net.minecraft.world.inventory.CraftingContainer grid,net.minecraft.core.HolderLookup.Provider registries) { return result(grid); }
    //?} else if >=1.19.4 {
    /^@Override public ItemStack assemble(net.minecraft.world.inventory.CraftingContainer grid,net.minecraft.core.RegistryAccess registries) { return result(grid); }
    ^///?} else {
    /^@Override public ItemStack assemble(net.minecraft.world.inventory.CraftingContainer grid) { return result(grid); }
    ^///?}
    *///?}

    //? if <1.21.2 {
    /*@Override public boolean canCraftInDimensions(int width,int height) { return width*height>=2; }
    *///?}

    //? if >=1.21.2 {
    @Override public RecipeSerializer<? extends CustomRecipe> getSerializer() { return MirrorContent.frameRecipe.get(); }
    //?} else {
    /*@Override public RecipeSerializer<?> getSerializer() { return MirrorContent.frameRecipe.get(); }
    *///?}

    //? if >=1.21 {
    private static ItemStack result(net.minecraft.world.item.crafting.CraftingInput grid) {
        int size=grid.size();
    //?} else {
    /*private static ItemStack result(net.minecraft.world.inventory.CraftingContainer grid) {
        int size=grid.getContainerSize();
    *///?}
        ItemStack mirror=ItemStack.EMPTY;
        Block frame=null;
        for(int slot=0;slot<size;slot++) {
            ItemStack stack=grid.getItem(slot);
            if(stack.isEmpty())continue;
            if(stack.getItem() instanceof MirrorItem&&mirror.isEmpty())mirror=stack;
            else if(stack.getItem() instanceof BlockItem&&!(stack.getItem() instanceof MirrorItem)&&frame==null)frame=((BlockItem)stack.getItem()).getBlock();
            // Anything else in the grid, or a second mirror or block, and this is not the recipe.
            else return ItemStack.EMPTY;
        }
        if(mirror.isEmpty()||frame==null)return ItemStack.EMPTY;
        ItemStack framed=mirror.copy();
        framed.setCount(1);
        MirrorItem.setFrame(framed,MirrorContent.id(frame));
        return framed;
    }
}

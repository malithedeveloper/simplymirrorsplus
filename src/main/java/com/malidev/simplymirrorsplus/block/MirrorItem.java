package com.malidev.simplymirrorsplus.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** The mirror in one of its forms, as an item. A framed mirror carries the id of its frame's block in the stack. */
public final class MirrorItem extends BlockItem {
    public final MirrorBlock.Form form;

    public MirrorItem(Block block, MirrorBlock.Form form, Properties properties) {
        super(block, properties);
        this.form = form;
    }

    @Override public Component getName(ItemStack stack) {
        if (form == MirrorBlock.Form.FULL) return super.getName(stack);
        return text("block.simplymirrorsplus.mirror." + form.getSerializedName());
    }

    /** Names the frame under the item's name. */
    //? if >=1.21.5 {
    /*@Override public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
            java.util.function.Consumer<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        Component line = frameLine(stack);
        if (line != null) lines.accept(line);
    }
    *///?} else if >=1.20.5 {
    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, java.util.List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        Component line = frameLine(stack);
        if (line != null) lines.add(line);
    }
    //?} else {
    /*@Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        Component line = frameLine(stack);
        if (line != null) lines.add(line);
    }
    *///?}

    private static Component frameLine(ItemStack stack) {
        Block frame = MirrorContent.block(frame(stack));
        if (frame == null) return null;
        return text("simplymirrorsplus.frame", frame.getName()).withStyle(net.minecraft.ChatFormatting.GRAY);
    }

    //? if >=1.19 {
    private static net.minecraft.network.chat.MutableComponent text(String key, Object... arguments) { return Component.translatable(key, arguments); }
    //?} else {
    /*private static net.minecraft.network.chat.TranslatableComponent text(String key, Object... arguments) { return new net.minecraft.network.chat.TranslatableComponent(key, arguments); }
    *///?}

    /** Id of the block this mirror will be framed with when placed; empty for none. */
    public static String frame(ItemStack stack) {
        //? if >=1.20.5 {
        net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) return "";
        //? if >=1.21.5 {
        /*return data.copyTag().getStringOr("MirrorFrame", "");
        *///?} else {
        return data.copyTag().getString("MirrorFrame");
        //?}
        //?} else {
        /*net.minecraft.nbt.CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString("MirrorFrame");
        *///?}
    }

    public static void setFrame(ItemStack stack, String id) {
        //? if >=1.20.5 {
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("MirrorFrame", id));
        //?} else {
        /*stack.getOrCreateTag().putString("MirrorFrame", id);
        *///?}
    }
}

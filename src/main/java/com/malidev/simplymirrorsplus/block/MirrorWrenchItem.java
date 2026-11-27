package com.malidev.simplymirrorsplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
//? if >=1.16 {
import net.minecraft.world.item.context.UseOnContext;
//?} else {
/*import net.minecraft.world.item.UseOnContext;
*///?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A tool for building with mirrors. Used on a mirror it turns the mirror's outline a quarter; used while
 * sneaking it switches a round mirror between joining its neighbours and standing alone.
 */
public final class MirrorWrenchItem extends Item {
    public MirrorWrenchItem(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MirrorBlock)) return InteractionResult.PASS;
        Player player = context.getPlayer();
        //? if >=1.15 {
        boolean sneaking = player != null && player.isShiftKeyDown();
        //?} else {
        /*boolean sneaking = player != null && player.isSneaking();
        *///?}
        if (sneaking && !state.getValue(MirrorBlock.FORM).shape().round()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            BlockState changed = sneaking ? state.cycle(MirrorBlock.CONNECT) : state.setValue(MirrorBlock.ROTATION, (state.getValue(MirrorBlock.ROTATION) + 1) % 4);
            level.setBlock(pos, MirrorBlock.joined(changed, level, pos), 3);
            if (sneaking && player != null) tell(player, changed.getValue(MirrorBlock.CONNECT) ? "simplymirrorsplus.wrench.joins" : "simplymirrorsplus.wrench.alone");
        }
        //? if >=1.21.2 {
        return InteractionResult.SUCCESS;
        //?} else if >=1.16 {
        /*return InteractionResult.sidedSuccess(level.isClientSide());
        *///?} else {
        /*return InteractionResult.SUCCESS;
        *///?}
    }

    /** Shows a short translated line above the hotbar. */
    private static void tell(Player player, String key) {
        //? if >=26.1 {
        /*player.sendOverlayMessage(Component.translatable(key));
        *///?} else if >=1.19 {
        player.displayClientMessage(Component.translatable(key), true);
        //?} else {
        /*player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(key), true);
        *///?}
    }
}

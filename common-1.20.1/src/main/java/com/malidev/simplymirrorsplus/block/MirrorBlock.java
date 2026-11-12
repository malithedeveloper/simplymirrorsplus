package com.malidev.simplymirrorsplus.block;

import com.malidev.simplymirrorsplus.client.MirrorEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** All six faces reflect. Legacy FACING states remain valid for existing placed blocks. */
public final class MirrorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public enum Mode implements StringRepresentable {
        NORMAL("normal"), PLAYERS_ONLY("players_only"), OFF("off");
        private final String name;
        Mode(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    static final int MAX_CONNECTED = 4096;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public MirrorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODE, Mode.NORMAL).setValue(POWERED, false));
    }

    /** What the renderer should do with this mirror: a redstone signal turns it off. */
    public static int renderMode(BlockState state) {
        if (state.getValue(POWERED) || state.getValue(MODE) == Mode.OFF) return MirrorEngine.MODE_OFF;
        return state.getValue(MODE) == Mode.PLAYERS_ONLY ? MirrorEngine.MODE_PLAYERS : MirrorEngine.MODE_NORMAL;
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, MODE, POWERED); }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        return defaultBlockState()
            .setValue(FACING, face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite())
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    /** Sneaking with an empty hand cycles Normal, Players Only and Off. */
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            Mode next = Mode.values()[(state.getValue(MODE).ordinal() + 1) % Mode.values().length];
            setModeConnected(level, pos, next);
            player.displayClientMessage(Component.translatable("simplymirrorsplus.mode." + next.getSerializedName()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Mirrors joined face to face form one surface, so they switch mode together. */
    private static void setModeConnected(Level level, BlockPos start, Mode mode) {
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<BlockPos>();
        java.util.Set<BlockPos> seen = new java.util.HashSet<BlockPos>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (state.getValue(MODE) != mode) level.setBlock(pos, state.setValue(MODE, mode), 3);
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                // ponytail: caps one click at MAX_CONNECTED blocks; a huge mirror wall switches in parts beyond that.
                if (seen.size() < MAX_CONNECTED && !seen.contains(next) && level.isLoaded(next) && level.getBlockState(next).getBlock() instanceof MirrorBlock) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
    }

    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        if (!level.isClientSide && powered != state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MirrorBlockEntity(pos, state); }
}

package com.malidev.simplymirrorsplus.block;

import com.malidev.simplymirrorsplus.math.MirrorShape;
//? if >=1.20.3 <26.3
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
//? if >=1.16 {
import net.minecraft.world.item.context.BlockPlaceContext;
//?} else {
/*import net.minecraft.world.item.BlockPlaceContext;
*///?}
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The mirror in all its forms. As a full block all six faces reflect; the other forms are a half block, a block
 * cut to an outline or a flat plate on one side. Each form has an item of its own, which decides the form on placing.
 */
public final class MirrorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public enum Mode implements StringRepresentable {
        NORMAL("normal"), PLAYERS_ONLY("players_only"), OFF("off");
        private final String name;
        Mode(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    /** The names are the block state values and the model names; the order is that of {@link MirrorShape}. */
    public enum Form implements StringRepresentable {
        FULL, SLAB, PANEL, CIRCLE, CIRCLE_PANEL, HALF_CIRCLE, HALF_CIRCLE_PANEL, QUARTER_CIRCLE, QUARTER_CIRCLE_PANEL,
        INVERSE_QUARTER_CIRCLE, INVERSE_QUARTER_CIRCLE_PANEL, TRIANGLE, TRIANGLE_PANEL, DIAMOND, DIAMOND_PANEL, OCTAGON, OCTAGON_PANEL,
        ARCH, ARCH_PANEL, RING, RING_PANEL, HEART, HEART_PANEL, DOUBLE_SLAB,
        CIRCLE_C1, CIRCLE_C2, CIRCLE_C3, CIRCLE_C4, CIRCLE_C5, CIRCLE_C6, CIRCLE_C7, CIRCLE_C8, CIRCLE_C9, CIRCLE_C10, CIRCLE_C11, CIRCLE_C12, CIRCLE_C13, CIRCLE_C14, CIRCLE_C15,
        CIRCLE_PANEL_C1, CIRCLE_PANEL_C2, CIRCLE_PANEL_C3, CIRCLE_PANEL_C4, CIRCLE_PANEL_C5, CIRCLE_PANEL_C6, CIRCLE_PANEL_C7, CIRCLE_PANEL_C8, CIRCLE_PANEL_C9, CIRCLE_PANEL_C10, CIRCLE_PANEL_C11, CIRCLE_PANEL_C12, CIRCLE_PANEL_C13, CIRCLE_PANEL_C14, CIRCLE_PANEL_C15;
        private final String name = name().toLowerCase(java.util.Locale.ROOT);
        @Override public String getSerializedName() { return name; }
        public MirrorShape shape() { return MirrorShape.values()[ordinal()]; }
        /** The form the player holds for this one: two slabs in a block are slabs, a joined round mirror is a round mirror. */
        public Form held() { return this == DOUBLE_SLAB ? SLAB : shape().round() ? values()[shape().joined(0).ordinal()] : this; }
        /** Path of this form's item: the full block is plain "mirror". */
        public String item() { return held() == FULL ? "mirror" : "mirror_" + held().name; }
        /** The forms that have an item of their own, the full block first. */
        public static Form[] items() {
            java.util.List<Form> own = new java.util.ArrayList<Form>();
            for (Form form : values()) if (form.held() == form) own.add(form);
            return own.toArray(new Form[0]);
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    public static final EnumProperty<Form> FORM = EnumProperty.create("shape", Form.class);
    /** Whether a slab or plate sits on a wall, the floor or the ceiling. */
    public static final EnumProperty<net.minecraft.world.level.block.state.properties.AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    /** Quarter turns of a plate's outline, clockwise seen from the front. */
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty ROTATION = net.minecraft.world.level.block.state.properties.IntegerProperty.create("rotation", 0, 3);
    /** Whether a round mirror joins the mirrors next to it. The Mirror Wrench switches it. */
    public static final BooleanProperty CONNECT = BooleanProperty.create("connect");
    private static final VoxelShape CUBE = Block.box(0, 0, 0, 16, 16, 16);
    /** Collision boxes of a slab, by the direction the mirror looks in. */
    private static final VoxelShape[] SLABS = sides(8);
    /** Boxes of the outlined forms, by form and placement; filled in as states are first asked for. */
    private static final java.util.Map<Integer, VoxelShape> OUTLINES = new java.util.concurrent.ConcurrentHashMap<Integer, VoxelShape>();
    /** A plate is drawn one pixel thick; two make it easier to aim at. */
    private static final int PLATE_THICKNESS = 2;
    //? if >=1.20.3 <26.3
    public static final MapCodec<MirrorBlock> CODEC = simpleCodec(MirrorBlock::new);

    public MirrorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODE, Mode.NORMAL).setValue(CONNECT, true)
            .setValue(FORM, Form.FULL).setValue(FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL).setValue(ROTATION, 0));
    }

    /** What the renderer should do with this mirror. Nothing sets a mode any more; worlds from older versions may hold one. */
    public static int renderMode(BlockState state) {
        if (state.getValue(MODE) == Mode.OFF) return com.malidev.simplymirrorsplus.client.MirrorEngine.MODE_OFF;
        return state.getValue(MODE) == Mode.PLAYERS_ONLY ? com.malidev.simplymirrorsplus.client.MirrorEngine.MODE_PLAYERS : com.malidev.simplymirrorsplus.client.MirrorEngine.MODE_NORMAL;
    }

    //? if >=1.20.3 <26.3
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, MODE, CONNECT, FORM, FACE, ROTATION); }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        net.minecraft.world.level.block.state.properties.AttachFace attach = face == Direction.UP ? net.minecraft.world.level.block.state.properties.AttachFace.FLOOR
            : face == Direction.DOWN ? net.minecraft.world.level.block.state.properties.AttachFace.CEILING : net.minecraft.world.level.block.state.properties.AttachFace.WALL;
        net.minecraft.world.item.Item item = context.getItemInHand().getItem();
        Form form = item instanceof MirrorItem ? ((MirrorItem) item).form : Form.FULL;
        // A second slab put into a block that holds one fills it, as with any slab.
        BlockState here = context.getLevel().getBlockState(context.getClickedPos());
        if (form == Form.SLAB && here.getBlock() == this && here.getValue(FORM) == Form.SLAB) return here.setValue(FORM, Form.DOUBLE_SLAB);
        BlockState state = defaultBlockState().setValue(FORM, form).setValue(FACING, facing).setValue(FACE, attach).setValue(ROTATION, quarter(context, face, facing));
        return joined(state, context.getLevel(), context.getClickedPos());
    }

    /**
     * An outline is turned towards the corner of the face that was clicked, so a quarter circle put
     * down near a corner curves around that corner.
     */
    private static int quarter(BlockPlaceContext context, Direction face, Direction facing) {
        net.minecraft.world.phys.Vec3 hit = context.getClickLocation();
        BlockPos pos = context.getClickedPos();
        double x = hit.x - pos.getX() - .5, y = hit.y - pos.getY() - .5, z = hit.z - pos.getZ() - .5;
        Direction look = look(face == Direction.UP ? net.minecraft.world.level.block.state.properties.AttachFace.FLOOR
            : face == Direction.DOWN ? net.minecraft.world.level.block.state.properties.AttachFace.CEILING : net.minecraft.world.level.block.state.properties.AttachFace.WALL, facing);
        double[] right = MirrorShape.right(look.getStepX(), look.getStepY(), look.getStepZ(), facing.getStepX(), facing.getStepZ());
        double[] up = MirrorShape.up(look.getStepX(), look.getStepY(), look.getStepZ(), facing.getStepX(), facing.getStepZ());
        boolean onRight = x * right[0] + y * right[1] + z * right[2] > 0, onTop = x * up[0] + y * up[1] + z * up[2] > 0;
        // Unturned, the outline's corner is at the bottom left; each turn moves it one corner clockwise.
        return onRight ? (onTop ? 2 : 3) : (onTop ? 1 : 0);
    }

    /**
     * A slab gives way to a second slab: one aimed at its mirror side, or one that would otherwise go into the
     * half of the block it leaves free.
     */
    @Override public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        net.minecraft.world.item.Item item = context.getItemInHand().getItem();
        if (state.getValue(FORM) != Form.SLAB || !(item instanceof MirrorItem) || ((MirrorItem) item).form != Form.SLAB) return false;
        return !context.replacingClickedOnBlock() || context.getClickedFace() == look(state.getValue(FACE), state.getValue(FACING));
    }

    /**
     * A round mirror with the mirrors beside it taken into account: a corner stays round only while both sides
     * that meet in it are free. Any other form comes back unchanged.
     */
    public static BlockState joined(BlockState state, BlockGetter level, BlockPos pos) {
        MirrorShape shape = state.getValue(FORM).shape();
        if (!shape.round()) return state;
        int corners = 0;
        if (state.getValue(CONNECT)) {
            Direction facing = state.getValue(FACING), look = look(state.getValue(FACE), facing);
            double[] right = MirrorShape.right(look.getStepX(), look.getStepY(), look.getStepZ(), facing.getStepX(), facing.getStepZ());
            double[] up = MirrorShape.up(look.getStepX(), look.getStepY(), look.getStepZ(), facing.getStepX(), facing.getStepZ());
            boolean left = joins(level, pos, right, -1, look, shape), east = joins(level, pos, right, 1, look, shape);
            boolean below = joins(level, pos, up, -1, look, shape), above = joins(level, pos, up, 1, look, shape);
            corners = (left || below ? MirrorShape.BOTTOM_LEFT : 0) | (east || below ? MirrorShape.BOTTOM_RIGHT : 0)
                | (left || above ? MirrorShape.TOP_LEFT : 0) | (east || above ? MirrorShape.TOP_RIGHT : 0);
        }
        return state.setValue(FORM, Form.values()[shape.joined(corners).ordinal()]);
    }

    /** Whether the block one step along the direction is a mirror this round one runs on into. */
    private static boolean joins(BlockGetter level, BlockPos pos, double[] direction, int steps, Direction look, MirrorShape shape) {
        BlockState other = level.getBlockState(new BlockPos(pos.getX() + (int) direction[0] * steps, pos.getY() + (int) direction[1] * steps, pos.getZ() + (int) direction[2] * steps));
        if (!(other.getBlock() instanceof MirrorBlock)) return false;
        MirrorShape there = other.getValue(FORM).shape();
        if (there.cube()) return true;
        return there.round() && there.flat() == shape.flat() && other.getValue(CONNECT) && look(other.getValue(FACE), other.getValue(FACING)) == look;
    }

    /** Joins a round mirror anew whenever a block beside it changes. */
    //? if >=1.21.2 {
    @Override protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbour, net.minecraft.util.RandomSource random) {
        return joined(state, level, pos);
    }
    //?} else {
    /*@Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return joined(state, level, pos);
    }
    *///?}

    /** A mirror placed from a framed stack keeps the frame. This runs on both sides, so the player who places it sees it at once. */
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        String frame = MirrorItem.frame(stack);
        BlockEntity entity = level.getBlockEntity(pos);
        if (!frame.isEmpty() && entity instanceof MirrorBlockEntity) ((MirrorBlockEntity) entity).setFrame(frame);
    }

    /** The direction a mirror in this state looks in; a slab or plate is attached on the opposite side. */
    public static Direction look(net.minecraft.world.level.block.state.properties.AttachFace face, Direction facing) {
        return face == net.minecraft.world.level.block.state.properties.AttachFace.FLOOR ? Direction.UP
            : face == net.minecraft.world.level.block.state.properties.AttachFace.CEILING ? Direction.DOWN : facing;
    }

    /** The reflecting surfaces of the mirror at this position. */
    public static java.util.List<com.malidev.simplymirrorsplus.math.MirrorSurface> surfaces(BlockState state, int x, int y, int z) {
        Direction facing = state.getValue(FACING), look = look(state.getValue(FACE), facing);
        return state.getValue(FORM).shape().surfaces(x, y, z, look.getStepX(), look.getStepY(), look.getStepZ(), facing.getStepX(), facing.getStepZ(), state.getValue(ROTATION));
    }

    private static VoxelShape[] sides(int thickness) {
        int t = thickness, f = 16 - thickness;
        // In the order of Direction: down, up, north, south, west, east. The box is on the side the mirror looks away from.
        return new VoxelShape[]{Block.box(0, f, 0, 16, 16, 16), Block.box(0, 0, 0, 16, t, 16), Block.box(0, 0, f, 16, 16, 16),
            Block.box(0, 0, 0, 16, 16, t), Block.box(f, 0, 0, 16, 16, 16), Block.box(0, 0, 0, t, 16, 16)};
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    /**
     * Only the full block hides the faces of its neighbours. A plate's box is larger than what its model draws,
     * so using it here would cut holes in the wall behind a round mirror.
     */
    //? if >=1.21.2 {
    @Override public VoxelShape getOcclusionShape(BlockState state) {
    //?} else {
    /*@Override public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
    *///?}
        return state.getValue(FORM).shape().cube() ? CUBE : net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Form form = state.getValue(FORM);
        if (form.shape().cube()) return CUBE;
        Direction facing = state.getValue(FACING), look = look(state.getValue(FACE), facing);
        if (form == Form.SLAB) return SLABS[look.ordinal()];
        int rotation = state.getValue(ROTATION), key = ((form.ordinal() * 6 + look.ordinal()) * 4 + facing.get2DDataValue()) * 4 + rotation;
        VoxelShape shape = OUTLINES.get(key);
        if (shape == null) OUTLINES.put(key, shape = outline(form.shape(), look, facing, rotation));
        return shape;
    }

    /** The boxes of an outlined form, laid out in the block the way its model and its reflection are. */
    private static VoxelShape outline(MirrorShape shape, Direction look, Direction facing, int rotation) {
        double[] right = MirrorShape.right(look.getStepX(), look.getStepY(), look.getStepZ(), facing.getStepX(), facing.getStepZ());
        double[] up = MirrorShape.up(look.getStepX(), look.getStepY(), look.getStepZ(), facing.getStepX(), facing.getStepZ());
        double[] out = {look.getStepX(), look.getStepY(), look.getStepZ()};
        double front = shape.flat() ? PLATE_THICKNESS - 8 : 8;
        VoxelShape all = net.minecraft.world.phys.shapes.Shapes.empty();
        for (int[] box : shape.rectangles(rotation)) {
            double[] low = new double[3], high = new double[3];
            for (int axis = 0; axis < 3; axis++) {
                double a = 8 + right[axis] * (box[0] - 8) + up[axis] * (box[1] - 8) - out[axis] * 8;
                double b = 8 + right[axis] * (box[2] - 8) + up[axis] * (box[3] - 8) + out[axis] * front;
                low[axis] = Math.min(a, b);
                high[axis] = Math.max(a, b);
            }
            all = net.minecraft.world.phys.shapes.Shapes.or(all, Block.box(low[0], low[1], low[2], high[0], high[1], high[2]));
        }
        return all;
    }

    /** Picking a mirror gives the item of its form. */
    //? if >=1.21.4 {
    @Override protected net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
    //?} else if >=1.20.3 {
    /*@Override public net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
    *///?} else {
    /*@Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
    *///?}
        return new net.minecraft.world.item.ItemStack(MirrorContent.item(state.getValue(FORM).held()));
    }

    //? if >=1.17 {
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MirrorBlockEntity(pos, state); }
    //?} else {
    /*@Override public BlockEntity newBlockEntity(BlockGetter level) { return new MirrorBlockEntity(); }
    *///?}
}

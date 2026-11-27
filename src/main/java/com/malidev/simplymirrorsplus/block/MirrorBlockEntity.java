package com.malidev.simplymirrorsplus.block;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Marks where a mirror is, so the client finds mirrors without scanning every block, and remembers the block
 * the mirror is framed with.
 */
public final class MirrorBlockEntity extends BlockEntity {
    private static final String KEY="Frame";
    /** Id of the framing block, such as minecraft:gold_block; empty for a mirror without a frame. */
    private String frame="";

    //? if >=1.17 {
    public MirrorBlockEntity(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state) { super(MirrorContent.blockEntityType.get(),pos,state); }
    //?} else {
    /*public MirrorBlockEntity() { super(MirrorContent.blockEntityType.get()); }
    *///?}

    public String frame() { return frame; }

    public void setFrame(String id) {
        frame=id==null?"":id;
        setChanged();
        if(level!=null)level.sendBlockUpdated(getBlockPos(),getBlockState(),getBlockState(),3);
    }

    // The key is written only for a framed mirror, so the loot table has nothing to copy onto a plain one.
    //? if >=1.21.6 {
    /*@Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) { super.loadAdditional(input);frame=input.getStringOr(KEY,""); }
    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) { super.saveAdditional(output);if(!frame.isEmpty())output.putString(KEY,frame); }
    *///?} else if >=1.20.5 {
    @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries) { super.loadAdditional(tag,registries);frame=read(tag); }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries) { super.saveAdditional(tag,registries);if(!frame.isEmpty())tag.putString(KEY,frame); }
    //?} else if >=1.18 {
    /*@Override public void load(net.minecraft.nbt.CompoundTag tag) { super.load(tag);frame=read(tag); }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) { super.saveAdditional(tag);if(!frame.isEmpty())tag.putString(KEY,frame); }
    *///?} else {
    /*//? if >=1.17 || <1.16 {
    @Override public void load(net.minecraft.nbt.CompoundTag tag) { super.load(tag);frame=read(tag); }
    //?} else {
    /^@Override public void load(net.minecraft.world.level.block.state.BlockState state,net.minecraft.nbt.CompoundTag tag) { super.load(state,tag);frame=read(tag); }
    ^///?}
    @Override public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag) { super.save(tag);if(!frame.isEmpty())tag.putString(KEY,frame);return tag; }
    *///?}

    //? if <1.21.6 {
    private static String read(net.minecraft.nbt.CompoundTag tag) {
        //? if >=1.21.5 {
        /*return tag.getStringOr(KEY,"");
        *///?} else {
        return tag.getString(KEY);
        //?}
    }
    //?}

    // Clients learn the frame with the chunk and again whenever it changes.
    //? if >=1.20.5 {
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    //?} else if >=1.18 {
    /*@Override public net.minecraft.nbt.CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    *///?} else {
    /*@Override public net.minecraft.nbt.CompoundTag getUpdateTag() { return save(new net.minecraft.nbt.CompoundTag()); }
    *///?}

    //? if >=1.18 {
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    //?} else {
    /*// ponytail: before 1.18 the game applies this packet only to its own block entities, so other players see a
    // new frame once the chunk reloads; a custom packet would fix that.
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return new net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket(getBlockPos(),0,getUpdateTag());
    }
    *///?}
}

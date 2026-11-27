package com.malidev.simplymirrorsplus.client;

import com.malidev.simplymirrorsplus.block.MirrorBlock;
import com.malidev.simplymirrorsplus.block.MirrorItem;
import com.malidev.simplymirrorsplus.math.MirrorItemFrame;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the frame of a framed mirror on the item itself, in the inventory and in the hand: bars along the
 * edges of the mirror's form, in the texture of the frame's block. Each version's item renderer calls in here
 * from the place where the item's own model has just been drawn and its pose still stands.
 */
public final class ItemFrames {
    /** The frame and form of the item whose render state is being drawn; set by the state, taken by its first layer (1.21.4 on). */
    public static String frame;
    public static MirrorBlock.Form form;
    private static final java.util.Map<MirrorBlock.Form,float[]> QUADS=new java.util.EnumMap<MirrorBlock.Form,float[]>(MirrorBlock.Form.class);

    private ItemFrames() { }

    /** The id of the block this stack is framed with, or null when it is no framed mirror. */
    public static String frameOf(ItemStack stack) {
        if(!(stack.getItem() instanceof MirrorItem))return null;
        String id=MirrorItem.frame(stack);
        return id.isEmpty()?null:id;
    }

    public static MirrorBlock.Form formOf(ItemStack stack) { return ((MirrorItem)stack.getItem()).form; }

    /** The bars around the box this form's item model fills. */
    private static float[] quads(MirrorBlock.Form form) {
        float[] quads=QUADS.get(form);
        if(quads==null) {
            // The slab's item is the lower half of the block; a plate lies against the south side.
            quads=form==MirrorBlock.Form.SLAB?MirrorItemFrame.quads(0,0,0,1,.5,1):form.shape().flat()?MirrorItemFrame.quads(0,0,15/16d,1,1,1):MirrorItemFrame.quads(0,0,0,1,1,1);
            QUADS.put(form,quads);
        }
        return quads;
    }

    //? if >=1.15 {
    /**
     * @param pose the pose the item's own model was drawn with
     * @param consumer takes vertices for a cutout layer on the block atlas
     */
    public static void draw(com.mojang.blaze3d.vertex.PoseStack.Pose pose,com.mojang.blaze3d.vertex.VertexConsumer consumer,String frame,MirrorBlock.Form form,int light,int overlay) {
        float[] sprite=MirrorRenderer.frameSprite(frame);
        if(sprite==null)return;
        float[] m=MirrorRenderer.array(pose.pose()),quads=quads(form);
        for(int quad=0;quad<quads.length;quad+=MirrorItemFrame.STRIDE) {
            float nx=quads[quad],ny=quads[quad+1],nz=quads[quad+2];
            float tx=m[0]*nx+m[4]*ny+m[8]*nz,ty=m[1]*nx+m[5]*ny+m[9]*nz,tz=m[2]*nx+m[6]*ny+m[10]*nz;
            float length=(float)Math.sqrt(tx*tx+ty*ty+tz*tz);
            if(length>0) { tx/=length;ty/=length;tz/=length; }
            for(int corner=0;corner<4;corner++) {
                int at=quad+3+corner*5;
                float x=quads[at],y=quads[at+1],z=quads[at+2];
                float px=m[0]*x+m[4]*y+m[8]*z+m[12],py=m[1]*x+m[5]*y+m[9]*z+m[13],pz=m[2]*x+m[6]*y+m[10]*z+m[14];
                float u=sprite[0]+(sprite[2]-sprite[0])*quads[at+3],v=sprite[3]-(sprite[3]-sprite[1])*quads[at+4];
                //? if >=1.21 {
                consumer.addVertex(px,py,pz,0xFFFFFFFF,u,v,overlay,light,tx,ty,tz);
                //?} else {
                /*consumer.vertex(px,py,pz,1,1,1,1,u,v,overlay,light,tx,ty,tz);
                *///?}
            }
        }
    }
    //?} else {
    /*/^* Minecraft 1.14 draws items with the fixed OpenGL pipeline; the item's matrix is the current one. ^/
    public static void drawLegacy(ItemStack stack) {
        String frame=frameOf(stack);
        float[] sprite=frame==null?null:MirrorRenderer.frameSprite(frame);
        if(sprite==null)return;
        float[] quads=quads(formOf(stack));
        com.mojang.blaze3d.platform.GlStateManager.pushMatrix();
        com.mojang.blaze3d.platform.GlStateManager.translatef(-.5f,-.5f,-.5f);
        net.minecraft.client.Minecraft.getInstance().getTextureManager().bind(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        com.mojang.blaze3d.vertex.Tesselator tesselator=com.mojang.blaze3d.vertex.Tesselator.getInstance();
        com.mojang.blaze3d.vertex.BufferBuilder builder=tesselator.getBuilder();
        builder.begin(7,com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_NORMAL);
        for(int quad=0;quad<quads.length;quad+=MirrorItemFrame.STRIDE)for(int corner=0;corner<4;corner++) {
            int at=quad+3+corner*5;
            builder.vertex(quads[at],quads[at+1],quads[at+2]).uv(sprite[0]+(sprite[2]-sprite[0])*quads[at+3],sprite[3]-(sprite[3]-sprite[1])*quads[at+4])
                .normal(quads[quad],quads[quad+1],quads[quad+2]).endVertex();
        }
        tesselator.end();
        com.mojang.blaze3d.platform.GlStateManager.popMatrix();
    }
    *///?}
}

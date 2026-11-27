package com.malidev.simplymirrorsplus.client;

import com.malidev.simplymirrorsplus.block.MirrorBlock;
import com.malidev.simplymirrorsplus.block.MirrorBlockEntity;
import com.malidev.simplymirrorsplus.math.MirrorSurface;
import com.mojang.blaze3d.pipeline.RenderTarget;
//? if >=1.17
import com.mojang.blaze3d.pipeline.TextureTarget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import static org.lwjgl.opengl.GL33.*;

/**
 * The part of the Minecraft side that every version shares: finding mirrors, the images they are drawn into
 * and the final composite. How the world gets drawn into an image changed three times, so that lives in
 * {@code LegacyRenderer} (1.14), {@code DirectRenderer} (1.15 to 1.21.8), {@code StateRenderer} (1.21.9 to 1.21.11) and {@code ExtractRenderer} (26.1 on).
 */
public abstract class MirrorRenderer implements MirrorEngine.Platform<RenderTarget> {
    /** The image the world is being drawn into while a reflection renders, otherwise null. The mixins read it. */
    public static RenderTarget redirect;
    /** Set while a Players Only reflection renders on the versions that trim the extracted world state for it. */
    static boolean playersPass;
    static final MirrorRenderer INSTANCE=create();

    final MirrorEngine<RenderTarget> engine=new MirrorEngine<RenderTarget>(this);
    final GlMirrorComposite composite=new GlMirrorComposite();
    final GlMirrorBackdrop backdrop=new GlMirrorBackdrop();
    final GlMirrorUi screen=new GlMirrorUi();
    final GlMirrorFrames frameDrawer=new GlMirrorFrames();
    /** Frame block id of every framed mirror block found by the last search, by packed position. */
    private final java.util.Map<Long,String> frames=new java.util.HashMap<Long,String>();
    /** Where each frame block's texture sits in the block atlas: u0, v0, u1, v1. */
    private final java.util.Map<String,float[]> sprites=new java.util.HashMap<String,float[]>();
    /** Every mirror found by the last search: id, lowest corner and block count. */
    private final List<Object[]> found=new ArrayList<Object[]>();
    private final BlockPos.MutableBlockPos query=new BlockPos.MutableBlockPos();
    /** Mirror id of every mirror block found by the last search, by packed position. */
    private final java.util.Map<Long,String> owners=new java.util.HashMap<Long,String>();
    /** The framed surfaces of the last search by plane and frame block: the block id and the surfaces. Null until asked for. */
    private List<Object[]> framedPlanes;
    /** Chiselled blocks with mirror bits found by the last search, by packed position. */
    private final java.util.Set<Long> chiselled=new java.util.HashSet<Long>();
    //? if >=1.21.5
    //int compositeFramebuffer;

    MirrorRenderer() { ShaderReflection.matrices=new ShaderMatrices(); }

    private static MirrorRenderer create() {
        //? if >=26.1 {
        /*return new ExtractRenderer();
        *///?} else if >=1.21.9 {
        /*return new StateRenderer();
        *///?} else if >=1.15 {
        return new DirectRenderer();
        //?} else {
        /*return new LegacyRenderer();
        *///?}
    }

    /** Frees every image and OpenGL object; called when the world or the game closes. */
    public static void release() {
        INSTANCE.engine.release();
        INSTANCE.composite.close();
        INSTANCE.backdrop.close();
        INSTANCE.screen.close();
        INSTANCE.frameDrawer.close();
        INSTANCE.sprites.clear();
        INSTANCE.releaseResources();
        redirect=null;
    }

    /** Sodium and the cloud, particle and weather passes ask this to leave the world out of a Players Only reflection. */
    public static boolean playersOnlyPass() { return redirect!=null&&playersPass; }

    void releaseResources() {
        //? if >=1.21.5 {
        /*if(compositeFramebuffer!=0)glDeleteFramebuffers(compositeFramebuffer);
        compositeFramebuffer=0;
        *///?}
    }

    @Override public RenderTarget createTarget(int width,int height) {
        //? if >=26.3 {
        /*RenderTarget target=new TextureTarget("Simply Mirrors Plus reflection",width,height,com.mojang.blaze3d.GpuFormat.RGBA8_UNORM,com.mojang.blaze3d.GpuFormat.D32_FLOAT);
        *///?} else if >=26.2 {
        /*RenderTarget target=new TextureTarget("Simply Mirrors Plus reflection",width,height,true,com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);
        *///?} else if >=1.21.5 {
        /*RenderTarget target=new TextureTarget("Simply Mirrors Plus reflection",width,height,true);
        *///?} else if >=1.21.2 {
        RenderTarget target=new TextureTarget(width,height,true);
        //?} else if >=1.17 {
        /*RenderTarget target=new TextureTarget(width,height,true,Minecraft.ON_OSX);
        *///?} else {
        /*RenderTarget target=new RenderTarget(width,height,true,Minecraft.ON_OSX);
        *///?}
        // Reprojected and downscaled images do not land on texel centres, so they need linear filtering.
        //? if >=1.21.9 {
        /*int bound=glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D,colorTexture(target));
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D,bound);
        *///?} else if >=1.21.5 {
        /*target.setFilterMode(com.mojang.blaze3d.textures.FilterMode.LINEAR);
        *///?} else {
        target.setFilterMode(GL_LINEAR);
        //?}
        return target;
    }

    @Override public void destroyTarget(RenderTarget target) { target.destroyBuffers(); }
    @Override public int width(RenderTarget target) { return target.width; }
    @Override public int height(RenderTarget target) { return target.height; }
    @Override public boolean shadersActive() { return ShaderReflection.enabled(); }
    @Override public boolean depthZeroToOne() { return false; }

    @Override public int colorTexture(Object target) {
        //? if >=1.21.5 {
        /*return glId(((RenderTarget)target).getColorTexture());
        *///?} else if >=1.16 {
        return ((RenderTarget)target).getColorTextureId();
        //?} else {
        /*return ((RenderTarget)target).colorTextureId;
        *///?}
    }

    //? if >=1.21.5 {
    /*/^* NeoForge's validation layer wraps textures, so the OpenGL one may sit a level down. ^/
    static int glId(Object texture) { return ((com.mojang.blaze3d.opengl.GlTexture)GpuTextures.unwrap(texture)).glId(); }

    /^* False on a graphics backend the mirrors cannot draw with, such as Vulkan. ^/
    static boolean openGl(RenderTarget main) { return GpuTextures.unwrap(main.getColorTexture()) instanceof com.mojang.blaze3d.opengl.GlTexture; }
    *///?}

    @Override public List<MirrorSurface> findSurfaces(double x,double y,double z,int chunks) {
        Level level=Minecraft.getInstance().level;
        List<MirrorSurface> surfaces=new ArrayList<MirrorSurface>();
        List<BlockPos> blocks=new ArrayList<BlockPos>();
        frames.clear();
        chiselled.clear();
        //? if chisels
        //ChiselledMirrors.nextSearch();
        int cx=(int)Math.floor(x/16),cz=(int)Math.floor(z/16);
        for(int chunkX=cx-chunks;chunkX<=cx+chunks;chunkX++)for(int chunkZ=cz-chunks;chunkZ<=cz+chunks;chunkZ++) {
            if(!level.hasChunk(chunkX,chunkZ))continue;
            for(BlockEntity entity:level.getChunk(chunkX,chunkZ).getBlockEntities().values()) {
                //? if chisels {
                /*if(!(entity instanceof MirrorBlockEntity)) {
                    List<MirrorSurface> bits=ChiselledMirrors.surfaces(entity);
                    if(bits!=null) {
                        BlockPos at=entity.getBlockPos();
                        chiselled.add(BlockPos.asLong(at.getX(),at.getY(),at.getZ()));
                        blocks.add(at);
                        surfaces.addAll(bits);
                    }
                    continue;
                }
                *///?} else {
                if(!(entity instanceof MirrorBlockEntity))continue;
                //?}
                BlockPos pos=entity.getBlockPos();
                blocks.add(pos);
                BlockState state=entity.getBlockState();
                String frame=((MirrorBlockEntity)entity).frame();
                if(!frame.isEmpty())frames.put(BlockPos.asLong(pos.getX(),pos.getY(),pos.getZ()),frame);
                if(state.getBlock() instanceof MirrorBlock)surfaces.addAll(MirrorBlock.surfaces(state,pos.getX(),pos.getY(),pos.getZ()));
            }
        }
        nameMirrors(blocks);
        framedPlanes=null;
        return surfaces;
    }

    /**
     * Mirror blocks that touch form one mirror. A mirror with settings of its own is named after the block it
     * was named after when it got them, so it keeps them while blocks are added or taken away around that one.
     * Any other mirror is named after its lowest corner.
     */
    private void nameMirrors(List<BlockPos> blocks) {
        java.util.Map<Long,String> before=new java.util.HashMap<Long,String>(owners);
        owners.clear();
        found.clear();
        java.util.Map<Long,BlockPos> open=new java.util.HashMap<Long,BlockPos>();
        for(BlockPos pos:blocks)open.put(BlockPos.asLong(pos.getX(),pos.getY(),pos.getZ()),pos);
        java.util.ArrayDeque<BlockPos> queue=new java.util.ArrayDeque<BlockPos>();
        List<BlockPos> members=new ArrayList<BlockPos>();
        java.util.Set<String> taken=new java.util.HashSet<String>();
        while(!open.isEmpty()) {
            java.util.Iterator<java.util.Map.Entry<Long,BlockPos>> first=open.entrySet().iterator();
            BlockPos lowest=first.next().getValue();
            first.remove();
            queue.add(lowest);
            members.clear();
            while(!queue.isEmpty()) {
                BlockPos pos=queue.poll();
                members.add(pos);
                if(pos.getY()<lowest.getY()||pos.getY()==lowest.getY()&&(pos.getX()<lowest.getX()||pos.getX()==lowest.getX()&&pos.getZ()<lowest.getZ()))lowest=pos;
                for(int side=0;side<6;side++) {
                    int x=pos.getX()+(side==0?1:side==1?-1:0),y=pos.getY()+(side==2?1:side==3?-1:0),z=pos.getZ()+(side==4?1:side==5?-1:0);
                    BlockPos next=open.remove(BlockPos.asLong(x,y,z));
                    if(next!=null)queue.add(next);
                }
            }
            String id=null,inherited=null;
            for(BlockPos member:members) {
                String own=member.getX()+"_"+member.getY()+"_"+member.getZ(),old=before.get(BlockPos.asLong(member.getX(),member.getY(),member.getZ()));
                if(id==null&&MirrorConfig.mirror(own)!=null&&!taken.contains(own))id=own;
                if(inherited==null&&old!=null&&MirrorConfig.mirror(old)!=null&&!taken.contains(old))inherited=old;
            }
            if(id==null) {
                id=lowest.getX()+"_"+lowest.getY()+"_"+lowest.getZ();
                // The block the settings were named after is gone; they move on with the rest of the mirror.
                if(inherited!=null&&!inherited.equals(id))MirrorConfig.rename(inherited,id);
            }
            taken.add(id);
            for(BlockPos member:members)owners.put(BlockPos.asLong(member.getX(),member.getY(),member.getZ()),id);
            found.add(new Object[]{id,lowest,members.size()});
        }
    }

    /** The id of the mirror this block belongs to, or null when the block is no mirror the last search found. */
    public static String mirrorAt(int x,int y,int z) { return INSTANCE.owners.get(BlockPos.asLong(x,y,z)); }

    @Override public String mirrorId(MirrorSurface face) {
        String id=owners.get(BlockPos.asLong(face.x,face.y,face.z));
        return id==null?"":id;
    }

    /** The mirrors around the player, nearest first, for the settings screen. */
    public static List<MirrorUi.Entry> mirrors() {
        Minecraft mc=Minecraft.getInstance();
        List<MirrorUi.Entry> entries=new ArrayList<MirrorUi.Entry>();
        if(mc.player==null)return entries;
        // The bounding box is the one way to the player's position that every version shares.
        net.minecraft.world.phys.AABB box=mc.player.getBoundingBox();
        double x=(box.minX+box.maxX)/2,y=box.minY,z=(box.minZ+box.maxZ)/2;
        for(Object[] mirror:INSTANCE.found) {
            BlockPos corner=(BlockPos)mirror[1];
            double dx=corner.getX()+.5-x,dy=corner.getY()+.5-y,dz=corner.getZ()+.5-z;
            entries.add(new MirrorUi.Entry((String)mirror[0],corner.getX(),corner.getY(),corner.getZ(),(Integer)mirror[2],Math.sqrt(dx*dx+dy*dy+dz*dz)));
        }
        java.util.Collections.sort(entries,new java.util.Comparator<MirrorUi.Entry>() {
            @Override public int compare(MirrorUi.Entry a,MirrorUi.Entry b) { return Double.compare(a.distance,b.distance); }
        });
        return entries;
    }

    /**
     * Draws the live preview and the colour picker of the settings screen over the finished frame. Both are put
     * on screen with plain OpenGL, so they do not depend on any version's GUI code.
     */
    public static void drawScreen() {
        Minecraft mc=Minecraft.getInstance();
        if(!(currentScreen(mc) instanceof MirrorSettingsScreen)) { INSTANCE.engine.requestPreview("",0,0);return; }
        MirrorSettingsScreen settings=(MirrorSettingsScreen)currentScreen(mc);
        MirrorUi ui=settings.ui;
        RenderTarget main=mainTarget();
        //? if >=1.21.5 {
        /*if(!openGl(main))return;
        *///?}
        //? if >=1.15 {
        com.mojang.blaze3d.platform.Window window=mc.getWindow();
        //?} else {
        /*com.mojang.blaze3d.platform.Window window=mc.window;
        *///?}
        // The mouse is reported in window units, which differ from pixels on high-density displays.
        double scaleX=(double)main.width/Math.max(1,window.getScreenWidth()),scaleY=(double)main.height/Math.max(1,window.getScreenHeight());
        ui.mouse(mc.mouseHandler.xpos()*scaleX,mc.mouseHandler.ypos()*scaleY);
        settings.frame(main.width,main.height,previewAspect());
        boolean changed=ui.paint(main.width,main.height);
        settings.picked();
        int[] area=ui.previewArea(),space=ui.previewSpace();
        INSTANCE.engine.requestPreview(settings.previewed(),space[2],space[3]);
        RenderTarget preview=INSTANCE.engine.previewTarget();
        //? if >=1.21.5 {
        /*int oldDraw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        if(INSTANCE.compositeFramebuffer==0)INSTANCE.compositeFramebuffer=glGenFramebuffers();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,INSTANCE.compositeFramebuffer);
        try {
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,glId(main.getColorTexture()),0);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,glId(main.getDepthTexture()),0);
            glViewport(0,0,main.width,main.height);
            if(changed)INSTANCE.screen.upload(ui.pixels(),main.width,main.height);
            INSTANCE.screen.draw(main.width,main.height,area,preview==null?0:INSTANCE.colorTexture(preview),INSTANCE.engine.previewFrame());
        } finally {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER,oldDraw);
        }
        *///?} else {
        main.bindWrite(true);
        if(changed)INSTANCE.screen.upload(ui.pixels(),main.width,main.height);
        INSTANCE.screen.draw(main.width,main.height,area,preview==null?0:INSTANCE.colorTexture(preview),INSTANCE.engine.previewFrame());
        //?}
    }

    /** Reads the pictures in the backgrounds folder again, after one was added or replaced. */
    public static void reloadPictures() { INSTANCE.backdrop.reload(); }

    /** Width over height of the mirror in the current preview picture, 0 without one. */
    public static float previewAspect() {
        RenderTarget preview=INSTANCE.engine.previewTarget();
        if(preview==null)return 0;
        float[] frame=INSTANCE.engine.previewFrame();
        return Math.abs((frame[2]-frame[0])*preview.width)/Math.max(1e-3f,Math.abs((frame[3]-frame[1])*preview.height));
    }

    private static net.minecraft.client.gui.screens.Screen currentScreen(Minecraft mc) {
        //? if >=26.2 {
        /*return mc.gui.screen();
        *///?} else {
        return mc.screen;
        //?}
    }

    @Override public Object viewer(float yaw,float pitch) {
        //? if >=1.15 {
        net.minecraft.client.Camera camera=new net.minecraft.client.Camera();
        ((com.malidev.simplymirrorsplus.client.mixin.CameraAccessor)camera).simplymirrorsplus$rotation(yaw,pitch);
        return camera;
        //?} else {
        /*return new float[]{yaw,pitch};
        *///?}
    }

    @Override public String worldKey() {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null)return "";
        // The integrated server's description is "player - world name" on every version.
        String world=mc.getCurrentServer()!=null?mc.getCurrentServer().ip:mc.getSingleplayerServer()!=null?mc.getSingleplayerServer().getMotd():"world";
        //? if >=1.16 {
        return world+"_"+mc.level.dimension();
        //?} else {
        /*return world+"_"+mc.level.getDimension().getType();
        *///?}
    }

    @Override public boolean solid(int x,int y,int z) {
        Level level=Minecraft.getInstance().level;
        //? if >=1.21.2 {
        return level.getBlockState(query.set(x,y,z)).isSolidRender();
        //?} else {
        /*return level.getBlockState(query.set(x,y,z)).isSolidRender(level,query);
        *///?}
    }

    @Override public int faceMode(MirrorSurface face) {
        Level level=Minecraft.getInstance().level;
        BlockState state=level.getBlockState(query.set(face.x,face.y,face.z));
        // A chiselled block stays what the search found it to be until the next search.
        if(chiselled.contains(BlockPos.asLong(face.x,face.y,face.z)))return MirrorEngine.MODE_NORMAL;
        if(!(state.getBlock() instanceof MirrorBlock))return MirrorEngine.MODE_NONE;
        // A face on the outside of the block is hidden when a mirror block stands against it. A block cut to an
        // outline counts like a full one: where two of them meet, as the quarters of a round mirror do, the faces between them are inside the mirror.
        // ponytail: also hides the face of a full block next to a round one, which the curve leaves partly open; compare the outlines if that shows.
        if(face.depth==0) {
            BlockState neighbour=level.getBlockState(query.set(face.x+face.nx,face.y+face.ny,face.z+face.nz));
            if(neighbour.getBlock() instanceof MirrorBlock) {
                MirrorBlock.Form form=neighbour.getValue(MirrorBlock.FORM);
                if(form.shape().cube()||form!=MirrorBlock.Form.SLAB&&!form.shape().flat())return MirrorEngine.MODE_NONE;
            }
        }
        return MirrorBlock.renderMode(state);
    }

    /**
     * Fills what is left empty in a Players Only reflection once its players are drawn. The End sky is the
     * game's own and is already there.
     */
    void backdrop(RenderTarget into,MirrorEngine.Capture<RenderTarget> capture) {
        if(capture.backdrop.kind==MirrorBackdrop.Kind.END)return;
        float[] viewProjection=capture.self.viewProjection();
        //? if >=1.21.5 {
        /*int oldDraw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        if(compositeFramebuffer==0)compositeFramebuffer=glGenFramebuffers();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,compositeFramebuffer);
        try {
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,glId(into.getColorTexture()),0);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,glId(into.getDepthTexture()),0);
            if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Mirror backdrop framebuffer incomplete");
            glViewport(0,0,into.width,into.height);
            backdrop.draw(capture.backdrop,MirrorConfig.pictures(),blockAtlas(),blockSprite(capture.backdrop),viewProjection,capture.frame,into.width,into.height);
        } finally {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER,oldDraw);
        }
        *///?} else {
        into.bindWrite(true);
        backdrop.draw(capture.backdrop,MirrorConfig.pictures(),blockAtlas(),blockSprite(capture.backdrop),viewProjection,capture.frame,into.width,into.height);
        //?}
    }

    @Override public void composite(RenderTarget into,RenderTarget source,float[] viewProjection,double[] eye,float[] sourceViewProjection,double[] sourceEye,List<MirrorSurface> faces) {
        RenderTarget target=into==null?mainTarget():into;
        //? if >=1.21.5 {
        /*// These versions have no bindable target, so the composite draws through a framebuffer of its own.
        int oldDraw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        if(compositeFramebuffer==0)compositeFramebuffer=glGenFramebuffers();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,compositeFramebuffer);
        try {
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,glId(target.getColorTexture()),0);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,glId(target.getDepthTexture()),0);
            if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Mirror composite framebuffer incomplete");
            glViewport(0,0,target.width,target.height);
            composite.drawMergedBatch(colorTexture(source),viewProjection,eye[0],eye[1],eye[2],sourceViewProjection,sourceEye[0],sourceEye[1],sourceEye[2],faces);
        } finally {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER,oldDraw);
        }
        *///?} else {
        // World rendering can leave another target's viewport set, which would misplace these draws.
        target.bindWrite(true);
        composite.drawMergedBatch(colorTexture(source),viewProjection,eye[0],eye[1],eye[2],sourceViewProjection,sourceEye[0],sourceEye[1],sourceEye[2],faces);
        //?}
    }

    /**
     * Draws the frames of every framed mirror the last search found, whether it reflects just now or not. A frame
     * runs around all the surfaces on one plane that are framed with the same block.
     */
    @Override public void decorate(RenderTarget into,float[] viewProjection,double[] eye) {
        if(frames.isEmpty())return;
        if(framedPlanes==null)framedPlanes=framedPlanes();
        if(framedPlanes.isEmpty())return;
        RenderTarget target=into==null?mainTarget():into;
        //? if >=1.21.5 {
        /*int oldDraw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        if(compositeFramebuffer==0)compositeFramebuffer=glGenFramebuffers();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,compositeFramebuffer);
        try {
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,glId(target.getColorTexture()),0);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,glId(target.getDepthTexture()),0);
            glViewport(0,0,target.width,target.height);
            drawFrames(viewProjection,eye);
        } finally {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER,oldDraw);
        }
        *///?} else {
        target.bindWrite(true);
        drawFrames(viewProjection,eye);
        //?}
    }

    /** The framed surfaces of the last search, grouped by plane and frame block. */
    private List<Object[]> framedPlanes() {
        java.util.Map<String,Object[]> planes=new java.util.LinkedHashMap<String,Object[]>();
        for(MirrorSurface face:engine.surfaces()) {
            String id=frames.get(BlockPos.asLong(face.x,face.y,face.z));
            // The stepped sides of an outlined block have no edge of their own to frame.
            if(id==null||face.flattened)continue;
            Object[] plane=planes.get(face.planeKey()+"/"+id);
            if(plane==null)planes.put(face.planeKey()+"/"+id,plane=new Object[]{id,new ArrayList<MirrorSurface>()});
            @SuppressWarnings("unchecked") List<MirrorSurface> faces=(List<MirrorSurface>)plane[1];
            faces.add(face);
        }
        return new ArrayList<Object[]>(planes.values());
    }

    private void drawFrames(float[] viewProjection,double[] eye) {
        Minecraft mc=Minecraft.getInstance();
        int atlas=blockAtlas();
        for(Object[] plane:framedPlanes) {
            @SuppressWarnings("unchecked") List<MirrorSurface> faces=(List<MirrorSurface>)plane[1];
            MirrorSurface first=faces.get(0);
            float[] sprite=sprite(mc,(String)plane[0]);
            if(sprite==null||!first.frontFacing(eye[0],eye[1],eye[2],0))continue;
            // The frame is as bright as the air in front of the mirror; it is not lit per pixel like a block.
            float light=.35f+.65f*mc.level.getMaxLocalRawBrightness(query.set(first.x+first.nx,first.y+first.ny,first.z+first.nz))/15f;
            frameDrawer.draw(atlas,sprite,light,viewProjection,eye[0],eye[1],eye[2],faces);
        }
    }

    /** OpenGL name of the texture that holds every block's texture. */
    private static int blockAtlas() {
        Minecraft mc=Minecraft.getInstance();
        //? if >=1.21.5 {
        /*return glId(mc.getTextureManager().getTexture(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).getTexture());
        *///?} else {
        return mc.getTextureManager().getTexture(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).getId();
        //?}
    }

    /** Where the texture of a block backdrop sits in the atlas, or null for another kind or an unknown block. */
    private float[] blockSprite(MirrorBackdrop backdrop) {
        return backdrop.kind==MirrorBackdrop.Kind.BLOCK?sprite(Minecraft.getInstance(),backdrop.block):null;
    }

    /** Where the texture of a frame's block sits in the block atlas: u0, v0, u1, v1; null for a block the game does not know. */
    public static float[] frameSprite(String id) { return INSTANCE.sprite(Minecraft.getInstance(),id); }

    private float[] sprite(Minecraft mc,String id) {
        if(sprites.containsKey(id))return sprites.get(id);
        float[] found=null;
        net.minecraft.world.level.block.Block block=com.malidev.simplymirrorsplus.block.MirrorContent.block(id);
        if(block!=null) {
            //? if >=26.1 {
            /*net.minecraft.client.renderer.texture.TextureAtlasSprite icon=mc.getModelManager().getBlockStateModelSet().getParticleMaterial(block.defaultBlockState()).sprite();
            *///?} else {
            net.minecraft.client.renderer.texture.TextureAtlasSprite icon=mc.getBlockRenderer().getBlockModelShaper().getParticleIcon(block.defaultBlockState());
            //?}
            found=new float[]{icon.getU0(),icon.getV0(),icon.getU1(),icon.getV1()};
        }
        sprites.put(id,found);
        return found;
    }

    static RenderTarget mainTarget() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gameRenderer.mainRenderTarget();
        *///?} else {
        return Minecraft.getInstance().getMainRenderTarget();
        //?}
    }

    //? if >=1.19.3 {
    static float[] array(org.joml.Matrix4fc matrix) { return matrix.get(new float[16]); }
    static org.joml.Matrix4f matrix(float[] values) { return new org.joml.Matrix4f().set(values); }
    //?} else {
    /*static float[] array(com.mojang.math.Matrix4f matrix) {
        java.nio.FloatBuffer buffer=org.lwjgl.BufferUtils.createFloatBuffer(16);
        matrix.store(buffer);
        float[] values=new float[16];
        buffer.get(values);
        return values;
    }

    /^* Mojang's matrix has no setter for a whole array, and its fields are named row first. ^/
    static com.mojang.math.Matrix4f matrix(float[] a) {
        com.mojang.math.Matrix4f result=new com.mojang.math.Matrix4f();
        //? if >=1.15 {
        com.malidev.simplymirrorsplus.client.mixin.MatrixAccessor fields=(com.malidev.simplymirrorsplus.client.mixin.MatrixAccessor)(Object)result;
        fields.simplymirrorsplus$m00(a[0]);fields.simplymirrorsplus$m01(a[4]);fields.simplymirrorsplus$m02(a[8]);fields.simplymirrorsplus$m03(a[12]);
        fields.simplymirrorsplus$m10(a[1]);fields.simplymirrorsplus$m11(a[5]);fields.simplymirrorsplus$m12(a[9]);fields.simplymirrorsplus$m13(a[13]);
        fields.simplymirrorsplus$m20(a[2]);fields.simplymirrorsplus$m21(a[6]);fields.simplymirrorsplus$m22(a[10]);fields.simplymirrorsplus$m23(a[14]);
        fields.simplymirrorsplus$m30(a[3]);fields.simplymirrorsplus$m31(a[7]);fields.simplymirrorsplus$m32(a[11]);fields.simplymirrorsplus$m33(a[15]);
        //?} else {
        /^result.load(java.nio.FloatBuffer.wrap(a));
        ^///?}
        return result;
    }
    *///?}
}

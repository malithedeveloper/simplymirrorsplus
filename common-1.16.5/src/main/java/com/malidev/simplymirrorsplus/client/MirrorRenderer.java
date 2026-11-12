package com.malidev.simplymirrorsplus.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.malidev.simplymirrorsplus.block.MirrorBlock;
import com.malidev.simplymirrorsplus.block.MirrorBlockEntity;
import com.malidev.simplymirrorsplus.client.mixin.CameraAccessor;
import com.malidev.simplymirrorsplus.client.mixin.LevelRendererAccessor;
import com.malidev.simplymirrorsplus.math.MirrorSurface;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Matrix4f;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL33.*;

/** Minecraft 1.21.4 side of the mirror pipeline; {@link MirrorEngine} decides what to render. */
public final class MirrorRenderer implements MirrorEngine.Platform<RenderTarget> {
    public static RenderTarget redirect;
    private static final MirrorRenderer PLATFORM=new MirrorRenderer();
    private static final MirrorEngine<RenderTarget> ENGINE=new MirrorEngine<RenderTarget>(PLATFORM);
    private static final GlMirrorComposite COMPOSITE=new GlMirrorComposite();
    private LightTexture light;
    private float partial;
    private long deadline;
    private GameRenderer game;

    private MirrorRenderer() { }

    public static void render(PoseStack mainPose,float partial,long deadline,Camera camera,GameRenderer game,LightTexture light,Matrix4f mainProjection) {
        Matrix4f mainView=mainPose.last().pose();
        if(redirect!=null)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        PLATFORM.light=light;
        PLATFORM.partial=partial;
        PLATFORM.deadline=deadline;
        PLATFORM.game=game;
        RenderTarget main=mc.getMainRenderTarget();
        // The last Fabulous post pass overwrites the main depth buffer, which would reject every mirror fragment.
        if(((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$transparencyChain()!=null) {
            main.copyDepthFrom(mc.levelRenderer.getTranslucentTarget());
            main.bindWrite(true);
        }
        Vec3 eye=camera.getPosition();
        MirrorEngine.Pass root=new MirrorEngine.Pass(camera,new double[]{eye.x,eye.y,eye.z},array(mainView),array(mainProjection),0);
        root.target=main;
        try {
            ENGINE.render(root,main.width,main.height,mc.level,System.nanoTime());
        } finally {
            redirect=null;
            main.bindWrite(true);
            game.resetProjectionMatrix(mainProjection);
        }
    }

    @Override public RenderTarget createTarget(int width,int height) {
        RenderTarget target=new RenderTarget(width,height,true,Minecraft.ON_OSX);
        // Reprojected and downscaled images do not land on texel centres.
        target.setFilterMode(GL_LINEAR);
        return target;
    }

    @Override public void destroyTarget(RenderTarget target) { target.destroyBuffers(); }
    @Override public int width(RenderTarget target) { return target.width; }
    @Override public int height(RenderTarget target) { return target.height; }
    @Override public boolean shadersActive() { return ShaderReflection.enabled(); }
    @Override public boolean depthZeroToOne() { return false; }

    @Override public List<MirrorSurface> findSurfaces(double x,double y,double z) {
        Level level=Minecraft.getInstance().level;
        List<MirrorSurface> surfaces=new ArrayList<>();
        int cx=(int)Math.floor(x/16),cz=(int)Math.floor(z/16);
        for(int chunkX=cx-2;chunkX<=cx+2;chunkX++)for(int chunkZ=cz-2;chunkZ<=cz+2;chunkZ++) {
            if(!level.hasChunk(chunkX,chunkZ))continue;
            for(BlockEntity entity:level.getChunk(chunkX,chunkZ).getBlockEntities().values()) {
                if(!(entity instanceof MirrorBlockEntity))continue;
                BlockPos pos=entity.getBlockPos();
                for(MirrorSurface face:MirrorSurface.faces(pos.getX(),pos.getY(),pos.getZ()))surfaces.add(face);
            }
        }
        return surfaces;
    }

    @Override public int faceMode(MirrorSurface face) {
        Level level=Minecraft.getInstance().level;
        BlockState state=level.getBlockState(new BlockPos(face.x,face.y,face.z));
        if(!(state.getBlock() instanceof MirrorBlock))return MirrorEngine.MODE_NONE;
        if(level.getBlockState(new BlockPos(face.x+face.nx,face.y+face.ny,face.z+face.nz)).getBlock() instanceof MirrorBlock)return MirrorEngine.MODE_NONE;
        return MirrorBlock.renderMode(state);
    }

    @Override public void capture(MirrorEngine.Capture<RenderTarget> capture) {
        Minecraft mc=Minecraft.getInstance();
        MirrorEngine.Pass parent=capture.parent,self=capture.self;
        MirrorSurface surface=capture.surface;
        RenderTarget into=capture.into;
        Vec3 eye=new Vec3(self.eye[0],self.eye[1],self.eye[2]);
        Matrix4f view=matrix(self.view),projection=matrix(self.projection);
        Camera parentCamera=(Camera)parent.camera;
        Camera virtual=new Camera();
        virtual.setup(mc.level,mc.getCameraEntity(),true,false,partial);
        CameraAccessor accessor=(CameraAccessor)(Object)virtual;
        accessor.simplymirrorsplus$position(eye);
        accessor.simplymirrorsplus$rotation(surface.ny!=0?parentCamera.getYRot():surface.nz!=0?180-parentCamera.getYRot():-parentCamera.getYRot(),surface.ny!=0?-parentCamera.getXRot():parentCamera.getXRot());
        self.camera=virtual;
        RenderTarget parentTarget=(RenderTarget)parent.target;
        int oldFront=glGetInteger(GL_FRONT_FACE);
        boolean oldSmartCull=mc.smartCull;
        boolean oldDepthMask=glGetBoolean(GL_DEPTH_WRITEMASK);
        // The virtual camera is behind the wall; portal clipping replaces ordinary wall occlusion.
        mc.smartCull=false;
        redirect=into;
        try {
            // Iris final compositing leaves depth writes disabled; clears obey that mask.
            RenderSystem.depthMask(true);
            if(capture.playersOnly)into.setClearColor(0,0,0,1);else into.setClearColor(0.615f,0.718f,0.792f,1);
            into.clear(Minecraft.ON_OSX);
            into.bindWrite(true);
            boolean shaderPass=capture.shaders&&!capture.playersOnly;
            if(shaderPass)ShaderReflection.begin(capture.perspective,self.view,surface.nx,surface.ny,surface.nz,capture.offsetX(),capture.offsetY(),capture.offsetZ(),virtual,parent.view,parent.eye,self.eye,capture.prior);
            try {
                game.resetProjectionMatrix(projection);
                glFrontFace(self.depth%2==1?GL_CW:GL_CCW);
                if(capture.playersOnly) {
                    renderPlayersOnEndSky(virtual,eye,view);
                } else {
                    // Fabulous post passes read fixed window-sized targets, so reflections use the Fancy path.
                    LevelRendererAccessor levelAccess=(LevelRendererAccessor)mc.levelRenderer;
                    PostChain transparency=levelAccess.simplymirrorsplus$transparencyChain();
                    levelAccess.simplymirrorsplus$setTransparencyChain(null);
                    try {
                        mc.levelRenderer.renderLevel(poseOf(view),partial,deadline,false,virtual,game,light,projection);
                    } finally {
                        levelAccess.simplymirrorsplus$setTransparencyChain(transparency);
                    }
                }
            } finally {
                if(shaderPass)ShaderReflection.end();
            }
            if(capture.nested!=null)capture.nested.run();
        } finally {
            RenderSystem.depthMask(oldDepthMask);
            mc.smartCull=oldSmartCull;
            glFrontFace(oldFront);
            redirect=parent.depth==0?null:parentTarget;
            parentTarget.bindWrite(true);
            game.resetProjectionMatrix(matrix(parent.projection));
            mc.getEntityRenderDispatcher().prepare(mc.level,parentCamera,mc.crosshairPickEntity);
        }
    }

    @Override public void composite(RenderTarget into,RenderTarget source,float[] viewProjection,double[] eye,float[] sourceViewProjection,double[] sourceEye,List<MirrorSurface> faces) {
        // World rendering can leave another target's viewport set, which would misplace these draws.
        (into==null?Minecraft.getInstance().getMainRenderTarget():into).bindWrite(true);
        COMPOSITE.drawMergedBatch(source.getColorTextureId(),viewProjection,eye[0],eye[1],eye[2],sourceViewProjection,sourceEye[0],sourceEye[1],sourceEye[2],faces);
    }

    /** VRChat-style pass: skips world rendering entirely, so it costs a sky draw plus the player models. */
    private void renderPlayersOnEndSky(Camera camera,Vec3 eye,Matrix4f view) {
        Minecraft mc=Minecraft.getInstance();
        EntityRenderDispatcher dispatcher=mc.getEntityRenderDispatcher();
        dispatcher.prepare(mc.level,camera,mc.crosshairPickEntity);
        FogRenderer.setupNoFog();
        PoseStack pose=poseOf(view);
        ((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$renderEndSky(pose);
        // Shadows are projected onto world blocks, which are not drawn here.
        dispatcher.setRenderShadow(false);
        try {
            Lighting.setupLevel(view);
            net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();
            for(AbstractClientPlayer player:mc.level.players()) {
                if(!player.shouldRender(eye.x,eye.y,eye.z))continue;

                dispatcher.render(player,Mth.lerp(partial,player.xOld,player.getX())-eye.x,Mth.lerp(partial,player.yOld,player.getY())-eye.y,Mth.lerp(partial,player.zOld,player.getZ())-eye.z,Mth.lerp(partial,player.yRotO,player.yRot),partial,pose,buffers,dispatcher.getPackedLightCoords(player,partial));
            }
            buffers.endBatch();
        } finally {
            dispatcher.setRenderShadow(true);
        }
    }

    private static PoseStack poseOf(Matrix4f view) {
        PoseStack pose=new PoseStack();
        pose.last().pose().multiply(view);
        pose.last().normal().load(new com.mojang.math.Matrix3f(view));
        return pose;
    }

    public static void release() {
        ENGINE.release();
        COMPOSITE.close();
        redirect=null;
    }

    private static float[] array(Matrix4f matrix) { FloatBuffer buffer=BufferUtils.createFloatBuffer(16);matrix.store(buffer);float[] result=new float[16];for(int i=0;i<16;i++)result[i]=buffer.get(i);return result; }
    private static Matrix4f matrix(float[] a) {
        Matrix4f result=new Matrix4f();
        com.malidev.simplymirrorsplus.client.mixin.MatrixAccessor fields=(com.malidev.simplymirrorsplus.client.mixin.MatrixAccessor)(Object)result;
        fields.simplymirrorsplus$m00(a[0]);fields.simplymirrorsplus$m01(a[4]);fields.simplymirrorsplus$m02(a[8]);fields.simplymirrorsplus$m03(a[12]);
        fields.simplymirrorsplus$m10(a[1]);fields.simplymirrorsplus$m11(a[5]);fields.simplymirrorsplus$m12(a[9]);fields.simplymirrorsplus$m13(a[13]);
        fields.simplymirrorsplus$m20(a[2]);fields.simplymirrorsplus$m21(a[6]);fields.simplymirrorsplus$m22(a[10]);fields.simplymirrorsplus$m23(a[14]);
        fields.simplymirrorsplus$m30(a[3]);fields.simplymirrorsplus$m31(a[7]);fields.simplymirrorsplus$m32(a[11]);fields.simplymirrorsplus$m33(a[15]);
        return result;
    }
}

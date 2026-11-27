package com.malidev.simplymirrorsplus.client;

import com.malidev.simplymirrorsplus.math.MirrorFrameMesh;
import com.malidev.simplymirrorsplus.math.MirrorSurface;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL33.*;

/** Draws mirror frames with the texture of the block they are made of, straight from the game's block atlas. */
public final class GlMirrorFrames {
    private static final String VERTEX="#version 150\nin vec4 Clip;in vec2 Uv;out vec2 vUv;void main(){vUv=Uv;gl_Position=Clip;}";
    private static final String FRAGMENT="#version 150\nuniform sampler2D Atlas;uniform float Light;in vec2 vUv;out vec4 FragColor;void main(){vec4 c=texture(Atlas,vUv);if(c.a<.1)discard;FragColor=vec4(c.rgb*Light,1.);}";
    private static final int[] TRIANGLES={0,1,2,0,2,3};
    private final Map<String,MirrorFrameMesh> meshes=new HashMap<String,MirrorFrameMesh>();
    private FloatBuffer vertices=BufferUtils.createFloatBuffer(6*6*64);
    private int program,vao,vbo,sampler;
    public boolean reversedDepth;

    /**
     * @param surfaces coplanar surfaces sharing one frame
     * @param sprite the frame block's place in the atlas: u0, v0, u1, v1
     * @param light 0 to 1, how bright the frame is where the mirror stands
     */
    public void draw(int atlas,float[] sprite,float light,float[] viewProjection,double eyeX,double eyeY,double eyeZ,List<MirrorSurface> surfaces) {
        MirrorFrameMesh mesh=mesh(surfaces);
        if(mesh.count==0||atlas==0)return;
        if(program==0)initialize();
        int needed=mesh.count*36;
        if(vertices.capacity()<needed)vertices=BufferUtils.createFloatBuffer(needed*2);
        vertices.clear();
        float[] q=mesh.quads;
        for(int quad=0;quad<mesh.count;quad++) {
            int at=quad*16;
            for(int index:TRIANGLES) {
                double x=q[at+index*3]-eyeX,y=q[at+index*3+1]-eyeY,z=q[at+index*3+2]-eyeZ;
                double cw=viewProjection[3]*x+viewProjection[7]*y+viewProjection[11]*z+viewProjection[15];
                double cz=viewProjection[2]*x+viewProjection[6]*y+viewProjection[10]*z+viewProjection[14];
                vertices.put((float)(viewProjection[0]*x+viewProjection[4]*y+viewProjection[8]*z+viewProjection[12]))
                    .put((float)(viewProjection[1]*x+viewProjection[5]*y+viewProjection[9]*z+viewProjection[13]))
                    .put((float)(reversedDepth?cz+cw*.00002:cz-cw*.00002)).put((float)cw);
                // Corner 0 and 3 are the run's start, 0 and 1 its lower edge.
                float tu=index==0||index==3?q[at+12]:q[at+14],tv=index<2?q[at+13]:q[at+15];
                vertices.put(sprite[0]+tu/16f*(sprite[2]-sprite[0])).put(sprite[3]-tv/16f*(sprite[3]-sprite[1]));
            }
        }
        vertices.flip();
        int oldProgram=glGetInteger(GL_CURRENT_PROGRAM),oldVao=glGetInteger(GL_VERTEX_ARRAY_BINDING),oldBuffer=glGetInteger(GL_ARRAY_BUFFER_BINDING);
        int oldActive=glGetInteger(GL_ACTIVE_TEXTURE);glActiveTexture(GL_TEXTURE0);int oldTexture=glGetInteger(GL_TEXTURE_BINDING_2D);
        boolean samplers=org.lwjgl.opengl.GL.getCapabilities().OpenGL33||org.lwjgl.opengl.GL.getCapabilities().GL_ARB_sampler_objects;
        int oldSampler=samplers?glGetInteger(GL_SAMPLER_BINDING):0;
        // Newer versions leave the atlas texture itself on smooth filtering and choose the filter per draw.
        // A sampler of our own keeps the frame as crisp as the block it is made of.
        if(samplers) { if(sampler==0)sampler=nearest();glBindSampler(0,sampler); }
        boolean depth=glIsEnabled(GL_DEPTH_TEST),cull=glIsEnabled(GL_CULL_FACE),blend=glIsEnabled(GL_BLEND),scissor=glIsEnabled(GL_SCISSOR_TEST);
        boolean depthMask=glGetBoolean(GL_DEPTH_WRITEMASK);int depthFunc=glGetInteger(GL_DEPTH_FUNC);
        try {
            glEnable(GL_DEPTH_TEST);glDepthFunc(reversedDepth?GL_GEQUAL:GL_LEQUAL);glDepthMask(false);glDisable(GL_CULL_FACE);glDisable(GL_BLEND);glDisable(GL_SCISSOR_TEST);
            glUseProgram(program);
            glUniform1i(glGetUniformLocation(program,"Atlas"),0);
            glUniform1f(glGetUniformLocation(program,"Light"),light);
            glBindTexture(GL_TEXTURE_2D,atlas);
            glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glBufferData(GL_ARRAY_BUFFER,vertices,GL_STREAM_DRAW);
            glDrawArrays(GL_TRIANGLES,0,mesh.count*6);
        } finally {
            glBindBuffer(GL_ARRAY_BUFFER,oldBuffer);glBindVertexArray(oldVao);glUseProgram(oldProgram);
            glBindTexture(GL_TEXTURE_2D,oldTexture);if(samplers)glBindSampler(0,oldSampler);glActiveTexture(oldActive);glDepthMask(depthMask);glDepthFunc(depthFunc);
            set(GL_DEPTH_TEST,depth);set(GL_CULL_FACE,cull);set(GL_BLEND,blend);set(GL_SCISSOR_TEST,scissor);
        }
    }

    public void close() {
        meshes.clear();
        if(program!=0) { glDeleteProgram(program);glDeleteVertexArrays(vao);glDeleteBuffers(vbo); }
        if(sampler!=0)glDeleteSamplers(sampler);
        program=vao=vbo=sampler=0;
    }

    /** A mesh is kept until the mirror it belongs to changes: its key lists every surface. */
    private MirrorFrameMesh mesh(List<MirrorSurface> surfaces) {
        long hash=surfaces.size();
        for(MirrorSurface surface:surfaces)hash=hash*31+(surface.x*73856093L^surface.y*19349663L^surface.z*83492791L)+Double.doubleToLongBits(surface.a0+surface.b0*3+surface.a1*5+surface.b1*7);
        String key=surfaces.get(0).planeKey()+"/"+hash;
        MirrorFrameMesh mesh=meshes.get(key);
        if(mesh==null) {
            if(meshes.size()>128)meshes.clear();
            mesh=MirrorFrameMesh.build(surfaces);
            meshes.put(key,mesh);
        }
        return mesh;
    }

    private void initialize() {
        int vertex=shader(GL_VERTEX_SHADER,VERTEX),fragment=shader(GL_FRAGMENT_SHADER,FRAGMENT);
        program=glCreateProgram();
        glAttachShader(program,vertex);glAttachShader(program,fragment);
        glBindAttribLocation(program,0,"Clip");glBindAttribLocation(program,1,"Uv");
        glLinkProgram(program);
        glDeleteShader(vertex);glDeleteShader(fragment);
        if(glGetProgrami(program,GL_LINK_STATUS)==GL_FALSE)throw new IllegalStateException("Mirror frame shader: "+glGetProgramInfoLog(program));
        int oldVao=glGetInteger(GL_VERTEX_ARRAY_BINDING),oldBuffer=glGetInteger(GL_ARRAY_BUFFER_BINDING);
        vao=glGenVertexArrays();vbo=glGenBuffers();
        glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);
        glVertexAttribPointer(0,4,GL_FLOAT,false,24,0L);glEnableVertexAttribArray(0);
        glVertexAttribPointer(1,2,GL_FLOAT,false,24,16L);glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER,oldBuffer);glBindVertexArray(oldVao);
    }

    private static int nearest() {
        int sampler=glGenSamplers();
        glSamplerParameteri(sampler,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glSamplerParameteri(sampler,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        return sampler;
    }

    private static int shader(int type,String source) {
        int shader=glCreateShader(type);
        glShaderSource(shader,source);glCompileShader(shader);
        if(glGetShaderi(shader,GL_COMPILE_STATUS)==GL_FALSE)throw new IllegalStateException("Mirror frame shader: "+glGetShaderInfoLog(shader));
        return shader;
    }

    private static void set(int state,boolean enabled) { if(enabled)glEnable(state);else glDisable(state); }
}

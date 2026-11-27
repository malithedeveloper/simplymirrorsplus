package com.malidev.simplymirrorsplus.client;

import static org.lwjgl.opengl.GL33.*;

/** Puts the settings screen's image, and the live mirror preview under it, into the bound framebuffer. */
public final class GlMirrorUi {
    private static final String VERTEX="#version 150\nin vec4 Vertex;out vec2 vUv;void main(){vUv=Vertex.zw;gl_Position=vec4(Vertex.xy,0.,1.);}";
    private static final String FRAGMENT="#version 150\nuniform sampler2D Image;uniform float Opaque;in vec2 vUv;out vec4 FragColor;void main(){vec4 c=texture(Image,vUv);FragColor=vec4(c.rgb,max(c.a,Opaque));}";
    private final float[] quad=new float[24];
    private int program,vao,vbo,texture,textureWidth,textureHeight;

    /** @param pixels premultiplied ARGB, top row first */
    public void upload(int[] pixels,int width,int height) {
        int oldTexture=glGetInteger(GL_TEXTURE_BINDING_2D),oldAlignment=glGetInteger(GL_UNPACK_ALIGNMENT),oldRow=glGetInteger(GL_UNPACK_ROW_LENGTH);
        int oldSkipRows=glGetInteger(GL_UNPACK_SKIP_ROWS),oldSkipPixels=glGetInteger(GL_UNPACK_SKIP_PIXELS);
        if(texture==0)texture=glGenTextures();
        try {
            glBindTexture(GL_TEXTURE_2D,texture);
            glPixelStorei(GL_UNPACK_ALIGNMENT,4);glPixelStorei(GL_UNPACK_ROW_LENGTH,0);glPixelStorei(GL_UNPACK_SKIP_ROWS,0);glPixelStorei(GL_UNPACK_SKIP_PIXELS,0);
            if(width!=textureWidth||height!=textureHeight) {
                glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAX_LEVEL,0);
                // An ARGB int is the bytes B, G, R, A in memory, which this format reads without a copy.
                glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,width,height,0,GL_BGRA,GL_UNSIGNED_INT_8_8_8_8_REV,pixels);
                textureWidth=width;textureHeight=height;
            } else {
                glTexSubImage2D(GL_TEXTURE_2D,0,0,0,width,height,GL_BGRA,GL_UNSIGNED_INT_8_8_8_8_REV,pixels);
            }
        } finally {
            glPixelStorei(GL_UNPACK_ALIGNMENT,oldAlignment);glPixelStorei(GL_UNPACK_ROW_LENGTH,oldRow);
            glPixelStorei(GL_UNPACK_SKIP_ROWS,oldSkipRows);glPixelStorei(GL_UNPACK_SKIP_PIXELS,oldSkipPixels);
            glBindTexture(GL_TEXTURE_2D,oldTexture);
        }
    }

    /**
     * @param area where the mirror of the preview goes: x, y, width, height in pixels from the top left; width 0 draws none
     * @param previewTexture the reflection image, 0 when there is none yet
     * @param frame the mirror's bounds inside that image, in clip space
     */
    public void draw(int width,int height,int[] area,int previewTexture,float[] frame) {
        if(texture==0)return;
        if(program==0)initialize();
        int oldProgram=glGetInteger(GL_CURRENT_PROGRAM),oldVao=glGetInteger(GL_VERTEX_ARRAY_BINDING),oldBuffer=glGetInteger(GL_ARRAY_BUFFER_BINDING);
        int oldActive=glGetInteger(GL_ACTIVE_TEXTURE);glActiveTexture(GL_TEXTURE0);int oldTexture=glGetInteger(GL_TEXTURE_BINDING_2D);
        boolean samplers=org.lwjgl.opengl.GL.getCapabilities().OpenGL33||org.lwjgl.opengl.GL.getCapabilities().GL_ARB_sampler_objects;
        int oldSampler=samplers?glGetInteger(GL_SAMPLER_BINDING):0;
        if(samplers)glBindSampler(0,0);
        boolean depth=glIsEnabled(GL_DEPTH_TEST),cull=glIsEnabled(GL_CULL_FACE),blend=glIsEnabled(GL_BLEND),scissor=glIsEnabled(GL_SCISSOR_TEST);
        int srcRgb=glGetInteger(GL_BLEND_SRC_RGB),dstRgb=glGetInteger(GL_BLEND_DST_RGB),srcAlpha=glGetInteger(GL_BLEND_SRC_ALPHA),dstAlpha=glGetInteger(GL_BLEND_DST_ALPHA);
        try {
            glDisable(GL_DEPTH_TEST);glDisable(GL_CULL_FACE);glDisable(GL_SCISSOR_TEST);
            glUseProgram(program);glUniform1i(glGetUniformLocation(program,"Image"),0);
            glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);
            if(previewTexture!=0&&area[2]>0&&area[3]>0) {
                float x=area[0],y=area[1],w=area[2],h=area[3];
                glDisable(GL_BLEND);glUniform1f(glGetUniformLocation(program,"Opaque"),1);
                glBindTexture(GL_TEXTURE_2D,previewTexture);
                quad(x/width*2-1,1-(y+h)/height*2,(x+w)/width*2-1,1-y/height*2,frame[0]*.5f+.5f,frame[1]*.5f+.5f,frame[2]*.5f+.5f,frame[3]*.5f+.5f);
            }
            // The screen image is premultiplied.
            glEnable(GL_BLEND);glBlendFuncSeparate(GL_ONE,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
            glUniform1f(glGetUniformLocation(program,"Opaque"),0);
            glBindTexture(GL_TEXTURE_2D,texture);
            quad(-1,-1,1,1,0,1,1,0);
        } finally {
            glBlendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);
            glBindBuffer(GL_ARRAY_BUFFER,oldBuffer);glBindVertexArray(oldVao);glUseProgram(oldProgram);
            glBindTexture(GL_TEXTURE_2D,oldTexture);if(samplers)glBindSampler(0,oldSampler);glActiveTexture(oldActive);
            set(GL_DEPTH_TEST,depth);set(GL_CULL_FACE,cull);set(GL_BLEND,blend);set(GL_SCISSOR_TEST,scissor);
        }
    }

    public void close() {
        if(texture!=0)glDeleteTextures(texture);
        if(program!=0) { glDeleteProgram(program);glDeleteVertexArrays(vao);glDeleteBuffers(vbo); }
        program=vao=vbo=texture=textureWidth=textureHeight=0;
    }

    /** Corners in clip space, then the texture coordinates of the bottom left and top right. */
    private void quad(float x0,float y0,float x1,float y1,float u0,float v0,float u1,float v1) {
        float[] q={x0,y0,u0,v0, x1,y0,u1,v0, x1,y1,u1,v1, x0,y0,u0,v0, x1,y1,u1,v1, x0,y1,u0,v1};
        System.arraycopy(q,0,quad,0,24);
        glBufferData(GL_ARRAY_BUFFER,quad,GL_STREAM_DRAW);
        glDrawArrays(GL_TRIANGLES,0,6);
    }

    private void initialize() {
        int vertex=shader(GL_VERTEX_SHADER,VERTEX),fragment=shader(GL_FRAGMENT_SHADER,FRAGMENT);
        program=glCreateProgram();
        glAttachShader(program,vertex);glAttachShader(program,fragment);
        glBindAttribLocation(program,0,"Vertex");
        glLinkProgram(program);
        glDeleteShader(vertex);glDeleteShader(fragment);
        if(glGetProgrami(program,GL_LINK_STATUS)==GL_FALSE)throw new IllegalStateException("Mirror screen shader: "+glGetProgramInfoLog(program));
        int oldVao=glGetInteger(GL_VERTEX_ARRAY_BINDING),oldBuffer=glGetInteger(GL_ARRAY_BUFFER_BINDING);
        vao=glGenVertexArrays();vbo=glGenBuffers();
        glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);
        glVertexAttribPointer(0,4,GL_FLOAT,false,16,0L);glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER,oldBuffer);glBindVertexArray(oldVao);
    }

    private static int shader(int type,String source) {
        int shader=glCreateShader(type);
        glShaderSource(shader,source);glCompileShader(shader);
        if(glGetShaderi(shader,GL_COMPILE_STATUS)==GL_FALSE)throw new IllegalStateException("Mirror screen shader: "+glGetShaderInfoLog(shader));
        return shader;
    }

    private static void set(int state,boolean enabled) { if(enabled)glEnable(state);else glDisable(state); }
}

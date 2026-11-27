package com.malidev.simplymirrorsplus.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL33.*;

/**
 * Fills what is behind the players in a Players Only reflection. It is drawn after them, at the far plane,
 * so it lands only on pixels nothing else covered and needs no hook into the game's own sky code.
 */
public final class GlMirrorBackdrop {
    private static final Logger LOGGER=Logger.getLogger("simplymirrorsplus");
    private static final String VERTEX="#version 150\nin vec2 Position;uniform float Far;out vec2 vNdc;void main(){vNdc=Position;gl_Position=vec4(Position,Far,1.);}";
    private static final String FRAGMENT="#version 150\n"
        +"uniform sampler2D Image;uniform vec4 Sprite;uniform mat4 Unproject;uniform int Kind,Motion,Radial;uniform vec3 Color,Color2;uniform vec2 Fit;uniform vec4 Frame;uniform float Angle,Time;in vec2 vNdc;out vec4 FragColor;\n"
        // The view has no translation, so the difference of two unprojected depths is the world direction of the pixel.
        +"vec3 direction(){vec4 a=Unproject*vec4(vNdc,-.5,1.),b=Unproject*vec4(vNdc,.5,1.);return normalize(b.xyz/b.w-a.xyz/a.w);}\n"
        // The game's own sky box: one tiled texture on the six faces of a cube around the camera.
        +"vec2 box(vec3 d){vec3 a=abs(d);return a.x>=a.y&&a.x>=a.z?d.zy/a.x:a.y>=a.z?d.xz/a.y:d.xy/a.z;}\n"
        +"vec3 hue(float h){return clamp(abs(fract(h+vec3(0.,2./3.,1./3.))*6.-3.)-1.,0.,1.);}\n"
        +"void main(){vec3 c;\n"
        // Pictures and gradients are laid out on the mirror, not on the image, which is larger and moves with the player.
        +"vec2 m=(vNdc-(Frame.xy+Frame.zw)*.5)/((Frame.zw-Frame.xy)*.5);\n"
        +"if(Kind==0)c=texture(Image,box(direction())*8.).rgb*Color;\n"
        // A room of one block: its texture repeats on the same cube, picked out of the block atlas.
        +"else if(Kind==1)c=texture(Image,mix(Sprite.xy,Sprite.zw,fract(box(direction())*4.+.5))).rgb*Color;\n"
        +"else if(Kind==2)c=Color;\n"
        +"else if(Kind==3){float a=Angle+(Motion==2?Time*6.2831853:0.);\n"
        +"float t=Radial==1?length(m*Fit)*.75:dot(m,vec2(cos(a),sin(a)))*.5+.5;\n"
        +"if(Motion==1)t=abs(fract(t*.5+Time)*2.-1.);else if(Radial==1&&Motion==2)t=abs(fract(t*.5-Time)*2.-1.);\n"
        +"else if(Motion==3)t=mix(t,1.-t,.5-.5*cos(Time*6.2831853));\n"
        +"c=Motion==4?hue(t*.5+Time):mix(Color,Color2,clamp(t,0.,1.));}\n"
        +"else if(Kind==4)c=texture(Image,vec2(m.x,-m.y)/Fit*.5+.5).rgb;\n"
        +"else{vec3 d=direction();c=texture(Image,vec2(atan(d.z,d.x)*.15915494+.5,acos(clamp(d.y,-1.,1.))*.31830989)).rgb;}\n"
        +"FragColor=vec4(c,1.);}";

    private static final class Picture {
        final int texture,width,height;
        Picture(int texture,int width,int height) { this.texture=texture;this.width=width;this.height=height; }
    }

    private final Map<String,Picture> pictures=new HashMap<String,Picture>();
    private int program,vao,vbo,sampler;
    public boolean reversedDepth;

    /**
     * Draws into the bound framebuffer, which must have the reflection's depth attached.
     * @param viewProjection the reflection's projection times its view, column-major
     * @param frame the mirror's bounds inside the image, in clip space
     * @param folder where custom pictures are looked up
     * @param atlas the game's block atlas texture, and {@code sprite} the place of the backdrop's block in it as
     *        u0, v0, u1, v1; null when the backdrop is not a block or the block is unknown
     */
    public void draw(MirrorBackdrop backdrop,java.nio.file.Path folder,int atlas,float[] sprite,float[] viewProjection,float[] frame,int width,int height) {
        if(program==0)initialize();
        MirrorBackdrop.Kind kind=backdrop.kind;
        boolean block=kind==MirrorBackdrop.Kind.BLOCK&&sprite!=null&&atlas!=0;
        Picture picture=picture(kind,backdrop.image,folder);
        // A picture that cannot be read falls back to the colour instead of leaving the mirror empty.
        if(picture==null&&!block&&kind!=MirrorBackdrop.Kind.COLOR&&kind!=MirrorBackdrop.Kind.GRADIENT)kind=MirrorBackdrop.Kind.COLOR;
        int oldProgram=glGetInteger(GL_CURRENT_PROGRAM),oldVao=glGetInteger(GL_VERTEX_ARRAY_BINDING),oldBuffer=glGetInteger(GL_ARRAY_BUFFER_BINDING);
        int oldActive=glGetInteger(GL_ACTIVE_TEXTURE);glActiveTexture(GL_TEXTURE0);int oldTexture=glGetInteger(GL_TEXTURE_BINDING_2D);
        boolean samplers=org.lwjgl.opengl.GL.getCapabilities().OpenGL33||org.lwjgl.opengl.GL.getCapabilities().GL_ARB_sampler_objects;
        int oldSampler=samplers?glGetInteger(GL_SAMPLER_BINDING):0;
        // The atlas may be set to smooth filtering and mipmaps; a block backdrop reads it as crisp as the block looks.
        if(samplers&&block&&sampler==0) { sampler=glGenSamplers();glSamplerParameteri(sampler,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glSamplerParameteri(sampler,GL_TEXTURE_MAG_FILTER,GL_NEAREST); }
        if(samplers)glBindSampler(0,block?sampler:0);
        boolean depth=glIsEnabled(GL_DEPTH_TEST),cull=glIsEnabled(GL_CULL_FACE),blend=glIsEnabled(GL_BLEND),scissor=glIsEnabled(GL_SCISSOR_TEST);
        boolean depthMask=glGetBoolean(GL_DEPTH_WRITEMASK);int depthFunc=glGetInteger(GL_DEPTH_FUNC);
        try {
            glEnable(GL_DEPTH_TEST);glDepthFunc(reversedDepth?GL_GEQUAL:GL_LEQUAL);glDepthMask(false);glDisable(GL_CULL_FACE);glDisable(GL_BLEND);glDisable(GL_SCISSOR_TEST);
            glUseProgram(program);
            glUniform1i(glGetUniformLocation(program,"Image"),0);
            glUniform1i(glGetUniformLocation(program,"Kind"),kind.ordinal());
            glUniform1i(glGetUniformLocation(program,"Motion"),backdrop.motion.ordinal());
            glUniform1i(glGetUniformLocation(program,"Radial"),backdrop.radial?1:0);
            glUniform1f(glGetUniformLocation(program,"Angle"),(float)Math.toRadians(backdrop.angle));
            // Whole cycles are dropped before the conversion so a long session keeps its precision.
            glUniform1f(glGetUniformLocation(program,"Time"),(float)(System.nanoTime()/6e10*backdrop.speed%1.0));
            // Reversed depth runs from 1 at the camera to 0 at the far plane.
            glUniform1f(glGetUniformLocation(program,"Far"),reversedDepth?0f:1f);
            glUniform4f(glGetUniformLocation(program,"Frame"),frame[0],frame[1],frame[2],frame[3]);
            glUniformMatrix4fv(glGetUniformLocation(program,"Unproject"),false,com.malidev.simplymirrorsplus.math.MirrorMatrices.inverse(viewProjection));
            float tint=kind==MirrorBackdrop.Kind.END?40/255f:.85f;
            if(block)glUniform4f(glGetUniformLocation(program,"Sprite"),sprite[0],sprite[1],sprite[2],sprite[3]);
            if(kind==MirrorBackdrop.Kind.COLOR||kind==MirrorBackdrop.Kind.GRADIENT)color("Color",backdrop.color);
            else glUniform3f(glGetUniformLocation(program,"Color"),tint,tint,tint);
            color("Color2",backdrop.color2);
            // The mirror's size on screen, so pictures keep their proportions and round gradients stay round.
            float mirror=Math.abs((frame[2]-frame[0])*width)/Math.max(1e-3f,Math.abs((frame[3]-frame[1])*height));
            float own=picture==null?1:(float)picture.width/picture.height;
            if(kind==MirrorBackdrop.Kind.GRADIENT)glUniform2f(glGetUniformLocation(program,"Fit"),mirror>1?mirror:1,mirror>1?1:1/mirror);
            // Cover the mirror without stretching: the longer side of the picture is cropped.
            else glUniform2f(glGetUniformLocation(program,"Fit"),mirror>own?1:own/mirror,mirror>own?mirror/own:1);
            if(block)glBindTexture(GL_TEXTURE_2D,atlas);
            else if(picture!=null)glBindTexture(GL_TEXTURE_2D,picture.texture);
            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES,0,3);
        } finally {
            glBindBuffer(GL_ARRAY_BUFFER,oldBuffer);glBindVertexArray(oldVao);glUseProgram(oldProgram);
            glBindTexture(GL_TEXTURE_2D,oldTexture);if(samplers)glBindSampler(0,oldSampler);glActiveTexture(oldActive);glDepthMask(depthMask);glDepthFunc(depthFunc);
            set(GL_DEPTH_TEST,depth);set(GL_CULL_FACE,cull);set(GL_BLEND,blend);set(GL_SCISSOR_TEST,scissor);
        }
    }

    private void color(String uniform,int rgb) {
        glUniform3f(glGetUniformLocation(program,uniform),(rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f);
    }

    /** Forgets custom pictures so edited files are read again. */
    public void reload() {
        for(Picture picture:pictures.values())if(picture!=null)glDeleteTextures(picture.texture);
        pictures.clear();
    }

    public void close() {
        reload();
        if(program!=0) { glDeleteProgram(program);glDeleteVertexArrays(vao);glDeleteBuffers(vbo); }
        if(sampler!=0)glDeleteSamplers(sampler);
        program=vao=vbo=sampler=0;
    }

    private Picture picture(MirrorBackdrop.Kind kind,String image,Path folder) {
        if(kind==MirrorBackdrop.Kind.COLOR||kind==MirrorBackdrop.Kind.GRADIENT||kind==MirrorBackdrop.Kind.BLOCK)return null;
        String key=kind==MirrorBackdrop.Kind.END?"end":"file:"+image;
        if(pictures.containsKey(key))return pictures.get(key);
        Picture loaded=null;
        try {
            if(kind==MirrorBackdrop.Kind.END)loaded=upload(game("assets/minecraft/textures/environment/end_sky.png"),true);
            else if(image!=null&&!image.isEmpty()&&folder!=null) {
                // Only a plain file name is accepted, so a config value cannot point outside the folder.
                Path file=folder.resolve(java.nio.file.Paths.get(image).getFileName().toString());
                if(Files.isRegularFile(file))try(InputStream in=Files.newInputStream(file)) { loaded=upload(ImageIO.read(in),false); }
            }
        } catch(IOException|RuntimeException exception) {
            LOGGER.log(Level.WARNING,"Could not load mirror background "+key,exception);
        }
        pictures.put(key,loaded);
        return loaded;
    }

    /** Reads a texture from the game's own jar; resource packs do not apply to it. */
    private static BufferedImage game(String path) throws IOException {
        for(ClassLoader loader:new ClassLoader[]{Thread.currentThread().getContextClassLoader(),GlMirrorBackdrop.class.getClassLoader()}) {
            if(loader==null)continue;
            try(InputStream in=loader.getResourceAsStream(path)) { if(in!=null)return ImageIO.read(in); }
        }
        return noise();
    }

    /** Stand-in when the game's texture cannot be reached: the same kind of speckle the End sky has. */
    private static BufferedImage noise() {
        BufferedImage image=new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB);
        java.util.Random random=new java.util.Random(7);
        for(int y=0;y<64;y++)for(int x=0;x<64;x++) { int v=90+random.nextInt(120);image.setRGB(x,y,v<<16|v<<8|v); }
        return image;
    }

    private static Picture upload(BufferedImage image,boolean tiled) {
        if(image==null)return null;
        int width=image.getWidth(),height=image.getHeight();
        ByteBuffer pixels=BufferUtils.createByteBuffer(width*height*4);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int argb=image.getRGB(x,y);
            pixels.put((byte)(argb>>16)).put((byte)(argb>>8)).put((byte)argb).put((byte)255);
        }
        pixels.flip();
        int oldTexture=glGetInteger(GL_TEXTURE_BINDING_2D),oldAlignment=glGetInteger(GL_UNPACK_ALIGNMENT),oldRow=glGetInteger(GL_UNPACK_ROW_LENGTH);
        int oldSkipRows=glGetInteger(GL_UNPACK_SKIP_ROWS),oldSkipPixels=glGetInteger(GL_UNPACK_SKIP_PIXELS);
        int texture=glGenTextures();
        try {
            glBindTexture(GL_TEXTURE_2D,texture);
            glPixelStorei(GL_UNPACK_ALIGNMENT,1);glPixelStorei(GL_UNPACK_ROW_LENGTH,0);glPixelStorei(GL_UNPACK_SKIP_ROWS,0);glPixelStorei(GL_UNPACK_SKIP_PIXELS,0);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,tiled?GL_NEAREST:GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,tiled?GL_NEAREST:GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,tiled?GL_REPEAT:GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,tiled?GL_REPEAT:GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAX_LEVEL,0);
            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,width,height,0,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
        } finally {
            glPixelStorei(GL_UNPACK_ALIGNMENT,oldAlignment);glPixelStorei(GL_UNPACK_ROW_LENGTH,oldRow);
            glPixelStorei(GL_UNPACK_SKIP_ROWS,oldSkipRows);glPixelStorei(GL_UNPACK_SKIP_PIXELS,oldSkipPixels);
            glBindTexture(GL_TEXTURE_2D,oldTexture);
        }
        return new Picture(texture,width,height);
    }

    private void initialize() {
        int vertex=shader(GL_VERTEX_SHADER,VERTEX),fragment=shader(GL_FRAGMENT_SHADER,FRAGMENT);
        program=glCreateProgram();
        glAttachShader(program,vertex);glAttachShader(program,fragment);
        glBindAttribLocation(program,0,"Position");
        glLinkProgram(program);
        glDeleteShader(vertex);glDeleteShader(fragment);
        if(glGetProgrami(program,GL_LINK_STATUS)==GL_FALSE)throw new IllegalStateException("Mirror backdrop shader: "+glGetProgramInfoLog(program));
        int oldVao=glGetInteger(GL_VERTEX_ARRAY_BINDING),oldBuffer=glGetInteger(GL_ARRAY_BUFFER_BINDING);
        vao=glGenVertexArrays();vbo=glGenBuffers();
        glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);
        // One triangle that covers the whole image.
        glBufferData(GL_ARRAY_BUFFER,new float[]{-1,-1,3,-1,-1,3},GL_STATIC_DRAW);
        glVertexAttribPointer(0,2,GL_FLOAT,false,8,0L);glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER,oldBuffer);glBindVertexArray(oldVao);
    }

    private static int shader(int type,String source) {
        int shader=glCreateShader(type);
        glShaderSource(shader,source);glCompileShader(shader);
        if(glGetShaderi(shader,GL_COMPILE_STATUS)==GL_FALSE)throw new IllegalStateException("Mirror backdrop shader: "+glGetShaderInfoLog(shader));
        return shader;
    }

    private static void set(int state,boolean enabled) { if(enabled)glEnable(state);else glDisable(state); }
}

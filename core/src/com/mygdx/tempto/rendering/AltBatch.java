package com.mygdx.tempto.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

import java.nio.Buffer;

/**An alternate implementation of {@link com.badlogic.gdx.graphics.g2d.Batch} to {@link com.badlogic.gdx.graphics.g2d.SpriteBatch}, for Tempto, since different part of the render pipeline call for different shaders/vertex forms.
 * {@link com.badlogic.gdx.graphics.g2d.SpriteBatch} has a ton of fields locked in private, so simply extending it wasn't an option.
 *
 * Additionally, restricts the number of draw methods by implementing with errors, since most won't be implemented in Tempto, so we can implement them case by case.*/
public abstract class AltBatch implements Batch {
    @Deprecated public static Mesh.VertexDataType defaultVertexDataType = Mesh.VertexDataType.VertexArray;

    protected Mesh mesh;
    /**How many data points per vertex. E.g., the depth map has 7: x, y, c, u, v, d, e (d & e being u & v but corresponding to the depth map location)*/
    protected int spriteSize;
    /**How many indices per sprite. 6 if each sprite is a quad, 3 if each is only a triangle*/
    protected int indicesPerSprite;

    protected float[] vertices;
    protected int idx = 0;
    protected Texture lastTexture = null;
    protected float invTexWidth = 0, invTexHeight = 0;

    protected boolean drawing = false;

    protected final Matrix4 transformMatrix = new Matrix4();
    protected final Matrix4 projectionMatrix = new Matrix4();
    protected final Matrix4 combinedMatrix = new Matrix4();

    protected boolean blendingDisabled = false;
    protected int blendSrcFunc = GL20.GL_SRC_ALPHA;
    protected int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;
    protected int blendSrcFuncAlpha = GL20.GL_SRC_ALPHA;
    protected int blendDstFuncAlpha = GL20.GL_ONE_MINUS_SRC_ALPHA;

    protected final ShaderProgram shader;
    protected ShaderProgram customShader = null;
    protected boolean ownsShader;

    protected final Color color = new Color(1, 1, 1, 1);
    protected float colorPacked = Color.WHITE_FLOAT_BITS;

    /** Number of render calls since the last {@link #begin()}. **/
    public int renderCalls = 0;

    /** Number of rendering calls, ever. Will not be reset unless set manually. **/
    public int totalRenderCalls = 0;

    /** The maximum number of sprites rendered in one batch so far. **/
    public int maxSpritesInBatch = 0;

    public AltBatch(int size, ShaderProgram defaultShader, Mesh mesh, int spriteSize) {this(size, defaultShader, mesh, spriteSize, true);}

    public static Mesh.VertexDataType getDefaultVertexDataType() {return (Gdx.gl30 != null) ? Mesh.VertexDataType.VertexBufferObjectWithVAO : defaultVertexDataType;}
    public AltBatch(int size, ShaderProgram defaultShader, Mesh mesh, int spriteSize, boolean indexQuadsToTriangles) {
        this.mesh = mesh;
        // 32767 is max vertex index, so 32767 / 4 vertices per sprite = 8191 sprites max.
        if (size > 8191) throw new IllegalArgumentException("Can't have more than 8191 sprites per batch: " + size);

        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        this.spriteSize = spriteSize;

        vertices = new float[size * spriteSize];

        short[] indices;
        if (indexQuadsToTriangles) { //Are quads, so map each quad to two triangles
            this.indicesPerSprite = 6;
            int len = size * this.indicesPerSprite;
            indices = new short[len];
            short j = 0;
            for (int i = 0; i < len; i += this.indicesPerSprite, j += 4) {
                indices[i] = j;
                indices[i + 1] = (short) (j + 1);
                indices[i + 2] = (short) (j + 2);
                indices[i + 3] = (short) (j + 2);
                indices[i + 4] = (short) (j + 3);
                indices[i + 5] = j;
            }
        } else { //Already are triangles
            this.indicesPerSprite = 3;
            int len = size * this.indicesPerSprite;
            indices = new short[len];
            for (int i = 0; i < len; i++) indices[i]= (short) i;
        }
        mesh.setIndices(indices);

        if (defaultShader == null) {
            shader = this.createDefaultShader();
            ownsShader = true;
        } else {
            shader = defaultShader;
            ownsShader = false;
        }
    }

    protected abstract ShaderProgram createDefaultShader();

    @Override
    public void begin () {
        if (drawing) throw new IllegalStateException("SpriteBatch.end must be called before begin.");
        renderCalls = 0;

        Gdx.gl.glDepthMask(false);
        if (customShader != null)
            customShader.bind();
        else
            shader.bind();
        setupMatrices();

        drawing = true;
    }

    @Override
    public void end () {
        if (!drawing) throw new IllegalStateException("AltDepthBatch.begin must be called before end.");
        if (idx > 0) flush();
        lastTexture = null;
        drawing = false;

        GL20 gl = Gdx.gl;
        gl.glDepthMask(true);
        if (isBlendingEnabled()) gl.glDisable(GL20.GL_BLEND);
        this.vertices = new float[this.vertices.length];
    }

    @Override
    public void setColor (Color tint) {
        color.set(tint);
        colorPacked = tint.toFloatBits();
    }

    @Override
    public void setColor (float r, float g, float b, float a) {
        color.set(r, g, b, a);
        colorPacked = color.toFloatBits();
    }

    @Override
    public Color getColor () {
        return color;
    }

    @Override
    public void setPackedColor (float packedColor) {
        Color.abgr8888ToColor(color, packedColor);
        this.colorPacked = packedColor;
    }

    @Override
    public float getPackedColor () {
        return colorPacked;
    }

    @Override
    public void flush () {
        if (idx == 0) return;

        renderCalls++;
        totalRenderCalls++;
        int spritesInBatch = idx / spriteSize;
        if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
        int count = spritesInBatch * this.indicesPerSprite;

        lastTexture.bind();
        Mesh mesh = this.mesh;
        mesh.setVertices(vertices, 0, idx);
        Buffer indicesBuffer = (Buffer)mesh.getIndicesBuffer(true);
        indicesBuffer.position(0);
        indicesBuffer.limit(count);

        if (blendingDisabled) {
            Gdx.gl.glDisable(GL20.GL_BLEND);
        } else {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            if (blendSrcFunc != -1) {
                Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha);
            }
        }

        mesh.render(customShader != null ? customShader : shader, GL20.GL_TRIANGLES, 0, count);

        idx = 0;
    }

    @Override
    public void disableBlending () {
        if (blendingDisabled) return;
        flush();
        blendingDisabled = true;
    }

    @Override
    public void enableBlending () {
        if (!blendingDisabled) return;
        flush();
        blendingDisabled = false;
    }

    @Override
    public void setBlendFunction (int srcFunc, int dstFunc) {
        setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc);
    }

    @Override
    public void setBlendFunctionSeparate (int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {
        if (blendSrcFunc == srcFuncColor && blendDstFunc == dstFuncColor && blendSrcFuncAlpha == srcFuncAlpha
                && blendDstFuncAlpha == dstFuncAlpha) return;
        flush();
        blendSrcFunc = srcFuncColor;
        blendDstFunc = dstFuncColor;
        blendSrcFuncAlpha = srcFuncAlpha;
        blendDstFuncAlpha = dstFuncAlpha;
    }

    @Override
    public int getBlendSrcFunc () {
        return blendSrcFunc;
    }

    @Override
    public int getBlendDstFunc () {
        return blendDstFunc;
    }

    @Override
    public int getBlendSrcFuncAlpha () {
        return blendSrcFuncAlpha;
    }

    @Override
    public int getBlendDstFuncAlpha () {
        return blendDstFuncAlpha;
    }

    @Override
    public void dispose () {
        mesh.dispose();
        if (ownsShader && shader != null) shader.dispose();
    }

    @Override
    public Matrix4 getProjectionMatrix () {
        return projectionMatrix;
    }

    @Override
    public Matrix4 getTransformMatrix () {
        return transformMatrix;
    }

    @Override
    public void setProjectionMatrix (Matrix4 projection) {
        if (drawing) flush();
        projectionMatrix.set(projection);
        if (drawing) setupMatrices();
    }

    @Override
    public void setTransformMatrix (Matrix4 transform) {
        if (drawing) flush();
        transformMatrix.set(transform);
        if (drawing) setupMatrices();
    }

    protected void setupMatrices () {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        if (customShader != null) {
            customShader.setUniformMatrix("u_projTrans", combinedMatrix);
            customShader.setUniformi("u_texture", 0);
        } else {
            shader.setUniformMatrix("u_projTrans", combinedMatrix);
            shader.setUniformi("u_texture", 0);
        }
    }

    protected void switchTexture (Texture texture) {
        flush();
        lastTexture = texture;
        invTexWidth = 1.0f / texture.getWidth();
        invTexHeight = 1.0f / texture.getHeight();
    }

    @Override
    public void setShader (ShaderProgram shader) {
        if (shader == customShader) // avoid unnecessary flushing in case we are drawing
            return;
        if (drawing) {
            flush();
        }
        customShader = shader;
        if (drawing) {
            if (customShader != null)
                customShader.bind();
            else
                this.shader.bind();
            setupMatrices();
        }
    }

    @Override
    public ShaderProgram getShader () {
        if (customShader == null) {
            return shader;
        }
        return customShader;
    }

    @Override
    public boolean isBlendingEnabled () {
        return !blendingDisabled;
    }

    public boolean isDrawing () {
        return drawing;
    }

    //// Draw methods unused by Tempto, so AltBatch implementations don't need to implement ////
    @Override
    public void draw (Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX,
                      float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        throw new IllegalArgumentException("AltBatch does not currently support this draw function");
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        throw new IllegalArgumentException("AltBatch does not currently support this draw function");
    }

    @Override
    public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
        throw new IllegalArgumentException("AltBatch does not currently support this draw function");
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
        throw new IllegalArgumentException("AltBatch does not currently support this draw function");
    }

    @Override
    public void draw(Texture texture, float x, float y) {
        throw new IllegalArgumentException("AltBatch does not currently support this draw function");
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height) {
        throw new IllegalArgumentException("AltBatch does not currently support this draw function");
    }


    @Override
    public void draw (Texture texture, float[] spriteVertices, int offset, int count) {
        if (!drawing) throw new IllegalStateException("AltBatch.begin must be called before draw.");

        int verticesLength = vertices.length;
        int remainingVertices = verticesLength;
        if (texture != lastTexture)
            switchTexture(texture);
        else {
            remainingVertices -= idx;
            if (remainingVertices == 0) {
                flush();
                remainingVertices = verticesLength;
            }
        }
        int copyCount = Math.min(remainingVertices, count);

        System.arraycopy(spriteVertices, offset, vertices, idx, copyCount);
        idx += copyCount;
        count -= copyCount;
        while (count > 0) {
            offset += copyCount;
            flush();
            copyCount = Math.min(verticesLength, count);
            System.arraycopy(spriteVertices, offset, vertices, 0, copyCount);
            idx += copyCount;
            count -= copyCount;
        }
    }

    public static final class Depth {
        static public final int X1 = 0;
        static public final int Y1 = 1;
        static public final int C1 = 2;
        static public final int U1 = 3;
        static public final int V1 = 4;
        static public final int D1 = 5;
        static public final int E1 = 6;
        static public final int X2 = 7;
        static public final int Y2 = 8;
        static public final int C2 = 9;
        static public final int U2 = 10;
        static public final int V2 = 11;
        static public final int D2 = 12;
        static public final int E2 = 13;
        static public final int X3 = 14;
        static public final int Y3 = 15;
        static public final int C3 = 16;
        static public final int U3 = 17;
        static public final int V3 = 18;
        static public final int D3 = 19;
        static public final int E3 = 20;
        static public final int X4 = 21;
        static public final int Y4 = 22;
        static public final int C4 = 23;
        static public final int U4 = 24;
        static public final int V4 = 25;
        static public final int D4 = 26;
        static public final int E4 = 27;
    }
}

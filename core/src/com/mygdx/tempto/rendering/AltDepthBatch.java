package com.mygdx.tempto.rendering;

import static com.mygdx.tempto.rendering.AltBatch.Depth.D1;
import static com.mygdx.tempto.rendering.AltBatch.Depth.D2;
import static com.mygdx.tempto.rendering.AltBatch.Depth.D3;
import static com.mygdx.tempto.rendering.AltBatch.Depth.D4;
import static com.mygdx.tempto.rendering.AltBatch.Depth.E1;
import static com.mygdx.tempto.rendering.AltBatch.Depth.E2;
import static com.mygdx.tempto.rendering.AltBatch.Depth.E3;
import static com.mygdx.tempto.rendering.AltBatch.Depth.E4;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.tempto.data.CentralTextureData;
import com.mygdx.tempto.rendering.AltBatch.Depth.*;
import com.badlogic.gdx.math.Matrix4;

import org.w3c.dom.Text;

import java.nio.Buffer;

/**A class implementing {@link Batch} to render to Tempto's depth map pipeline.
 * This pipeline involves passing a different number of values per vertex (7 instead of 5), thus requiring a custom Batch.
 * Values are as follows:
 *
 * -
 * */
public class AltDepthBatch extends AltBatch{

    public static final int DEPTH_SPRITE_SIZE = 28;
    public static final String DEPCOORD_ATTRIBUTE = "a_depCoord";
    public static final String DEPTH_VERT_PATH_INTERNAL = "shaders/depthVert.glsl", DEPTH_FRAG_PATH_INTERNAL = "shaders/depthFrag.glsl";



    /** Constructs a new SpriteBatch with a size of 1000, one buffer, and the default shader.
     * @see SpriteBatch#SpriteBatch(int, ShaderProgram) */
    public AltDepthBatch () {
        this(1000);
    }

    /** Constructs a SpriteBatch with one buffer and the default shader.
     * @see SpriteBatch#SpriteBatch(int, ShaderProgram) */
    public AltDepthBatch (int size) {
        this(size, null);
    }

    /** Constructs a new SpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point upwards, x-axis
     * point to the right and the origin being in the bottom left corner of the screen. The projection will be pixel perfect with
     * respect to the current screen resolution.
     * <p>
     * The defaultShader specifies the shader to use. Note that the names for uniforms for this default shader are different than
     * the ones expect for shaders set with {@link #setShader(ShaderProgram)}. See {@link #createDefaultShader()}.
     * @param size The max number of sprites in a single batch. Max of 8191.
     * @param defaultShader The default shader to use. This is not owned by the SpriteBatch and must be disposed separately. */
    public AltDepthBatch (int size, ShaderProgram defaultShader) {
        super(size, defaultShader, new Mesh(getDefaultVertexDataType(), false, size * 4, size * 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, DEPCOORD_ATTRIBUTE+"0")), DEPTH_SPRITE_SIZE);
    }


    /** Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified. */
    protected ShaderProgram createDefaultShader () {
        return new ShaderProgram(Gdx.files.internal(DEPTH_VERT_PATH_INTERNAL), Gdx.files.internal(DEPTH_FRAG_PATH_INTERNAL));
    }

    @Override
    public void draw (TextureRegion region, float x, float y) {
        draw(region, x, y, region.getRegionWidth(), region.getRegionHeight());
    }

    @Override
    public void draw (TextureRegion region, float x, float y, float width, float height) {
        if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        TextureRegion depthRegion = CentralTextureData.baseToDepthPairs.get(region);

        float[] vertices = this.vertices;

        Texture texture = region.getTexture();
        if (texture != lastTexture) {
            switchTexture(texture);
        } else if (idx == vertices.length) //
            flush();

        final float fx2 = x + width;
        final float fy2 = y + height;
        final float u = region.getU();
        final float v = region.getV2();
        final float u2 = region.getU2();
        final float v2 = region.getV();
        final float d = depthRegion.getU();
        final float e = depthRegion.getV2();
        final float d2 = depthRegion.getU2();
        final float e2 = depthRegion.getV();

        float color = this.colorPacked;
//        int idx = this.idx;
//        vertices[idx] = x;
//        vertices[idx + 1] = y;
//        vertices[idx + 2] = color;
//        vertices[idx + 3] = u;
//        vertices[idx + 4] = v;
//
//        vertices[idx + 5] = x;
//        vertices[idx + 6] = fy2;
//        vertices[idx + 7] = color;
//        vertices[idx + 8] = u;
//        vertices[idx + 9] = v2;
//
//        vertices[idx + 10] = fx2;
//        vertices[idx + 11] = fy2;
//        vertices[idx + 12] = color;
//        vertices[idx + 13] = u2;
//        vertices[idx + 14] = v2;
//
//        vertices[idx + 15] = fx2;
//        vertices[idx + 16] = y;
//        vertices[idx + 17] = color;
//        vertices[idx + 18] = u2;
//        vertices[idx + 19] = v;
        this.writeSpriteToDepthVertices(vertices,
                x, y, color, u, v, d, e,
                x, fy2, color, u, v2, d, e2,
                fx2, fy2, color, u2, v2, d2, e2,
                fy2, y, color, u2, v, d2, e);
    }

    @Override
    public void draw (TextureRegion region, float x, float y, float originX, float originY, float width, float height,
                      float scaleX, float scaleY, float rotation) {
        if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

        TextureRegion depthRegion = CentralTextureData.baseToDepthPairs.get(region);

        float[] vertices = this.vertices;

        Texture texture = region.getTexture();
        if (texture != lastTexture) {
            switchTexture(texture);
        } else if (idx == vertices.length) //
            flush();

        // bottom left and top right corner points relative to origin
        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;

        // scale
        if (scaleX != 1 || scaleY != 1) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }

        // construct corner points, start from top left and go counter clockwise
        final float p1x = fx;
        final float p1y = fy;
        final float p2x = fx;
        final float p2y = fy2;
        final float p3x = fx2;
        final float p3y = fy2;
        final float p4x = fx2;
        final float p4y = fy;

        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;

        // rotate
        if (rotation != 0) {
            final float cos = MathUtils.cosDeg(rotation);
            final float sin = MathUtils.sinDeg(rotation);

            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;

            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;

            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;

            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;

            x2 = p2x;
            y2 = p2y;

            x3 = p3x;
            y3 = p3y;

            x4 = p4x;
            y4 = p4y;
        }

        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;

        final float u = region.getU();
        final float v = region.getV2();
        final float u2 = region.getU2();
        final float v2 = region.getV();

        final float d = depthRegion.getU();
        final float e = depthRegion.getV2();
        final float d2 = depthRegion.getU2();
        final float e2 = depthRegion.getV();

        float color = this.colorPacked;
        this.writeSpriteToDepthVertices(vertices,
                x1, y1, color, u, v, d, e,
                x2, y2, color, u, v2, d, e2,
                x3, y3, color, u2, v2, d2, e2,
                x4, y4, color, u2, v, d2, e);
    }

    @Override
    public void draw (TextureRegion region, float x, float y, float originX, float originY, float width, float height,
                      float scaleX, float scaleY, float rotation, boolean clockwise) {
        if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

        TextureRegion depthRegion = CentralTextureData.baseToDepthPairs.get(region);

        float[] vertices = this.vertices;

        Texture texture = region.getTexture();
        if (texture != lastTexture) {
            switchTexture(texture);
        } else if (idx == vertices.length) //
            flush();

        // bottom left and top right corner points relative to origin
        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;

        // scale
        if (scaleX != 1 || scaleY != 1) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }

        // construct corner points, start from top left and go counter clockwise
        final float p1x = fx;
        final float p1y = fy;
        final float p2x = fx;
        final float p2y = fy2;
        final float p3x = fx2;
        final float p3y = fy2;
        final float p4x = fx2;
        final float p4y = fy;

        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;

        // rotate
        if (rotation != 0) {
            final float cos = MathUtils.cosDeg(rotation);
            final float sin = MathUtils.sinDeg(rotation);

            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;

            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;

            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;

            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;

            x2 = p2x;
            y2 = p2y;

            x3 = p3x;
            y3 = p3y;

            x4 = p4x;
            y4 = p4y;
        }

        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;

        float u1, v1, u2, v2, u3, v3, u4, v4;
        float d1, e1, d2, e2, d3, e3, d4, e4;
        if (clockwise) {
            u1 = region.getU2();
            v1 = region.getV2();
            u2 = region.getU();
            v2 = region.getV2();
            u3 = region.getU();
            v3 = region.getV();
            u4 = region.getU2();
            v4 = region.getV();

            d1 = depthRegion.getU2();
            e1 = depthRegion.getV2();
            d2 = depthRegion.getU();
            e2 = depthRegion.getV2();
            d3 = depthRegion.getU();
            e3 = depthRegion.getV();
            d4 = depthRegion.getU2();
            e4 = depthRegion.getV();
        } else {
            u1 = region.getU();
            v1 = region.getV();
            u2 = region.getU2();
            v2 = region.getV();
            u3 = region.getU2();
            v3 = region.getV2();
            u4 = region.getU();
            v4 = region.getV2();

            d1 = depthRegion.getU();
            e1 = depthRegion.getV();
            d2 = depthRegion.getU2();
            e2 = depthRegion.getV();
            d3 = depthRegion.getU2();
            e3 = depthRegion.getV2();
            d4 = depthRegion.getU();
            e4 = depthRegion.getV2();
        }

        float color = this.colorPacked;

        this.writeSpriteToDepthVertices(vertices,
                x1, y1, color, u1, v1, d1, e1,
                x2, y2, color, u2, v2, d2, e2,
                x3, y3, color, u3, v3, d3, e3,
                x4, y4, color, u4, v4, d4, e4);

    }

    public void writeSpriteToDepthVertices(float[] vertices,
            float x1, float y1, float c1, float u1, float v1, float d1, float e1,
            float x2, float y2, float c2, float u2, float v2, float d2, float e2,
            float x3, float y3, float c3, float u3, float v3, float d3, float e3,
            float x4, float y4, float c4, float u4, float v4, float d4, float e4) {

        int idx = this.idx;

        vertices[idx + X1] = x1;
        vertices[idx + Y1] = y1;
        vertices[idx + C1] = c1;
        vertices[idx + U1] = u1;
        vertices[idx + V1] = v1;
        vertices[idx + D1] = d1;
        vertices[idx + E1] = e1;

        vertices[idx + X2] = x2;
        vertices[idx + Y2] = y2;
        vertices[idx + C2] = c2;
        vertices[idx + U2] = u2;
        vertices[idx + V2] = v2;
        vertices[idx + D2] = d2;
        vertices[idx + E2] = e2;

        vertices[idx + X3] = x3;
        vertices[idx + Y3] = y3;
        vertices[idx + C3] = c3;
        vertices[idx + U3] = u3;
        vertices[idx + V3] = v3;
        vertices[idx + D3] = d3;
        vertices[idx + E3] = e3;

        vertices[idx + X4] = x4;
        vertices[idx + Y4] = y4;
        vertices[idx + C4] = c4;
        vertices[idx + U4] = u4;
        vertices[idx + V4] = v4;
        vertices[idx + D4] = d4;
        vertices[idx + E4] = e4;
        this.idx = idx + this.spriteSize;
    }

    @Override
    public void draw (TextureRegion region, float width, float height, Affine2 transform) {
        if (!drawing) throw new IllegalStateException("AltDepthBatch.begin must be called before draw.");

        float[] vertices = this.vertices;

        Texture texture = region.getTexture();
        if (texture != lastTexture) {
            switchTexture(texture);
        } else if (idx == vertices.length) {
            flush();
        }

        // construct corner points
        float x1 = transform.m02;
        float y1 = transform.m12;
        float x2 = transform.m01 * height + transform.m02;
        float y2 = transform.m11 * height + transform.m12;
        float x3 = transform.m00 * width + transform.m01 * height + transform.m02;
        float y3 = transform.m10 * width + transform.m11 * height + transform.m12;
        float x4 = transform.m00 * width + transform.m02;
        float y4 = transform.m10 * width + transform.m12;

        float u = region.getU();
        float v = region.getV2();
        float u2 = region.getU2();
        float v2 = region.getV();

        float color = this.colorPacked;
        int idx = this.idx;
        vertices[idx] = x1;
        vertices[idx + 1] = y1;
        vertices[idx + 2] = color;
        vertices[idx + 3] = u;
        vertices[idx + 4] = v;

        vertices[idx + 5] = x2;
        vertices[idx + 6] = y2;
        vertices[idx + 7] = color;
        vertices[idx + 8] = u;
        vertices[idx + 9] = v2;

        vertices[idx + 10] = x3;
        vertices[idx + 11] = y3;
        vertices[idx + 12] = color;
        vertices[idx + 13] = u2;
        vertices[idx + 14] = v2;

        vertices[idx + 15] = x4;
        vertices[idx + 16] = y4;
        vertices[idx + 17] = color;
        vertices[idx + 18] = u2;
        vertices[idx + 19] = v;
        this.idx = idx + this.spriteSize;
    }


}

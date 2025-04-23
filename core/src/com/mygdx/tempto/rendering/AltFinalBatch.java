package com.mygdx.tempto.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;

/**For now, operates similarly to {@link SpriteBatch}, but putting this out there to reinforce the pattern of {@link AltBatch} and {@link AltDepthBatch} before we make the shadow batch*/
public class AltFinalBatch extends AltBatch{

    public static final int FINAL_SPRITE_SIZE = 20;
    public AltFinalBatch(int size, ShaderProgram defaultShader) {
        super(size, defaultShader, new Mesh((Gdx.gl30 != null) ? Mesh.VertexDataType.VertexBufferObjectWithVAO : defaultVertexDataType, false, size * 4, size * 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0")), FINAL_SPRITE_SIZE);
    }

    public AltFinalBatch() {
        this(1000);
    }
    public AltFinalBatch(int size) {
        this(size, null);
    }

    @Override
    protected ShaderProgram createDefaultShader() {
        return SpriteBatch.createDefaultShader();
    }


    @Override
    public void draw (Texture texture, float x, float y) {
        draw(texture, x, y, texture.getWidth(), texture.getHeight());
    }

    @Override
    public void draw (Texture texture, float x, float y, float width, float height) {
        if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

        float[] vertices = this.vertices;

        if (texture != lastTexture)
            switchTexture(texture);
        else if (idx == vertices.length) //
            flush();

        final float fx2 = x + width;
        final float fy2 = y + height;
        final float u = 0;
        final float v = 1;
        final float u2 = 1;
        final float v2 = 0;

        float color = this.colorPacked;
        int idx = this.idx;
        vertices[idx] = x;
        vertices[idx + 1] = y;
        vertices[idx + 2] = color;
        vertices[idx + 3] = u;
        vertices[idx + 4] = v;

        vertices[idx + 5] = x;
        vertices[idx + 6] = fy2;
        vertices[idx + 7] = color;
        vertices[idx + 8] = u;
        vertices[idx + 9] = v2;

        vertices[idx + 10] = fx2;
        vertices[idx + 11] = fy2;
        vertices[idx + 12] = color;
        vertices[idx + 13] = u2;
        vertices[idx + 14] = v2;

        vertices[idx + 15] = fx2;
        vertices[idx + 16] = y;
        vertices[idx + 17] = color;
        vertices[idx + 18] = u2;
        vertices[idx + 19] = v;
        this.idx = idx + 20;
    }

    @Override
    public void draw(TextureRegion region, float x, float y) {

    }


    @Override
    public void draw(TextureRegion region, float x, float y, float width, float height) {

    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {

    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, boolean clockwise) {

    }

    @Override
    public void draw(TextureRegion region, float width, float height, Affine2 transform) {

    }
}

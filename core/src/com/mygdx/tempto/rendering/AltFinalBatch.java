package com.mygdx.tempto.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.mygdx.tempto.TemptoNova;

import org.w3c.dom.Text;

import java.nio.Buffer;

/**For now, operates similarly to {@link SpriteBatch}, but putting this out there to reinforce the pattern of {@link AltBatch} and {@link AltDepthBatch} before we make the shadow batch.
 * Similarly to {@link AltDepthBatch}, uses color attribute to pass base depth information*/
public class AltFinalBatch extends AltBatch{

    public static final String FINAL_FRAG_PATH = "shaders/finalPassFrag.glsl";

    public static final String DEPTHMAP_UNIFORM = "u_depthMapTex";
    public static final String LIGHTMAP_UNIFORM = "u_lightMapTex";
    protected final static String SENSITIVITY_UNIFORM = "u_sensitivity";

    public static final int FINAL_SPRITE_SIZE = 20;

    protected Texture lastLightTexture;
    protected Texture lastDepthTexture;

    protected float sensitivity = 0.5f;

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
        return new ShaderProgram(AltBatch.defaultVertexShader(), Gdx.files.internal(FINAL_FRAG_PATH).readString());
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

    @Override
    public void flush() {
        if (idx == 0) return;

        renderCalls++;
        totalRenderCalls++;
        int spritesInBatch = idx / spriteSize;
        if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
        int count = spritesInBatch * this.indicesPerSprite;

        lastDepthTexture.bind(2);
        lastLightTexture.bind(1);
        lastTexture.bind(0);

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
    protected void setupMatrices() {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);

        ShaderProgram shaderToSet = customShader != null? customShader : shader;

        shaderToSet.setUniformMatrix("u_projTrans", combinedMatrix);
        shaderToSet.setUniformf("u_invScreenDims", 1f/(float) TemptoNova.PIXEL_GAME_WIDTH, 1f/(float) TemptoNova.PIXEL_GAME_HEIGHT);
        shaderToSet.setUniformf("u_lightDecodeFactor", 1f/AltLightBatch.BASE_LIGHT_ENCODING_FACTOR);
        shaderToSet.setUniformf(SENSITIVITY_UNIFORM, this.sensitivity);
        shaderToSet.setUniformi("u_texture", 0);
        shaderToSet.setUniformi(LIGHTMAP_UNIFORM, 1);
        shaderToSet.setUniformi(DEPTHMAP_UNIFORM, 2);
    }

    public void switchLightTexture(Texture lightTexture) {
        if (lightTexture != this.lastLightTexture) this.flush();
        this.lastLightTexture = lightTexture;
//        this.invLightWidth = 1f/(float) lightTexture.getWidth();
//        this.invLightHeight = 1f/(float) lightTexture.getHeight();
    }

    public void switchDepthTexture(Texture depthTexture) {
        if (depthTexture != this.lastDepthTexture) this.flush();
        this.lastDepthTexture = depthTexture;
    }

    public float getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }
}

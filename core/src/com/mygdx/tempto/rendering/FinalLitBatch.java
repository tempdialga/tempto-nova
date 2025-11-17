package com.mygdx.tempto.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.nio.Buffer;

public class FinalLitBatch extends AltBatch{

    public static final float BASE_EXPOSURE = 0.5f;
    public static float exposureModifier = 1f;


    public static final String LIGHTCOORD_ATTRIBUTE = "a_lightCoord";
    public static final String SHADOWCHANNEL_ATTRIBUTE = "a_shadowChannel";
    public static final String LIGHTCOLOR_ATTRIBUTE = "a_lightColor";
    public static final String POSCHANNEL_ATTRIBUTE = "a_positionChannel";
    public static final String LIGHTSPREAD_ATTRIBUTE = "a_lightSpread";

    protected final static String DMAPTEX_UNIFORM = "u_dMapTex";
    protected final static String SHADMAP_UNIFORM = "u_shadMapTex";
    protected final static String CMAPTEX_UNIFORM = "u_baseColTex";
    protected final static String POSDIMS_UNIFORM = "u_positionChannelDimensions";
    protected final static String VIEWDIMS_UNIFORM = "u_viewDims";
    protected final static String EXPOSURE_UNIFORM = "u_exposure";

    public static final String LIGHT_VERT_PATH_INTERNAL = "shaders/lightVert.glsl", LIGHT_FRAG_PATH_INTERNAL = "shaders/lightFinalFrag.glsl";
    public static ShaderProgram pointShader;

    public static final String LIGHT_AMB_FRAG_INTERNAL = "shaders/lightFinalAmbientFrag.glsl";
    public static ShaderProgram ambientShader;

    private static int i=0;
    public static final int X1 = i++, Y1 = i++, A1 = i++, B1 = i++, C1 = i++, Ch1 = i++, Col1 = i++, ChC1 = i++, ChR1 = i++, Spr1 = i++,
                            X2 = i++, Y2 = i++, A2 = i++, B2 = i++, C2 = i++, Ch2 = i++, Col2 = i++, ChC2 = i++, ChR2 = i++, Spr2 = i++,
                            X3 = i++, Y3 = i++, A3 = i++, B3 = i++, C3 = i++, Ch3 = i++, Col3 = i++, ChC3 = i++, ChR3 = i++, Spr3 = i++,
                            X4 = i++, Y4 = i++, A4 = i++, B4 = i++, C4 = i++, Ch4 = i++, Col4 = i++, ChC4 = i++, ChR4 = i++, Spr4 = i++;

    public static final int LIGHT_SPRITE_SIZE = i;

    protected Texture lastShadowTexture;
    protected Texture lastColorTexture;
    protected float[] posChannelDims = new float[]{1.0f, 1.0f}; // Dimensions of each channel in the shadow map
    protected float[] viewDims = new float[2]; // Dimensions of the screen's view in the world (for light-point distance calculation)

    public FinalLitBatch() {
        this(1000, null);
    }
    public FinalLitBatch(int size, ShaderProgram defaultShader) {
        super(size, defaultShader, new Mesh((Gdx.gl30 != null) ? Mesh.VertexDataType.VertexBufferObjectWithVAO : defaultVertexDataType, false, size * 4, size * 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.Position, 3, LIGHTCOORD_ATTRIBUTE+"0"),
                new VertexAttribute(VertexAttributes.Usage.Generic, 1, SHADOWCHANNEL_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, LIGHTCOLOR_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.Generic, 2, POSCHANNEL_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.Generic, 1, LIGHTSPREAD_ATTRIBUTE)), LIGHT_SPRITE_SIZE);
        ambientShader = new ShaderProgram(Gdx.files.internal(LIGHT_VERT_PATH_INTERNAL), Gdx.files.internal(LIGHT_AMB_FRAG_INTERNAL));
        pointShader = new ShaderProgram(Gdx.files.internal(LIGHT_VERT_PATH_INTERNAL), Gdx.files.internal(LIGHT_FRAG_PATH_INTERNAL));
    }

    public void adjustChannelDims(int numColumns, int numRows) {
        this.posChannelDims[0] = 1f/((float) numColumns);
        this.posChannelDims[1] = 1f/((float) numRows);
    }

    public void setViewport(Viewport viewport) {
        this.viewDims[0] = viewport.getWorldWidth();
        this.viewDims[1] = viewport.getWorldHeight();
    }

    @Override
    protected ShaderProgram createDefaultShader() {
        return new ShaderProgram(Gdx.files.internal(LIGHT_VERT_PATH_INTERNAL), Gdx.files.internal(LIGHT_FRAG_PATH_INTERNAL));
    }

    @Override
    protected void setupMatrices () {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        ShaderProgram shaderToSet = this.currentShader();
        shaderToSet.bind();
        shaderToSet.setUniformMatrix("u_projTrans", combinedMatrix);
        shaderToSet.setUniformf(EXPOSURE_UNIFORM, BASE_EXPOSURE*exposureModifier);
        if (shaderToSet != ambientShader) { // Ambient fragment shader doesn't need these
            shaderToSet.setUniform2fv(POSDIMS_UNIFORM, this.posChannelDims, 0, 2);
            shaderToSet.setUniform2fv(VIEWDIMS_UNIFORM, this.viewDims, 0, 2);
            shaderToSet.setUniformi(DMAPTEX_UNIFORM, 2);
            shaderToSet.setUniformi(SHADMAP_UNIFORM, 1);
        }
        shaderToSet.setUniformi(CMAPTEX_UNIFORM, 0);

    }


    @Override
    public void flush () {
        if (idx == 0) return;

        renderCalls++;
        totalRenderCalls++;
        int spritesInBatch = idx / spriteSize;
        if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
        int count = spritesInBatch * this.indicesPerSprite;

        lastTexture.bind(2);
        lastShadowTexture.bind(1);
        lastColorTexture.bind(0);

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

    public void switchShadowTexture (Texture shadowTexture) {
        flush();
        this.lastShadowTexture = shadowTexture;
    }

    public void switchColorTexture (Texture colorTexture) {
        flush();
        this.lastColorTexture = colorTexture;
    }

    public void drawAmbient(Color color, Texture depthMap, Rectangle viewBounds) {
        float[] verts = new float[LIGHT_SPRITE_SIZE];
        float s = 1;
        verts[Spr1] = s;
        verts[Spr2] = s;
        verts[Spr3] = s;
        verts[Spr4] = s;

        Vector3 p = new Vector3(0,0,0); // Ambient lighting doesn't need a set position, that we know of at least (still here to remind that this uses the same vertex shader)
        // TODO: Do we want to make a different mesh setup for ambient lights with less info? Or would that imply just making a whole new kind of batch
        float l = viewBounds.x, r = viewBounds.x + viewBounds.width,
                d = viewBounds.y, u = viewBounds.y + viewBounds.height;

        verts[X1] = l;
        verts[Y1] = d;
        verts[A1] = p.x;
        verts[B1] = p.y;
        verts[C1] = p.z;

        verts[X2] = l;
        verts[Y2] = u;
        verts[A2] = p.x;
        verts[B2] = p.y;
        verts[C2] = p.z;

        verts[X3] = r;
        verts[Y3] = u;
        verts[A3] = p.x;
        verts[B3] = p.y;
        verts[C3] = p.z;

        verts[X4] = r;
        verts[Y4] = d;
        verts[A4] = p.x;
        verts[B4] = p.y;
        verts[C4] = p.z;


        float shadowColorChannel = 0, horizontal_idx = 0, vertical_idx = 0; // placeholder values to use same vertex shader, it won't actually use these
        verts[Ch1] = shadowColorChannel;
        verts[Ch2] = shadowColorChannel;
        verts[Ch3] = shadowColorChannel;
        verts[Ch4] = shadowColorChannel;

        float packedColor = color.toFloatBits(); // It does actually use the color tho
        verts[Col1] = packedColor;
        verts[Col2] = packedColor;
        verts[Col3] = packedColor;
        verts[Col4] = packedColor;

        verts[ChC1] = horizontal_idx;
        verts[ChR1] = vertical_idx;
        verts[ChC2] = horizontal_idx;
        verts[ChR2] = vertical_idx;
        verts[ChC3] = horizontal_idx;
        verts[ChR3] = vertical_idx;
        verts[ChC4] = horizontal_idx;
        verts[ChR4] = vertical_idx;


        this.draw(depthMap, verts, 0, verts.length);
    }

    public void drawLight(LightSource source, Texture depthMap, OrthographicCamera camera, Rectangle viewBounds, float shadowColorChannel, int horizontal_idx, int vertical_idx) {
        Vector3 p = source.pos();
        Vector3 p_screen = camera.project(new Vector3(p));
        p_screen.x /= (float) Gdx.graphics.getWidth();
        p_screen.y /= (float) Gdx.graphics.getHeight();
        p_screen.z = p.z;

        float s = source.spread();
        float[] verts = new float[LIGHT_SPRITE_SIZE];
        verts[Spr1] = s;
        verts[Spr2] = s;
        verts[Spr3] = s;
        verts[Spr4] = s;

        float l = p.x-s, r=p.x+s, u=p.y+s, d=p.y-s;

        p = p_screen;

        verts[X1] = l;
        verts[Y1] = d;
        verts[A1] = p.x;
        verts[B1] = p.y;
        verts[C1] = p.z;

        verts[X2] = l;
        verts[Y2] = u;
        verts[A2] = p.x;
        verts[B2] = p.y;
        verts[C2] = p.z;

        verts[X3] = r;
        verts[Y3] = u;
        verts[A3] = p.x;
        verts[B3] = p.y;
        verts[C3] = p.z;

        verts[X4] = r;
        verts[Y4] = d;
        verts[A4] = p.x;
        verts[B4] = p.y;
        verts[C4] = p.z;

        verts[Ch1] = shadowColorChannel;
        verts[Ch2] = shadowColorChannel;
        verts[Ch3] = shadowColorChannel;
        verts[Ch4] = shadowColorChannel;

        float packedColor = source.color().toFloatBits();
        verts[Col1] = packedColor;
        verts[Col2] = packedColor;
        verts[Col3] = packedColor;
        verts[Col4] = packedColor;

        //Region of the shadow map to read from
        verts[ChC1] = horizontal_idx;
        verts[ChR1] = vertical_idx;
        verts[ChC2] = horizontal_idx;
        verts[ChR2] = vertical_idx;
        verts[ChC3] = horizontal_idx;
        verts[ChR3] = vertical_idx;
        verts[ChC4] = horizontal_idx;
        verts[ChR4] = vertical_idx;


        this.draw(depthMap, verts, 0, verts.length);
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

package com.mygdx.tempto.rendering;

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.tempto.TemptoNova;

import java.nio.Buffer;

public class AltLightBatch extends AltBatch{

    public static final float BASE_LIGHT_ENCODING_FACTOR = 0.125f;//All intensities multiplied by this going into the light map, and then divided coming out, to allow light to exceed 1 effectively and wash out



    public static final String LIGHTCOORD_ATTRIBUTE = "a_lightCoord";
    public static final String SHADOWCHANNEL_ATTRIBUTE = "a_shadowChannel";
    public static final String LIGHTCOLOR_ATTRIBUTE = "a_lightColor";
    public static final String POSCHANNEL_ATTRIBUTE = "a_positionChannel";
    public static final String LIGHTSPREAD_ATTRIBUTE = "a_lightSpread";

    protected final static String DMAPTEX_UNIFORM = "u_dMapTex";
    protected final static String SHADMAP_UNIFORM = "u_shadMapTex";
    protected final static String POSDIMS_UNIFORM = "u_positionChannelDimensions";
    protected final static String LIGHTENCODEFACTOR_UNIFORM = "u_lightEncodeFactor";

    public static final String LIGHT_VERT_PATH_INTERNAL = "shaders/lightVert.glsl", LIGHT_FRAG_PATH_INTERNAL = "shaders/lightFrag.glsl";

    private static int i=0;
    public static final int X1 = i++, Y1 = i++, /*U1 = i++, V1 = i++,*/ A1 = i++, B1 = i++, C1 = i++, Ch1 = i++, Col1 = i++, ChC1 = i++, ChR1 = i++, Spr1 = i++,/* ChW1 = i++, ChH1 = i++,*/
                            X2 = i++, Y2 = i++, /*U2 = i++, V2 = i++,*/ A2 = i++, B2 = i++, C2 = i++, Ch2 = i++, Col2 = i++, ChC2 = i++, ChR2 = i++, Spr2 = i++,/* ChW2 = i++, ChH2 = i++,*/
                            X3 = i++, Y3 = i++, /*U3 = i++, V3 = i++,*/ A3 = i++, B3 = i++, C3 = i++, Ch3 = i++, Col3 = i++, ChC3 = i++, ChR3 = i++, Spr3 = i++,/* ChW3 = i++, ChH3 = i++,*/
                            X4 = i++, Y4 = i++, /*U4 = i++, V4 = i++,*/ A4 = i++, B4 = i++, C4 = i++, Ch4 = i++, Col4 = i++, ChC4 = i++, ChR4 = i++, Spr4 = i++/*, ChW4 = i++, ChH4 = i++*/;

    public static final int LIGHT_SPRITE_SIZE = i;

    protected int loc_u_S;//Location of the uniform for u_S (light source)
    protected int loc_u_viewDims = -10;//Location of the uniform for u_viewDims (dimensions of the view window)
    protected Texture lastShadowTexture;
    protected float[] posChannelDims = new float[]{1.0f, 1.0f};//Dimensions of each channel in the shadow map

    public AltLightBatch() {
        this(1000, null);
    }
    public AltLightBatch(int size, ShaderProgram defaultShader) {
        super(size, defaultShader, new Mesh((Gdx.gl30 != null) ? Mesh.VertexDataType.VertexBufferObjectWithVAO : defaultVertexDataType, false, size * 4, size * 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
//                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, AltDepthBatch.DEPCOORD_ATTRIBUTE + "0"),
                new VertexAttribute(VertexAttributes.Usage.Position, 3, LIGHTCOORD_ATTRIBUTE+"0"),
                new VertexAttribute(VertexAttributes.Usage.Generic, 1, SHADOWCHANNEL_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, LIGHTCOLOR_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.Generic, 2, POSCHANNEL_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.Generic, 1, LIGHTSPREAD_ATTRIBUTE)), LIGHT_SPRITE_SIZE);


    }

    public void adjustChannelDims(int numColumns, int numRows) {
        this.posChannelDims[0] = 1f/((float) numColumns);
        this.posChannelDims[1] = 1f/((float) numRows);
    }

    public void setViewport(Viewport viewport) {
        if (this.loc_u_viewDims == -10) this.loc_u_viewDims = this.shader.fetchUniformLocation("u_viewDims", true);
        this.shader.setUniform2fv(this.loc_u_viewDims, new float[]{viewport.getWorldWidth(), viewport.getWorldHeight()}, 0, 2);
    }

    @Override
    protected ShaderProgram createDefaultShader() {
        return new ShaderProgram(Gdx.files.internal(LIGHT_VERT_PATH_INTERNAL), Gdx.files.internal(LIGHT_FRAG_PATH_INTERNAL));
    }

    @Override
    protected void setupMatrices () {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        ShaderProgram shaderToSet;
        if (customShader != null) {
            shaderToSet = customShader;
        } else {
            shaderToSet = shader;
        }
        shaderToSet.setUniformMatrix("u_projTrans", combinedMatrix);
        shaderToSet.setUniform2fv(POSDIMS_UNIFORM, this.posChannelDims, 0, 2);
        shaderToSet.setUniformf(LIGHTENCODEFACTOR_UNIFORM, BASE_LIGHT_ENCODING_FACTOR);
        shaderToSet.setUniformi(DMAPTEX_UNIFORM, 0);
        shaderToSet.setUniformi(SHADMAP_UNIFORM, 1);

    }


    @Override
    public void flush () {
        if (idx == 0) return;

        renderCalls++;
        totalRenderCalls++;
        int spritesInBatch = idx / spriteSize;
        if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
        int count = spritesInBatch * this.indicesPerSprite;

        lastShadowTexture.bind(1);
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

    protected void switchShadowTexture (Texture shadowTexture) {
        flush();
        lastShadowTexture = shadowTexture;
//        invTexWidth = 1.0f / texture.getWidth();
//        invTexHeight = 1.0f / texture.getHeight();
    }

    public void drawLight(LightSource source, Texture depthMap, Texture shadowMap, OrthographicCamera camera, Rectangle viewBounds, float shadowColorChannel, int horizontal_idx, int vertical_idx) {

        if (shadowMap != lastShadowTexture)
            switchShadowTexture(shadowMap);

        Vector3 p = source.pos();
        Vector3 p_screen = camera.project(new Vector3(p));
//        p_screen.x /= (float) TemptoNova.PIXEL_GAME_WIDTH;
//        p_screen.y /= (float) TemptoNova.PIXEL_GAME_HEIGHT;
        p_screen.x /= (float) Gdx.graphics.getWidth();
        p_screen.y /= (float) Gdx.graphics.getHeight();
        p_screen.z = p.z;
//        p_screen.scl(0.5f,0.5f,1);

        float s = source.spread();
        float[] verts = new float[LIGHT_SPRITE_SIZE];
        verts[Spr1] = s;
        verts[Spr2] = s;
        verts[Spr3] = s;
        verts[Spr4] = s;

        float l = p.x-s, r=p.x+s, u=p.y+s, d=p.y-s;
//        float l = Math.max(viewBounds.x, p.x-s),
//                r=Math.min(viewBounds.x+viewBounds.width, p.x+s),
//                u=Math.min(viewBounds.y+viewBounds.height, p.y+s),
//                d=Math.max(viewBounds.y, p.x-s);
//        float u1 = 0, v1 = 0, u2 = 1, v2 = 1; //Uh wait actually do we need these

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

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
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.tempto.data.CentralTextureData;
import com.mygdx.tempto.util.MiscFunctions;
import com.mygdx.tempto.view.GameScreen;

import java.nio.Buffer;

public class AltShadeBatch extends AltBatch {

    

    protected final static String SHADOWVERT_PATH_INTERNAL = "shaders/shadeVert.glsl";
    protected final static String SHADOWFRAG_PATH_INTERNAL = "shaders/shadeFrag_4samp_direct.glsl";

    protected final static String DEPTHMAPCOORD_ATTRIBUTE = AltDepthBatch.DEPCOORD_ATTRIBUTE;
    protected final static String SHADOWTEXCOORD_ATTRIBUTE = "a_shadTexCoord";
    protected final static String SHADOWTEXDIMS_ATTRIBUTE = "a_shadTexDims";
    protected final static String SHADOWTEXDEPCOORD_ATTRIBUTE = "a_shadTexDepCoord"; //Coordinates of the depth texture on the same texture as the base texture


    protected final static String DMAPTEX_UNIFORM = "u_dMapTex";
    protected final static String SHADTEX_UNIFORM = "u_shadTex";
    protected final static String SHADPXDIM_UNIFORM = "u_shadPxDims";
    protected final static String SHADTEXWIDTH_UNIFORM = "u_shadTexWidth";
    protected final static String SHADTEXHEIGHT_UNIFORM = "u_shadTexHeight";

    protected final static String   A_ATTRIBUTE = "a_a",
                                    AB_ATTRIBUTE = "a_ab",
                                    AC_ATTRIBUTE = "a_ac",
                                    S_ATTRIBUTE = "a_S";

    private static int i=0;
    public static final int X1 = i++, Y1 = i++, D1 = i++, E1 = i++, ShU1 = i++, ShV1 = i++, ShW1 = i++, ShH1 = i++, ShD1 = i++, ShE1 = i++, AX1 = i++, AY1 = i++, AZ1 = i++, ABX1 = i++, ABY1 = i++, ABZ1 = i++, ACX1 = i++, ACY1 = i++, ACZ1 = i++, SX1 = i++, SY1 = i++, SZ1 = i++,
                            X2 = i++, Y2 = i++, D2 = i++, E2 = i++, ShU2 = i++, ShV2 = i++, ShW2 = i++, ShH2 = i++, ShD2 = i++, ShE2 = i++, AX2 = i++, AY2 = i++, AZ2 = i++, ABX2 = i++, ABY2 = i++, ABZ2 = i++, ACX2 = i++, ACY2 = i++, ACZ2 = i++, SX2 = i++, SY2 = i++, SZ2 = i++,
                            X3 = i++, Y3 = i++, D3 = i++, E3 = i++, ShU3 = i++, ShV3 = i++, ShW3 = i++, ShH3 = i++, ShD3 = i++, ShE3 = i++, AX3 = i++, AY3 = i++, AZ3 = i++, ABX3 = i++, ABY3 = i++, ABZ3 = i++, ACX3 = i++, ACY3 = i++, ACZ3 = i++, SX3 = i++, SY3 = i++, SZ3 = i++;
    protected final static int SHADOW_SPRITE_SIZE = i;


    protected static Mesh.VertexDataType meshVertexDataType = (Gdx.gl30 != null) ? Mesh.VertexDataType.VertexBufferObjectWithVAO : defaultVertexDataType;

    protected Texture lastShadowTexture;
    protected float invShadTexWidth = 0, invShadTexHeight = 0;
    private int loc_u_viewDims = -10;

    public AltShadeBatch() {this(1000, null);}

    public AltShadeBatch(int size, ShaderProgram defaultShader) {
        super(size, defaultShader, createShadowMesh(size),
                SHADOW_SPRITE_SIZE,
                false);

    }

    public static Mesh createShadowMesh(int size) {
        return new Mesh(meshVertexDataType, false, size * 4, size * 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, DEPTHMAPCOORD_ATTRIBUTE + "0"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, SHADOWTEXCOORD_ATTRIBUTE + "0"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, SHADOWTEXDIMS_ATTRIBUTE+"0"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, SHADOWTEXDEPCOORD_ATTRIBUTE+"0"),
                new VertexAttribute(VertexAttributes.Usage.Generic, 3, A_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.Generic, 3, AB_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.Generic, 3, AC_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.Generic, 3, S_ATTRIBUTE));
    }

    @Override
    protected ShaderProgram createDefaultShader() {
        return new ShaderProgram(Gdx.files.internal(SHADOWVERT_PATH_INTERNAL), Gdx.files.internal(SHADOWFRAG_PATH_INTERNAL));
    }

    private float cross2D(Vector2 lineEnd, Vector2 point) {
        return lineEnd.x*point.y - lineEnd.y*point.x;
    }
    public void drawShadow(ShadowCaster caster, LightSource source, Texture depthMap, OrthographicCamera camera, Rectangle viewBounds, Polygon viewPolygonClockwise) {
        
        Vector3 cPos = caster.origin();
        Vector3 cU = caster.u();
        Vector3 cV = caster.v();
        Vector3 S = source.pos();
        float r = source.radius();

        final float[] vertices = new float[SHADOW_SPRITE_SIZE];

        //Initialize data that's the same for all vertices
        //Shadow location information TODO: Do we need to preemptively project these into [0-1] screen coords?
        Vector3 cPos_nor = new Vector3(cPos);
        cPos_nor.x = MiscFunctions.parameterizeWithDistance(viewBounds.x, viewBounds.width, cPos.x);
        cPos_nor.y = MiscFunctions.parameterizeWithDistance(viewBounds.y, viewBounds.height, cPos.y);
        Vector3 cU_nor = new Vector3(cU).scl(1/viewBounds.width, 1/viewBounds.height, 1);
        Vector3 cV_nor = new Vector3(cV).scl(1/viewBounds.width, 1/viewBounds.height, 1);
        vertices[AX1] = cPos_nor.x;
        vertices[AY1] = cPos_nor.y;
        vertices[AZ1] = cPos_nor.z;
        vertices[AX2] = cPos_nor.x;
        vertices[AY2] = cPos_nor.y;
        vertices[AZ2] = cPos_nor.z;
        vertices[AX3] = cPos_nor.x;
        vertices[AY3] = cPos_nor.y;
        vertices[AZ3] = cPos_nor.z;

        vertices[ABX1] = cU_nor.x;
        vertices[ABY1] = cU_nor.y;
        vertices[ABZ1] = cU_nor.z;
        vertices[ABX2] = cU_nor.x;
        vertices[ABY2] = cU_nor.y;
        vertices[ABZ2] = cU_nor.z;
        vertices[ABX3] = cU_nor.x;
        vertices[ABY3] = cU_nor.y;
        vertices[ABZ3] = cU_nor.z;

        vertices[ACX1] = cV_nor.x;
        vertices[ACY1] = cV_nor.y;
        vertices[ACZ1] = cV_nor.z;
        vertices[ACX2] = cV_nor.x;
        vertices[ACY2] = cV_nor.y;
        vertices[ACZ2] = cV_nor.z;
        vertices[ACX3] = cV_nor.x;
        vertices[ACY3] = cV_nor.y;
        vertices[ACZ3] = cV_nor.z;
        
        //Light source coordinates
        Vector3 S_nor = new Vector3(
                MiscFunctions.parameterizeWithDistance(viewBounds.x, viewBounds.width, S.x),
                MiscFunctions.parameterizeWithDistance(viewBounds.y, viewBounds.height, S.y),
                S.z
        );
        vertices[SX1] = S_nor.x;
        vertices[SX2] = S_nor.x;
        vertices[SX3] = S_nor.x;

        vertices[SY1] = S_nor.y;
        vertices[SY2] = S_nor.y;
        vertices[SY3] = S_nor.y;

        vertices[SZ1] = S_nor.z;
        vertices[SZ2] = S_nor.z;
        vertices[SZ3] = S_nor.z;
        
        //Shadow texture coordinates. This is confusing bc normally texture coords are interpolated, but that interpolation happens per fragment based on its own depth/intersection with the shadow plane described by a, b, and c above
        float sh_invTexWid = 1/(float)caster.shadowTexture().getTexture().getWidth(), sh_invTexHgt = 1/(float)caster.shadowTexture().getTexture().getHeight();
//        float sh_u = caster.shadowTexture().getU()*sh_invTexWid, sh_v = caster.shadowTexture().getV()*sh_invTexHgt;
        float sh_w = caster.shadowTexture().getRegionWidth()*sh_invTexWid, sh_h = caster.shadowTexture().getRegionHeight()*sh_invTexHgt;
        float sh_u = caster.shadowTexture().getU(), sh_v = caster.shadowTexture().getV();
        TextureRegion depReg = CentralTextureData.baseToDepthPairs.get(caster.shadowTexture());
        float sh_d = depReg.getU(), sh_e = depReg.getV();
//        float sh_w = caster.shadowTexture().getRegionWidth(), sh_h = caster.shadowTexture().getRegionHeight();
//        float sh_u = 0, sh_v = 0;
//        float sh_w = 1, sh_h = 1;
        vertices[ShU1] = sh_u;
        vertices[ShU2] = sh_u;
        vertices[ShU3] = sh_u;

        vertices[ShV1] = sh_v;
        vertices[ShV2] = sh_v;
        vertices[ShV3] = sh_v;

        vertices[ShW1] = sh_w;
        vertices[ShW2] = sh_w;
        vertices[ShW3] = sh_w;

        vertices[ShH1] = sh_h;
        vertices[ShH2] = sh_h;
        vertices[ShH3] = sh_h;

        vertices[ShD1] = sh_d;
        vertices[ShD2] = sh_d;
        vertices[ShD3] = sh_d;

        vertices[ShE1] = sh_e;
        vertices[ShE2] = sh_e;
        vertices[ShE3] = sh_e;

        Matrix3 toRegCoords = new Matrix3(new float[]{
                cU.x,cV.x,0,
                cU.y,cV.y,0,
                0,0,1
        });
        if (toRegCoords.det() != 0) { //If the shadow surface is fully orthogonal to the screen, there isn't a way for the light to be "inside" it
            Vector3 regCoords = new Vector3(S).sub(cPos).mul(toRegCoords.inv());

//        if (a.x <= 0 && a.y <=0 && c.x >= 0 && c.y >=0) {//The source is inside the parallelogram (infinite prism?) represented by the caster, check whole screen
            if (regCoords.x >= 0 && regCoords.x <= 1 && regCoords.y >= 0 && regCoords.y <= 1) {
                this.pushConvexShadowPolygon(new float[]{
                        S.x - r, S.y - r,
                        S.x - r, S.y + r,
                        S.x + r, S.y + r,
                        S.x + r, S.y - r,
                }, vertices, caster.shadowTexture().getTexture(), depthMap, viewBounds);
                return;
            }
        }

        //Coordinates of shadow casting parallelogram relative to the source
        Vector2 a = new Vector2(cPos.x, cPos.y).sub(S.x, S.y);
        Vector2 b = new Vector2(a).add(cV.x, cV.y);
        Vector2 c = new Vector2(b).add(cU.x, cU.y);
        Vector2 d = new Vector2(c).sub(cV.x, cV.y);

        //Expand out
        float expand = 5.5f;
        Vector2 u_exp = new Vector2(cU.x, cU.y).nor().scl(expand);
        Vector2 v_exp = new Vector2(cV.x, cV.y).nor().scl(expand);
        Vector2 a_exp = new Vector2(a).sub(u_exp).sub(v_exp);
        Vector2 b_exp = new Vector2(b).sub(u_exp).add(v_exp);
        Vector2 c_exp = new Vector2(c).add(u_exp).add(v_exp);
        Vector2 d_exp = new Vector2(d).add(u_exp).sub(v_exp);

        Vector2 zero = new Vector2();
        float r2 = source.radius()*source.radius();
        if (!(
            Intersector.intersectSegmentCircle(a, b, zero, r2) ||
            Intersector.intersectSegmentCircle(b, c, zero, r2) ||
            Intersector.intersectSegmentCircle(c, d, zero, r2) ||
            Intersector.intersectSegmentCircle(d, a, zero, r2)
        )) return; //Don't try to render too far away

        Vector2 farthestLeft = a;
        Vector2 farthestRight = a;
        Vector2 afterFR = b; //Vector after farthest right


        if (cross2D(farthestLeft, b) > 0) farthestLeft = b;
        if (cross2D(farthestLeft, c) > 0) farthestLeft = c;
        if (cross2D(farthestLeft, d) > 0) farthestLeft = d;

        if (cross2D(farthestRight, b) < 0) {
            farthestRight = b;
            afterFR = c;
        }
        if (cross2D(farthestRight, c) < 0) {
            farthestRight = c;
            afterFR = d;
        }
        if (cross2D(farthestRight, d) < 0) {
            farthestRight = d;
            afterFR = a;
        }



        Vector2 maxFL = new Vector2(farthestLeft).nor().scl(source.radius());
        Vector2 maxFR = new Vector2(farthestRight).nor().scl(source.radius());
        Vector2 maxFL_Adj = new Vector2(maxFL).rotate90(-1).add(maxFL);
        Vector2 maxFR_Adj = new Vector2(maxFR).rotate90(+1).add(maxFR);
        Vector2 maxFLR = new Vector2();
        boolean rightLeftAcute = Intersector.intersectLines(maxFL, maxFL_Adj, maxFR, maxFR_Adj, maxFLR);
        Polygon shadowRange;

        a.set(a_exp);
        b.set(b_exp);
        c.set(c_exp);
        d.set(d_exp);

        if (afterFR != farthestLeft) {
            if (!rightLeftAcute) {//Angle > 90⁰, use two tangents to describe max shadow range instead of parallelogram
                shadowRange = new Polygon(new float[]{
                        afterFR.x, afterFR.y,
                        farthestLeft.x, farthestLeft.y,
                        maxFL.x, maxFL.y,
                        maxFL_Adj.x, maxFL_Adj.y,
                        maxFR_Adj.x, maxFR_Adj.y,
                        maxFR.x, maxFR.y,
                        farthestRight.x, farthestRight.y
                });
            } else { //Angle < 90⁰, describe with intersection of the tangents instead of both tangents
                shadowRange = new Polygon(new float[]{
                        afterFR.x, afterFR.y,
                        farthestLeft.x, farthestLeft.y,
                        maxFL.x, maxFL.y,
                        maxFLR.x, maxFLR.y,
                        maxFR.x, maxFR.y,
                        farthestRight.x, farthestRight.y
                });
            }
        } else {
            if (!rightLeftAcute) {//Angle > 90⁰, use two tangents to describe max shadow range instead of parallelogram
                shadowRange=new Polygon(new float[]{
                        farthestLeft.x, farthestLeft.y,
                        maxFL.x, maxFL.y,
                        maxFL_Adj.x, maxFL_Adj.y,
                        maxFR_Adj.x, maxFR_Adj.y,
                        maxFR.x, maxFR.y,
                        farthestRight.x, farthestRight.y
                });
            } else { //Angle < 90⁰, describe with intersection of the tangents instead of both tangents
                shadowRange = new Polygon(new float[]{
                        farthestLeft.x, farthestLeft.y,
                        maxFL.x, maxFL.y,
                        maxFLR.x, maxFLR.y,
                        maxFR.x, maxFR.y,
                        farthestRight.x, farthestRight.y
                });
            }
        }
        shadowRange.translate(source.pos().x, source.pos().y);
        Polygon visRange = new Polygon();
        boolean rangeVisible = Intersector.intersectPolygons(shadowRange, viewPolygonClockwise, visRange);

        if (rangeVisible) {
            float[] rangeVerts = visRange.getTransformedVertices();
            this.pushConvexShadowPolygon(rangeVerts, vertices, caster.shadowTexture().getTexture(), depthMap, viewBounds);
        }
    }

    protected void pushConvexShadowPolygon(float[] polygonVerts, float[] preloadedMeshVerts, Texture shadowTexture, Texture depthMap, Rectangle viewBounds) {
        if (shadowTexture != this.lastShadowTexture) { //If it's using a different shadowTexture than before
            this.switchShadowTexture(shadowTexture);
        }

        preloadedMeshVerts[X1] = polygonVerts[0];
        preloadedMeshVerts[Y1] = polygonVerts[1];

        //It might seem redundant to assign depth map coordinates, but if each light is drawn on a distinct channel then the frag coordinates correspond to that channel, and not the overall screen
        preloadedMeshVerts[D1] = MiscFunctions.parameterizeWithDistance(viewBounds.x, viewBounds.width, polygonVerts[0]);
        preloadedMeshVerts[E1] = MiscFunctions.parameterizeWithDistance(viewBounds.y, viewBounds.height, polygonVerts[1]);

        for (int i = 2; i < polygonVerts.length-2; i+=2) {
            int j = i+2;

            preloadedMeshVerts[X2] = polygonVerts[i];
            preloadedMeshVerts[Y2] = polygonVerts[i+1];

            preloadedMeshVerts[X3] = polygonVerts[j];
            preloadedMeshVerts[Y3] = polygonVerts[j+1];

            preloadedMeshVerts[D2] = MiscFunctions.parameterizeWithDistance(viewBounds.x, viewBounds.width, polygonVerts[i]);
            preloadedMeshVerts[E2] = MiscFunctions.parameterizeWithDistance(viewBounds.y, viewBounds.height, polygonVerts[i+1]);

            preloadedMeshVerts[D3] = MiscFunctions.parameterizeWithDistance(viewBounds.x, viewBounds.width, polygonVerts[j]);
            preloadedMeshVerts[E3] = MiscFunctions.parameterizeWithDistance(viewBounds.y, viewBounds.height, polygonVerts[j+1]);

            this.draw(depthMap, preloadedMeshVerts, 0, SHADOW_SPRITE_SIZE);
        }
    }

    protected void switchShadowTexture (Texture shadowTexture) {
        flush();
        this.lastShadowTexture = shadowTexture;
        invShadTexWidth = 1.0f / shadowTexture.getWidth();
        invShadTexHeight = 1.0f / shadowTexture.getHeight();
    }

    @Override
    public void end() {
        super.end();
    }

    public float[] verticesOfShadowRegion(){return null;}

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
        shaderToSet.setUniformi(DMAPTEX_UNIFORM, 0);
        shaderToSet.setUniformi(SHADTEX_UNIFORM, 1);

    }

    public void setViewport(Viewport viewport) {
        if (this.loc_u_viewDims == -10) this.loc_u_viewDims = this.shader.fetchUniformLocation("u_viewDims", true);
        this.shader.setUniform2fv(this.loc_u_viewDims, new float[]{viewport.getWorldWidth(), viewport.getWorldHeight()}, 0, 2);
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
    public void begin() {
//        this.enableBlending();
        super.begin();
    }

    @Override
    public void flush() {
        if (idx == 0) return;

        renderCalls++;
        totalRenderCalls++;
        int spritesInBatch = idx / spriteSize;
        if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
        int count = spritesInBatch * this.indicesPerSprite;

        lastShadowTexture.bind(1);
        lastTexture.bind(0);

        ShaderProgram shaderToUse = customShader != null ? customShader : shader;
        shaderToUse.setUniform2fv(SHADPXDIM_UNIFORM, new float[]{
                invShadTexWidth, invShadTexHeight
        }, 0, 2);
        shaderToUse.setUniformi(SHADTEXWIDTH_UNIFORM, this.lastShadowTexture.getWidth());
        shaderToUse.setUniformi(SHADTEXHEIGHT_UNIFORM, this.lastShadowTexture.getHeight());
        shaderToUse.setUniformf("u_elapsedTime", GameScreen.elapsedTime);

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

        mesh.render(shaderToUse, GL20.GL_TRIANGLES, 0, count);

        idx = 0;
    }
}

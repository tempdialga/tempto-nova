package com.mygdx.tempto.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.tempto.util.MiscFunctions;

import java.util.Arrays;
import java.util.Vector;

public class AltShadeBatch extends AltBatch {

    protected final static int SHADOW_SPRITE_SIZE = 54;

    protected final static String SHADOWVERT_PATH_INTERNAL = "shaders/shadeVert.glsl";
    protected final static String SHADOWFRAG_PATH_INTERNAL = "shaders/shadeFrag.glsl";

    protected final static String DEPTHMAPCOORD_ATTRIBUTE = AltDepthBatch.DEPCOORD_ATTRIBUTE;
    protected final static String SHADOWTEXCOORD_ATTRIBUTE = "a_shadTexCoord";

    protected final static String DMAPTEX_UNIFORM = "u_dMapTex";
    protected final static String SHADTEX_UNIFORM = "u_shadTex";

    protected final static String   A_ATTRIBUTE = "u_a",
                                    AB_ATTRIBUTE = "u_ab",
                                    AC_ATTRIBUTE = "u_ac",
                                    S_ATTRIBUTE = "u_S";

    private static int i=0;
    public static final int X1 = i++, Y1 = i++, D1 = i++, E1 = i++, ShU1 = i++, ShV1 = i++, AX1 = i++, AY1 = i++, AZ1 = i++, ABX1 = i++, ABY1 = i++, ABZ1 = i++, ACX1 = i++, ACY1 = i++, ACZ1 = i++, SX1 = i++, SY1 = i++, SZ1 = i++,
                            X2 = i++, Y2 = i++, D2 = i++, E2 = i++, ShU2 = i++, ShV2 = i++, AX2 = i++, AY2 = i++, AZ2 = i++, ABX2 = i++, ABY2 = i++, ABZ2 = i++, ACX2 = i++, ACY2 = i++, ACZ2 = i++, SX2 = i++, SY2 = i++, SZ2 = i++,
                            X3 = i++, Y3 = i++, D3 = i++, E3 = i++, ShU3 = i++, ShV3 = i++, AX3 = i++, AY3 = i++, AZ3 = i++, ABX3 = i++, ABY3 = i++, ABZ3 = i++, ACX3 = i++, ACY3 = i++, ACZ3 = i++, SX3 = i++, SY3 = i++, SZ3 = i++;

    protected static ShaderProgram shadowShader;
    protected static ShaderProgram lightShader;


    protected static Mesh.VertexDataType meshVertexDataType = (Gdx.gl30 != null) ? Mesh.VertexDataType.VertexBufferObjectWithVAO : defaultVertexDataType;

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


        //Start by drawing just a triangle on half of the shadow to make sure we set it up right
        Vector3 cPos = caster.origin();
        Vector3 cU = caster.u();
        Vector3 cV = caster.v();

        //Coordinates of shadow casting parallelogram relative to the source
        Vector2 a = new Vector2(cPos.x, cPos.y).sub(source.pos().x, source.pos().y);
        Vector2 b = new Vector2(a).add(cV.x, cV.y);
        Vector2 c = new Vector2(b).add(cU.x, cU.y);
        Vector2 d = new Vector2(c).sub(cV.x, cV.y);

        if (Math.min(Math.min(a.len(), b.len()), Math.min(c.len(), d.len())) > source.radius()) return; //Don't try to render too far away

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



        float x = MiscFunctions.parameterizeWithDistance(viewBounds.x, viewBounds.width, cPos.x);
        float y = MiscFunctions.parameterizeWithDistance(viewBounds.y, viewBounds.height, cPos.y);
        Vector3 cU_screen = new Vector3(cU).scl(1/viewBounds.width, 1/viewBounds.height, 1);
        Vector3 cV_screen = new Vector3(cV).scl(1/viewBounds.width, 1/viewBounds.height, 1);

        float[] vertices = new float[SHADOW_SPRITE_SIZE];

//        vertices[X1] = cPos.x;
//        vertices[Y1] = cPos.y;
//
//        vertices[X2] = cPos.x+cV.x; //Typically cV is only y, ergo to wind clockwise like we normally do
//        vertices[Y2] = cPos.y+cV.y;
//
//        vertices[X3] = cPos.x+cU.x;
//        vertices[Y3] = cPos.y+cU.y;

        Vector2 maxFL = new Vector2(farthestLeft).nor().scl(source.radius());
        Vector2 maxFR = new Vector2(farthestRight).nor().scl(source.radius());
        Vector2 maxFL_Adj = new Vector2(maxFL).rotate90(-1).add(maxFL);
        Vector2 maxFR_Adj = new Vector2(maxFR).rotate90(+1).add(maxFR);
        Vector2 maxFLR = new Vector2();
        boolean rightLeftAcute = Intersector.intersectLines(maxFL, maxFL_Adj, maxFR, maxFR_Adj, maxFLR);
        Polygon shadowRange;
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
            System.out.println("Range # "+(ShadowCaster.numRangesVisible++)+" visible!");
//            caster
            float[] rangeVerts = visRange.getTransformedVertices();
            for (int i = 2; i < rangeVerts.length-2; i+=2) {
                int j = i+2;
                vertices = new float[SHADOW_SPRITE_SIZE];

                vertices[X1] = rangeVerts[0];
                vertices[Y1] = rangeVerts[1];

                vertices[X2] = rangeVerts[i];
                vertices[Y2] = rangeVerts[i+1];

                vertices[X3] = rangeVerts[j];
                vertices[Y3] = rangeVerts[j+1];

//                vertices[X3+18] = rangeVerts[j];
//                vertices[Y3+18] = rangeVerts[j+1];

                this.draw(caster.shadowTexture().getTexture(), vertices, 0, SHADOW_SPRITE_SIZE);
            }
        }
//        if (true) return;
//
//        if (afterFR == farthestLeft) {
////            vertices[X1] = source.pos().x;
////            vertices[Y1] = source.pos().y;
//            vertices[X1] = afterFR.x-1;
//            vertices[Y1] = afterFR.y-1;
//        } else {
//            vertices[X1] = afterFR.x;
//            vertices[Y1] = afterFR.y;
//        }
//        vertices[X1] += source.pos().x;
//        vertices[Y1] += source.pos().y;
//
//
//        vertices[X2] = farthestLeft.x+source.pos().x; //Typically cV is only y, ergo to wind clockwise like we normally do
//        vertices[Y2] = farthestLeft.y+source.pos().y;
//
//        vertices[X3] = farthestRight.x+source.pos().x;
//        vertices[Y3] = farthestRight.y+source.pos().y;
//
//
////        vertices[X3] = cPos.x+cU.x+cV.x;
////        vertices[Y3] = cPos.y+cU.y+cV.y;
//
////        System.out.println("Caster: "+Arrays.toString(vertices));
//
//
//        this.draw(caster.shadowTexture().getTexture(), vertices, 0, SHADOW_SPRITE_SIZE);
    }

    @Override
    public void end() {
        super.end();
    }

    public float[] verticesOfShadowRegion(){return null;}

    @Override
    protected void setupMatrices () {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        if (customShader != null) {
            customShader.setUniformMatrix("u_projTrans", combinedMatrix);
            customShader.setUniformi(DMAPTEX_UNIFORM, 0);
//            customShader.setUniformi(SHADTEX_UNIFORM, 1);
        } else {
            shader.setUniformMatrix("u_projTrans", combinedMatrix);
            shader.setUniformi(DMAPTEX_UNIFORM, 0);
//            shader.setUniformi(SHADTEX_UNIFORM, 1);
        }
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
        super.flush();
        System.out.println("Mesh size on flush: " + this.mesh.getNumVertices());
    }
}

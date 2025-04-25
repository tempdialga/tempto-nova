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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.tempto.TemptoNova;

public class AltLightBatch extends AltBatch{

    public static final int LIGHT_SPRITE_SIZE = 20;

    public static final String LIGHTCOORD_ATTRIBUTE = "a_lightCoord";

    public static final String LIGHT_VERT_PATH_INTERNAL = "shaders/lightVert.glsl", LIGHT_FRAG_PATH_INTERNAL = "shaders/lightFrag.glsl";

    private static int i=0;
    public static final int X1 = i++, Y1 = i++, /*U1 = i++, V1 = i++,*/ A1 = i++, B1 = i++, C1 = i++,
                            X2 = i++, Y2 = i++, /*U2 = i++, V2 = i++,*/ A2 = i++, B2 = i++, C2 = i++,
                            X3 = i++, Y3 = i++, /*U3 = i++, V3 = i++,*/ A3 = i++, B3 = i++, C3 = i++,
                            X4 = i++, Y4 = i++, /*U4 = i++, V4 = i++,*/ A4 = i++, B4 = i++, C4 = i++;

    protected int loc_u_S;//Location of the uniform for u_S (light source)
    protected int loc_u_viewDims = -10;//Location of the uniform for u_viewDims (dimensions of the view window)

    public AltLightBatch() {
        this(1000, null);
    }
    public AltLightBatch(int size, ShaderProgram defaultShader) {
        super(size, defaultShader, new Mesh((Gdx.gl30 != null) ? Mesh.VertexDataType.VertexBufferObjectWithVAO : defaultVertexDataType, false, size * 4, size * 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
//                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, AltDepthBatch.DEPCOORD_ATTRIBUTE + "0"),
                new VertexAttribute(VertexAttributes.Usage.Position, 3, LIGHTCOORD_ATTRIBUTE+"0")), LIGHT_SPRITE_SIZE);
    }

    public void setViewport(Viewport viewport) {
        if (this.loc_u_viewDims == -10) this.loc_u_viewDims = this.shader.fetchUniformLocation("u_viewDims", true);
        this.shader.setUniform2fv(this.loc_u_viewDims, new float[]{viewport.getWorldWidth(), viewport.getWorldHeight()}, 0, 2);
    }

    @Override
    protected ShaderProgram createDefaultShader() {
        return new ShaderProgram(Gdx.files.internal(LIGHT_VERT_PATH_INTERNAL), Gdx.files.internal(LIGHT_FRAG_PATH_INTERNAL));
    }

    public void drawLight(LightSource source, Texture depthMap, OrthographicCamera camera) {
        float radius = 50;
        Vector3 p = source.pos();
        Vector3 p_screen = camera.project(new Vector3(p));
        p_screen.x /= (float) TemptoNova.PIXEL_GAME_WIDTH;
        p_screen.y /= (float) TemptoNova.PIXEL_GAME_HEIGHT;
        p_screen.z = p.z;
        p_screen.scl(0.5f,0.5f,1);

        float l = p.x-radius, r=p.x+radius, u=p.y+radius, d=p.y-radius;
        float u1 = 0, v1 = 0, u2 = 1, v2 = 1; //Uh wait actually do we need these
        float[] verts = new float[LIGHT_SPRITE_SIZE];
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

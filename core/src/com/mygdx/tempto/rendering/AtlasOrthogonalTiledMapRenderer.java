package com.mygdx.tempto.rendering;

import static com.badlogic.gdx.graphics.g2d.Batch.C1;
import static com.badlogic.gdx.graphics.g2d.Batch.C2;
import static com.badlogic.gdx.graphics.g2d.Batch.C3;
import static com.badlogic.gdx.graphics.g2d.Batch.C4;
import static com.badlogic.gdx.graphics.g2d.Batch.U1;
import static com.badlogic.gdx.graphics.g2d.Batch.U2;
import static com.badlogic.gdx.graphics.g2d.Batch.U3;
import static com.badlogic.gdx.graphics.g2d.Batch.U4;
import static com.badlogic.gdx.graphics.g2d.Batch.V1;
import static com.badlogic.gdx.graphics.g2d.Batch.V2;
import static com.badlogic.gdx.graphics.g2d.Batch.V3;
import static com.badlogic.gdx.graphics.g2d.Batch.V4;
import static com.badlogic.gdx.graphics.g2d.Batch.X1;
import static com.badlogic.gdx.graphics.g2d.Batch.X2;
import static com.badlogic.gdx.graphics.g2d.Batch.X3;
import static com.badlogic.gdx.graphics.g2d.Batch.X4;
import static com.badlogic.gdx.graphics.g2d.Batch.Y1;
import static com.badlogic.gdx.graphics.g2d.Batch.Y2;
import static com.badlogic.gdx.graphics.g2d.Batch.Y3;
import static com.badlogic.gdx.graphics.g2d.Batch.Y4;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.mygdx.tempto.data.CentralTextureData;

import org.lwjgl.Sys;

import java.util.Iterator;

/**An impromptu class that hot swaps discrete texture references to the equivalent location on the {@link CentralTextureData} texture atlas.
 *
 * Also passes 7d vertices, instead of 5d, the extra 2 being depth map coordinates*/
public class AtlasOrthogonalTiledMapRenderer extends OrthogonalTiledMapRenderer {

    public static final int NUM_VERTICES_WITH_DEPTH = BatchTiledMapRenderer.NUM_VERTICES+8;
    protected boolean usesDepthMap;


    /**The base depth map value to use. This should be set for each texture, at least the r value (depth of that texture)*/
    public Color currentBaseDepth = new Color(0.5f,0.5f,0.5f,1f);


    public AtlasOrthogonalTiledMapRenderer(TiledMap map, Batch batch, boolean usesDepthMap) {
        super(map, batch);
        this.usesDepthMap = usesDepthMap;
        if (usesDepthMap) {
            this.vertices = new float[NUM_VERTICES_WITH_DEPTH];
        }
    }


    @Override
    public void renderTileLayer (TiledMapTileLayer layer) {
//        final Color batchColor = batch.getColor();

        final float color = Color.toFloatBits(this.currentBaseDepth.r, this.currentBaseDepth.g, this.currentBaseDepth.b, this.currentBaseDepth.a * layer.getOpacity());

        final int layerWidth = layer.getWidth();
        final int layerHeight = layer.getHeight();

        final float layerTileWidth = layer.getTileWidth() * unitScale;
        final float layerTileHeight = layer.getTileHeight() * unitScale;

        final float layerOffsetX = layer.getRenderOffsetX() * unitScale - viewBounds.x * (layer.getParallaxX() - 1);
        // offset in tiled is y down, so we flip it
        final float layerOffsetY = -layer.getRenderOffsetY() * unitScale - viewBounds.y * (layer.getParallaxY() - 1);

        final int col1 = Math.max(0, (int)((viewBounds.x - layerOffsetX) / layerTileWidth));
        final int col2 = Math.min(layerWidth,
                (int)((viewBounds.x + viewBounds.width + layerTileWidth - layerOffsetX) / layerTileWidth));

        final int row1 = Math.max(0, (int)((viewBounds.y - layerOffsetY) / layerTileHeight));
        final int row2 = Math.min(layerHeight,
                (int)((viewBounds.y + viewBounds.height + layerTileHeight - layerOffsetY) / layerTileHeight));

        float y = row2 * layerTileHeight + layerOffsetY;
        float xStart = col1 * layerTileWidth + layerOffsetX;
        final float[] vertices = this.vertices;

        for (int row = row2; row >= row1; row--) {
            float x = xStart;
            for (int col = col1; col < col2; col++) {
                final TiledMapTileLayer.Cell cell = layer.getCell(col, row);
                if (cell == null) {
                    x += layerTileWidth;
                    continue;
                }
                final TiledMapTile tile = cell.getTile();

                if (tile != null) {
                    final boolean flipX = cell.getFlipHorizontally();
                    final boolean flipY = cell.getFlipVertically();
                    final int rotations = cell.getRotation();

                    TextureRegion region = tile.getTextureRegion();
//                    String sourceRegion = tile.getProperties().get("sourceCTD",String.class);
//                    TextureAtlas.AtlasRegion tileSetRegion = CentralTextureData.getRegion(sourceRegion);
//                    TextureRegion nestedRegion = new TextureRegion(tileSetRegion, region.getRegionX(), region.getRegionY(), region.getRegionWidth(), region.getRegionHeight());
//                    region = nestedRegion;

                    float x1 = x + tile.getOffsetX() * unitScale;
                    float y1 = y + tile.getOffsetY() * unitScale;
                    float x2 = x1 + region.getRegionWidth() * unitScale;
                    float y2 = y1 + region.getRegionHeight() * unitScale;

                    float u1 = region.getU();
                    float v1 = region.getV2();
                    float u2 = region.getU2();
                    float v2 = region.getV();
                    

                    //Vertices have slightly different indices if using depth shader, because depth shader uses 7 components instead of 5
                    int iX1 = X1, iY1 = Y1, iC1=C1, iU1=U1, iV1=V1,
                        iX2 = X2, iY2 = Y2, iC2=C2, iU2=U2, iV2=V2,
                        iX3 = X3, iY3 = Y3, iC3=C3, iU3=U3, iV3=V3,
                        iX4 = X4, iY4 = Y4, iC4=C4, iU4=U4, iV4=V4;
                    if (this.usesDepthMap) {
                        iX2 = Depth.X2; iY2 = Depth.Y2; iC2 = Depth.C2; iU2 = Depth.U2; iV2 = Depth.V2;
                        iX3 = Depth.X3; iY3 = Depth.Y3; iC3 = Depth.C3; iU3 = Depth.U3; iV3 = Depth.V3;
                        iX4 = Depth.X4; iY4 = Depth.Y4; iC4 = Depth.C4; iU4 = Depth.U4; iV4 = Depth.V4;
                    }
                    int iD1 = Depth.D1, iE1 = Depth.E1,
                        iD2 = Depth.D2, iE2 = Depth.E2,
                        iD3 = Depth.D3, iE3 = Depth.E3,
                        iD4 = Depth.D4, iE4 = Depth.E4;

                    vertices[iX1] = x1;
                    vertices[iY1] = y1;
                    vertices[iC1] = color;
                    vertices[iU1] = u1;
                    vertices[iV1] = v1;

                    vertices[iX2] = x1;
                    vertices[iY2] = y2;
                    vertices[iC2] = color;
                    vertices[iU2] = u1;
                    vertices[iV2] = v2;

                    vertices[iX3] = x2;
                    vertices[iY3] = y2;
                    vertices[iC3] = color;
                    vertices[iU3] = u2;
                    vertices[iV3] = v2;

                    vertices[iX4] = x2;
                    vertices[iY4] = y1;
                    vertices[iC4] = color;
                    vertices[iU4] = u2;
                    vertices[iV4] = v1;
                    
                    //If using the depth map, also assign depth coordinates
                    if (this.usesDepthMap) {
//                        MapProperties tileProps = tile.getProperties();
//                        String sourceCTD = tileProps.get("sourceCTD", String.class);
//                        System.out.println("Pair exists? "+CentralTextureData.baseToDepthPairs.containsKey(region));
                        TextureRegion depthRegion = CentralTextureData.baseToDepthPairs.get(region);
                        if (depthRegion == null) depthRegion = CentralTextureData.defaultDepthRegion;
//                        depthRegion = CentralTextureData.defaultDepthRegion;

                        float d1 = depthRegion.getU();
                        float e1 = depthRegion.getV2();
                        float d2 = depthRegion.getU2();
                        float e2 = depthRegion.getV();


                        vertices[iD1] = d1;
                        vertices[iE1] = e1;

                        vertices[iD2] = d1;
                        vertices[iE2] = e2;

                        vertices[iD3] = d2;
                        vertices[iE3] = e2;

                        vertices[iD4] = d2;
                        vertices[iE4] = e1;

//                        vertices[iD1] = d1;
//                        vertices[iE1] = e1;
//
//                        vertices[iD2] = d1;
//                        vertices[iE2] = e2;
//
//                        vertices[iD3] = d2;
//                        vertices[iE3] = e2;
//
//                        vertices[iD4] = d2;
//                        vertices[iE4] = e1;
                    }

                    if (flipX) {
                        float temp = vertices[iU1];
                        vertices[iU1] = vertices[iU3];
                        vertices[iU3] = temp;
                        temp = vertices[iU2];
                        vertices[iU2] = vertices[iU4];
                        vertices[iU4] = temp;
                    }
                    if (flipY) {
                        float temp = vertices[iV1];
                        vertices[iV1] = vertices[iV3];
                        vertices[iV3] = temp;
                        temp = vertices[iV2];
                        vertices[iV2] = vertices[iV4];
                        vertices[iV4] = temp;
                    }
                    if (rotations != 0) {
                        switch (rotations) {
                            case TiledMapTileLayer.Cell.ROTATE_90: {
                                float tempV = vertices[iV1];
                                vertices[iV1] = vertices[iV2];
                                vertices[iV2] = vertices[iV3];
                                vertices[iV3] = vertices[iV4];
                                vertices[iV4] = tempV;

                                float tempU = vertices[iU1];
                                vertices[iU1] = vertices[iU2];
                                vertices[iU2] = vertices[iU3];
                                vertices[iU3] = vertices[iU4];
                                vertices[iU4] = tempU;
                                break;
                            }
                            case TiledMapTileLayer.Cell.ROTATE_180: {
                                float tempU = vertices[iU1];
                                vertices[iU1] = vertices[iU3];
                                vertices[iU3] = tempU;
                                tempU = vertices[iU2];
                                vertices[iU2] = vertices[iU4];
                                vertices[iU4] = tempU;
                                float tempV = vertices[iV1];
                                vertices[iV1] = vertices[iV3];
                                vertices[iV3] = tempV;
                                tempV = vertices[iV2];
                                vertices[iV2] = vertices[iV4];
                                vertices[iV4] = tempV;
                                break;
                            }
                            case TiledMapTileLayer.Cell.ROTATE_270: {
                                float tempV = vertices[iV1];
                                vertices[iV1] = vertices[iV4];
                                vertices[iV4] = vertices[iV3];
                                vertices[iV3] = vertices[iV2];
                                vertices[iV2] = tempV;

                                float tempU = vertices[iU1];
                                vertices[iU1] = vertices[iU4];
                                vertices[iU4] = vertices[iU3];
                                vertices[iU3] = vertices[iU2];
                                vertices[iU2] = tempU;
                                break;
                            }
                        }
                    }
                    batch.draw(region.getTexture(), vertices, 0, this.usesDepthMap? NUM_VERTICES_WITH_DEPTH : NUM_VERTICES);
                }
                x += layerTileWidth;
            }
            y -= layerTileHeight;
        }
    }

    /**Vertex information for rendering to the depth map enabled shader, which includes vertex info of both the original texture region, and the depth map region, which should both be of the same texture (since this uses an atlas)
     *
     * U, V: Texture coordinates of the main texture region
     * D, E: Texture coordinates of the corresponding depth texture region. Could be the default depth map.
     *
     * Both must be passed because rendering to the depth map reuses the alpha from the main texture*/
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

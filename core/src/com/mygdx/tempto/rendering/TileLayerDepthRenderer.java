package com.mygdx.tempto.rendering;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.tempto.data.CentralTextureData;
import com.mygdx.tempto.entity.decoration.TileLayer;
import com.mygdx.tempto.rendering.AltBatch.Depth;

public class TileLayerDepthRenderer extends TileLayerRenderer{
    public static final int NUM_VERTICES_WITH_DEPTH = BatchTiledMapRenderer.NUM_VERTICES+8;
    public TileLayerDepthRenderer(TiledMap map, AltDepthBatch batch) {
        super(map, batch);
    }

    /**Depth color encoding:
     * r: 1/depth
     * g: x of normal vector, scaled from [-1 1] to [0 1], so it's half precision and relative to 0.5 instead of 0
     * b: y of normal vector, scaled just as with x
     * a: reflectivity, scale TBD*/
    @Override
    float packedColorForLayer(TileLayer layer) {
        return packedDepthColor(layer.getBaseDepth(), layer.getBaseNormVec(), 0.01f/8f);
//        return Color.toFloatBits(1/(layer.getBaseDepth()), layer.getBaseNormVec().x*0.5f+0.5f, layer.getBaseNormVec().y*0.5f+0.5f, 0.01f/8f);
    }



    @Override
    void drawTile(TileLayer originalLayer, TiledMapTileLayer.Cell cell, float x, float y, float w, float h, float[] vertices, float[] depthColors) {
//        float refl_coeff = 0.99f/8f;
//        float flatColor = packedColorForLayer(originalLayer);
        float colorPackedA = depthColors[0], colorPackedB = depthColors[1], colorPackedC = depthColors[2], colorPackedD = depthColors[3];
//        if (originalLayer.isRotate()) {
//            Vector3 altNormVec = new Vector3(0.707f*16, -w, 0).nor(); //TEMPORARY: This uses y to represent z since y of the normal vec for debugging is 0
//            altNormVec.y = 0;
//
//            float frontColor = packedDepthColor(originalLayer.getBaseDepth(), altNormVec, refl_coeff);
//            colorPackedA = frontColor;
//            colorPackedB = frontColor;
//            float backColor = packedDepthColor(originalLayer.getBaseDepth()+0.707f*16, altNormVec, refl_coeff);
//            colorPackedC = backColor;
//            colorPackedD = backColor;
//        } else {
//            colorPackedA = flatColor;
//            colorPackedB = flatColor;
//            colorPackedC = flatColor;
//            colorPackedD = flatColor;
//        }


        TiledMapTile tile = cell.getTile();

        final boolean flipX = cell.getFlipHorizontally();
        final boolean flipY = cell.getFlipVertically();
        final int rotations = cell.getRotation();

        TextureRegion region = tile.getTextureRegion();

        float x1 = x + tile.getOffsetX() * unitScale;
        float y1 = y + tile.getOffsetY() * unitScale;
        float x2 = x1 + w * unitScale;
        float y2 = y1 + h * unitScale;

        float u1 = region.getU();
        float v1 = region.getV2();
        float u2 = region.getU2();
        float v2 = region.getV();

        //Since using the depth map, also assign depth coordinates
        TextureRegion depthRegion = CentralTextureData.baseToDepthPairs.get(region);
        if (depthRegion == null) depthRegion = CentralTextureData.defaultDepthRegion;

        float d1 = depthRegion.getU();
        float e1 = depthRegion.getV2();
        float d2 = depthRegion.getU2();
        float e2 = depthRegion.getV();

        //Vertices have slightly different indices if using depth shader, because depth shader uses 7 components instead of 5
        int iX1 = Depth.X1, iY1 = Depth.Y1, iC1 = Depth.C1, iU1 = Depth.U1, iV1 = Depth.V1, iD1 = Depth.D1, iE1 = Depth.E1,
                iX2 = Depth.X2, iY2 = Depth.Y2, iC2 = Depth.C2, iU2 = Depth.U2, iV2 = Depth.V2, iD2 = Depth.D2, iE2 = Depth.E2,
                iX3 = Depth.X3, iY3 = Depth.Y3, iC3 = Depth.C3, iU3 = Depth.U3, iV3 = Depth.V3, iD3 = Depth.D3, iE3 = Depth.E3,
                iX4 = Depth.X4, iY4 = Depth.Y4, iC4 = Depth.C4, iU4 = Depth.U4, iV4 = Depth.V4, iD4 = Depth.D4, iE4 = Depth.E4;

        vertices[iX1] = x1;
        vertices[iY1] = y1;
        vertices[iC1] = colorPackedA;
        vertices[iU1] = u1;
        vertices[iV1] = v1;
        vertices[iD1] = d1;
        vertices[iE1] = e1;

        vertices[iX2] = x1;
        vertices[iY2] = y2;
        vertices[iC2] = colorPackedB;
        vertices[iU2] = u1;
        vertices[iV2] = v2;
        vertices[iD2] = d1;
        vertices[iE2] = e2;

        vertices[iX3] = x2;
        vertices[iY3] = y2;
        vertices[iC3] = colorPackedC;
        vertices[iU3] = u2;
        vertices[iV3] = v2;
        vertices[iD3] = d2;
        vertices[iE3] = e2;

        vertices[iX4] = x2;
        vertices[iY4] = y1;
        vertices[iC4] = colorPackedD;
        vertices[iU4] = u2;
        vertices[iV4] = v1;
        vertices[iD4] = d2;
        vertices[iE4] = e1;

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
        batch.draw(region.getTexture(), vertices, 0, NUM_VERTICES_WITH_DEPTH);
    }
}

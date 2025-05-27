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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.mygdx.tempto.entity.decoration.TileLayer;

public class TileLayerFinalRenderer extends TileLayerRenderer{



    public static final int NUM_VERTICES_FOR_FINAL = BatchTiledMapRenderer.NUM_VERTICES;
    public TileLayerFinalRenderer(TiledMap map, AltFinalBatch batch) {

        super(map, batch);
    }

    @Override
    float packedColorForLayer(TileLayer layer) {
        Color c = this.batch.getColor();
        return Color.toFloatBits(c.r, c.g, c.b, c.a);
    }

    @Override
    void drawTile(TileLayer originalLayer, TiledMapTileLayer.Cell cell, float x, float y, float w, float h, float[] vertices, float[] depthColors) {
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


        //Vertices have slightly different indices if using depth shader, because depth shader uses 7 components instead of 5
        int iX1 = X1, iY1 = Y1, iC1=C1, iU1=U1, iV1=V1,
                iX2 = X2, iY2 = Y2, iC2=C2, iU2=U2, iV2=V2,
                iX3 = X3, iY3 = Y3, iC3=C3, iU3=U3, iV3=V3,
                iX4 = X4, iY4 = Y4, iC4=C4, iU4=U4, iV4=V4;

        vertices[iX1] = x1;
        vertices[iY1] = y1;
        vertices[iC1] = depthColors[0];
        vertices[iU1] = u1;
        vertices[iV1] = v1;

        vertices[iX2] = x1;
        vertices[iY2] = y2;
        vertices[iC2] = depthColors[1];
        vertices[iU2] = u1;
        vertices[iV2] = v2;

        vertices[iX3] = x2;
        vertices[iY3] = y2;
        vertices[iC3] = depthColors[2];
        vertices[iU3] = u2;
        vertices[iV3] = v2;

        vertices[iX4] = x2;
        vertices[iY4] = y1;
        vertices[iC4] = depthColors[3];
        vertices[iU4] = u2;
        vertices[iV4] = v1;


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
        batch.draw(region.getTexture(), vertices, 0, NUM_VERTICES);
    }


}

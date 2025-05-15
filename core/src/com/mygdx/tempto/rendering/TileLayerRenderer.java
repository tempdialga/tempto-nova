package com.mygdx.tempto.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.mygdx.tempto.data.CentralTextureData;
import com.mygdx.tempto.entity.decoration.TileLayer;

/**An impromptu class that hot swaps discrete texture references to the equivalent location on the {@link CentralTextureData} texture atlas.
 *
 * Also passes 7d vertices, instead of 5d, the extra 2 being depth map coordinates*/
public abstract class TileLayerRenderer extends OrthogonalTiledMapRenderer {

    public static final int NUM_VERTICES_WITH_DEPTH = BatchTiledMapRenderer.NUM_VERTICES+8;


    /**The base depth map value to use. This should be set for each texture, at least the r value (depth of that texture)*/
    public Color currentBaseDepth = new Color(0.5f,0.5f,0.5f,1f);


    public TileLayerRenderer(TiledMap map, AltBatch batch) {
        super(map, batch);
        this.vertices = new float[batch.spriteSize];
    }


    @Override
    public void renderTileLayer (TiledMapTileLayer layer) {
//        final Color batchColor = batch.getColor();

        final float color = Color.toFloatBits(this.currentBaseDepth.r, this.currentBaseDepth.g, this.currentBaseDepth.b, this.currentBaseDepth.a);

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

                }
                x += layerTileWidth;
            }
            y -= layerTileHeight;
        }
    }

    public void renderTileLayer(TileLayer layer) {
//        if (layer.isRotate()) return;//For now don't try to render rotated ones, they're only for shadows
        final float color = this.packedColorForLayer(layer);
        final float[] vertices = this.vertices;
        TiledMapTileLayer tiledLayer = layer.getMapLayer();

        final int layerWidth = tiledLayer.getWidth();
        final int layerHeight = tiledLayer.getHeight();

        final float layerTileWidth = tiledLayer.getTileWidth() * unitScale;
        final float layerTileHeight = tiledLayer.getTileHeight() * unitScale;

        final float layerOffsetX = tiledLayer.getRenderOffsetX() * unitScale - viewBounds.x * (tiledLayer.getParallaxX() - 1);
        // offset in tiled is y down, so we flip it
        final float layerOffsetY = -tiledLayer.getRenderOffsetY() * unitScale - viewBounds.y * (tiledLayer.getParallaxY() - 1);

        final int col1 = Math.max(0, (int)((viewBounds.x - layerOffsetX) / layerTileWidth));
        final int col2 = Math.min(layerWidth,
                (int)((viewBounds.x + viewBounds.width + layerTileWidth - layerOffsetX) / layerTileWidth));

        final int row1 = Math.max(0, (int)((viewBounds.y - layerOffsetY) / layerTileHeight));
        final int row2 = Math.min(layerHeight,
                (int)((viewBounds.y + viewBounds.height + layerTileHeight - layerOffsetY) / layerTileHeight));

        float y = row2 * layerTileHeight + layerOffsetY;
        float xStart = col1 * layerTileWidth + layerOffsetX;


        for (int row = row2; row >= row1; row--) {
            float x = xStart;
            for (int col = col1; col < col2; col++) {
                final TiledMapTileLayer.Cell cell = tiledLayer.getCell(col, row);
                if (cell == null) {
                    x += layerTileWidth;
                    continue;
                }
                final TiledMapTile tile = cell.getTile();

                if (tile != null) {
                    float w = cell.getTile().getTextureRegion().getRegionWidth(), h = cell.getTile().getTextureRegion().getRegionHeight();
                    if (layer.isRotate()) {
                        w *= (float) Math.cos(Math.toRadians(45));
                    }

                    this.drawTile(layer, cell, x, y, w, h, vertices, color);
                }
                x += layerTileWidth;
            }
            y -= layerTileHeight;
        }
    }

    abstract float packedColorForLayer(TileLayer layer);

    /**Renders a specific tile.
     * Between different rendering stages (Drawing to the depth map, to the shadow/light map, the final pass), the loop itself doesn't change,
     * just the piece of code to render each tile.*/
    abstract void drawTile(TileLayer originalLayer, TiledMapTileLayer.Cell cell, float x, float y, float w, float h, float[] vertices, float color);




    /**Vertex information for rendering to the depth map enabled shader, which includes vertex info of both the original texture region, and the depth map region, which should both be of the same texture (since this uses an atlas)
     *
     * U, V: Texture coordinates of the main texture region
     * D, E: Texture coordinates of the corresponding depth texture region. Could be the default depth map.
     *
     * Both must be passed because rendering to the depth map reuses the alpha from the main texture*/


}

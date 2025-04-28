package com.mygdx.tempto.entity.decoration;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.XmlReader;
import com.mygdx.tempto.entity.Entity;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.rendering.ShadowCaster;
import com.mygdx.tempto.rendering.TileLayerDepthRenderer;
import com.mygdx.tempto.rendering.TileLayerFinalRenderer;
import com.mygdx.tempto.rendering.RendersToWorld;

import java.util.List;

public class TileLayer implements Entity, RendersToWorld {

    String ID;
    TiledMapTileLayer mapLayer;
    XmlReader.Element originalElement;
    float baseDepth; //In pixel space, e.g. 10 pixels
    WorldMap parent;
    boolean rotate; //Whether this layer's tiles are rotated, for now just flipped back along the left side, goes -z instead of +x

    public TileLayer(WorldMap parent, TiledMapTileLayer mapLayer, XmlReader.Element originalElement, float baseDepth) {
        this(parent, mapLayer, originalElement, baseDepth, false);
    }
    public TileLayer(WorldMap parent, TiledMapTileLayer mapLayer, XmlReader.Element originalElement, float baseDepth, boolean rotate) {
        this.setParentWorld(parent);
        this.mapLayer = mapLayer;
        this.originalElement = originalElement;
        this.baseDepth = baseDepth;
        this.ID = "tiles_"+this.baseDepth;
        this.rotate = rotate;
    }


    @Override
    public void update(float deltaTime, WorldMap world) {

    }



    @Override
    public void setParentWorld(WorldMap parent) {
        this.parent = parent;
    }

    @Override
    public String getID() {
        return this.ID;
    }

    @Override
    public void renderToWorld(Batch batch, OrthographicCamera worldCamera) {
        TileLayerFinalRenderer renderer = this.parent.tileFinalRenderer;
        renderer.setView(worldCamera);
        renderer.renderTileLayer(this);
    }

    @Override
    public void renderToDepthMap(Batch depthBatch, OrthographicCamera worldCamera) {
        TileLayerDepthRenderer renderer = this.parent.tileDepthRenderer;
        renderer.setView(worldCamera);
        renderer.renderTileLayer(this);
    }

    @Override
    public void addShadowCastersToList(List<ShadowCaster> centralList) {this.addShadowCastersToList(centralList, 1.0f);}
    public void addShadowCastersToList(List<ShadowCaster> centralList, float unitScale) {
//        RendersToWorld.super.addShadowCastersToList(centralList);

        TiledMapTileLayer layer = this.mapLayer;
        int col1 = 0;
        int row1 = 0;
        float xStart = 0;
        float y = 0;
        for (int row = row1; row < layer.getHeight(); row++) {
            float x = xStart;
            for (int col = col1; col < layer.getWidth(); col++) {
                final TiledMapTileLayer.Cell cell = layer.getCell(col, row);
                if (cell == null) {
                    x+=layer.getTileWidth();
                    continue;
                }
                TiledMapTile tile = cell.getTile();
                float x1 = x + tile.getOffsetX() * unitScale;
                float y1 = y + tile.getOffsetY() * unitScale;
                TextureRegion tileRegion = tile.getTextureRegion();

                Vector3 u = new Vector3(tileRegion.getRegionWidth(), 0, 0);

                if (this.rotate) { //Test: rotate back along left side
                    u.z = u.x*(float) Math.cos(Math.toRadians(45));
                    u.x = u.x*(float) Math.cos(Math.toRadians(45));
                }

                ShadowCaster cellCaster = new ShadowCaster(tileRegion,
                        new Vector3(x1, y1, this.baseDepth),
                        u,
                        new Vector3(0, tileRegion.getRegionHeight(), 0));
                centralList.add(cellCaster);
                x+=layer.getTileWidth();
            }
            y+=layer.getTileWidth();
        }

//        Rectangle viewBounds = this.parent.tileDepthRenderer.getViewBounds();
//        TiledMapTileLayer tiledLayer = layer;
//
//        final int layerWidth = tiledLayer.getWidth();
//        final int layerHeight = tiledLayer.getHeight();
//
//        final float layerTileWidth = tiledLayer.getTileWidth() * unitScale;
//        final float layerTileHeight = tiledLayer.getTileHeight() * unitScale;
//
//        final float layerOffsetX = tiledLayer.getRenderOffsetX() * unitScale - viewBounds.x * (tiledLayer.getParallaxX() - 1);
//        // offset in tiled is y down, so we flip it
//        final float layerOffsetY = -tiledLayer.getRenderOffsetY() * unitScale - viewBounds.y * (tiledLayer.getParallaxY() - 1);
//
//        final int col1 = Math.max(0, (int)((viewBounds.x - layerOffsetX) / layerTileWidth));
//        final int col2 = Math.min(layerWidth,
//                (int)((viewBounds.x + viewBounds.width + layerTileWidth - layerOffsetX) / layerTileWidth));
//
//        final int row1 = Math.max(0, (int)((viewBounds.y - layerOffsetY) / layerTileHeight));
//        final int row2 = Math.min(layerHeight,
//                (int)((viewBounds.y + viewBounds.height + layerTileHeight - layerOffsetY) / layerTileHeight));
//
//        float y = row2 * layerTileHeight + layerOffsetY;
//        float xStart = col1 * layerTileWidth + layerOffsetX;
//        for (int row = row2; row >= row1; row--) {
//            float x = xStart;
//            for (int col = col1; col < col2; col++) {
//                final TiledMapTileLayer.Cell cell = tiledLayer.getCell(col, row);
//                if (cell == null) {
//                    x += layerTileWidth;
//                    continue;
//                }
//                final TiledMapTile tile = cell.getTile();
//
//                if (tile != null) {
//                    this.drawTile(cell, x, y, vertices, color);
//                }
//                x += layerTileWidth;
//            }
//            y -= layerTileHeight;
//        }
    }

    public TiledMapTileLayer getMapLayer() {
        return mapLayer;
    }

    public float getBaseDepth() {
        return baseDepth;
    }

    public boolean isRotate() {
        return rotate;
    }

    public WorldMap getParent() {
        return parent;
    }
}

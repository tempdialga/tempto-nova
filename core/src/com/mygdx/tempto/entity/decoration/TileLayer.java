package com.mygdx.tempto.entity.decoration;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.mygdx.tempto.entity.Entity;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.rendering.AtlasOrthogonalTiledMapRenderer;
import com.mygdx.tempto.rendering.RendersToWorld;

public class TileLayer implements Entity, RendersToWorld {

    String ID;
    TiledMapTileLayer mapLayer;
    float baseDepth; //In pixel space, e.g. 10 pixels
    WorldMap parent;

    public TileLayer(WorldMap parent, TiledMapTileLayer mapLayer, float baseDepth) {
        this.setParentWorld(parent);
        this.mapLayer = mapLayer;
        this.baseDepth = baseDepth;
        this.ID = "tiles_"+this.baseDepth;
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
        OrthogonalTiledMapRenderer renderer = this.parent.tileRenderer;
        renderer.setView(worldCamera);
        renderer.renderTileLayer(this.mapLayer);
    }

    @Override
    public void renderToDepthMap(Batch depthBatch, OrthographicCamera worldCamera) {
        AtlasOrthogonalTiledMapRenderer renderer = this.parent.tileDepthRenderer;
        renderer.currentBaseDepth.r = (1/this.baseDepth);
        renderer.setView(worldCamera);
        renderer.renderTileLayer(this.mapLayer);
    }
}

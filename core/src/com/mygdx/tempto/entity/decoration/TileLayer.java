package com.mygdx.tempto.entity.decoration;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.XmlReader;
import com.mygdx.tempto.entity.Entity;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.rendering.AltDepthBatch;
import com.mygdx.tempto.rendering.ShadowCaster;
import com.mygdx.tempto.rendering.TileLayerDepthRenderer;
import com.mygdx.tempto.rendering.TileLayerFinalRenderer;
import com.mygdx.tempto.rendering.RendersToWorld;

import org.eclipse.collections.impl.map.mutable.primitive.IntBooleanHashMap;

import java.util.List;

public class TileLayer implements Entity, RendersToWorld {

    protected static final IntBooleanHashMap FLAT_TILES = new IntBooleanHashMap();//Surely there can't be more than 1,000 kinds of tiles right

    String ID;
    TiledMapTileLayer mapLayer;
    XmlReader.Element originalElement;
    float baseDepth; //In pixel space, e.g. 10 pixels
    Vector3 baseU = new Vector3(1,0,0);//Base U vector, as a normal vector. Scale to final tile width.
    Vector3 baseV = new Vector3(0,1,0); //Likewise, scale by actual tile height
    Vector3 baseNormVec; //Only includes x and y, since z can be inferred
    WorldMap parent;
    boolean rotate; //Whether this layer's tiles are rotated, for now just flipped back along the left side, goes -z instead of +x
    boolean waver; //Debug test: If true, varies depth by a sine wave of 10 px

    public static final int SIMULTANEOUS = 0, X_THEN_Y = 1, Y_THEN_X = 2;
    /**How to order the rotations. Simultaneous is typically fine, but would stretch it diagonally instead of a consistent rotation*/
    public int rotationOrder = SIMULTANEOUS;
    /**Rotation around x and y axes. Currently only around left or bottom edges, but in the future would allow an axis to be set by pixel.*/
    public Vector2 rotations = new Vector2();
    /**CURRENTLY UNUSED: Describes the offsets (x, right from the left edge, and y up from the bottom) around which the rotations of {@link #rotations} are applied*/
    public Vector2 rotationAxes = new Vector2();

    public TileLayer(WorldMap parent, TiledMapTileLayer mapLayer, XmlReader.Element originalElement, float baseDepth) {
        this(parent, mapLayer, originalElement, baseDepth, false);
    }
    public TileLayer(WorldMap parent, TiledMapTileLayer mapLayer, XmlReader.Element originalElement, float baseDepth, boolean rotate) {
        this.setParentWorld(parent);
        this.mapLayer = mapLayer;
        this.originalElement = originalElement;
        this.baseDepth = baseDepth;
        this.ID = originalElement.getName();
        this.rotate = rotate;
        this.baseNormVec = new Vector3();
        if (rotate) {
            this.baseNormVec.x = (float) Math.cos(Math.toRadians(45));
        }
    }

    public void setRotations(Vector2 rotations) {
        this.rotations = rotations;
        this.rotate = true;
        Vector3 u = this.baseU.set(1,0,0);
        Vector3 v = this.baseV.set(0,1,0);

        double x_rad = Math.toRadians(this.rotations.x);
        double y_rad = Math.toRadians(this.rotations.y);

        if (this.rotationOrder == SIMULTANEOUS){
            u.z += (float) Math.sin(x_rad);
            u.x *= (float) Math.cos(x_rad);

            v.z += (float) Math.sin(y_rad);
            v.y *= (float) Math.cos(y_rad);
        }
        this.baseU = u;
        this.baseV = v;
        this.baseNormVec = new Vector3(u).crs(v).nor();
    }

    public void setWaver(boolean waver) {
        this.waver = waver;
    }

    public boolean isWaver() {
        return waver;
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
    public void renderToDepthMap(AltDepthBatch depthBatch, OrthographicCamera worldCamera) {

        TileLayerDepthRenderer renderer = this.parent.tileDepthRenderer;
        renderer.setView(worldCamera);
        renderer.renderTileLayer(this);
    }

    /**Takes the given {@link TiledMapTileLayer}, and marks that any tiles present on that layer are flat, and don't need textures for their shadows.*/
    public static void setFlatTiles(TiledMapTileLayer flatLayer) {
        FLAT_TILES.clear();
        int col1 = 0;
        int row1 = 0;
        float xStart = 0;
        float y = 0;
        for (int row = row1; row < flatLayer.getHeight(); row++) {
            float x = xStart;
            for (int col = col1; col < flatLayer.getWidth(); col++) {
                final TiledMapTileLayer.Cell cell = flatLayer.getCell(col, row);
                if (cell == null) {
                    x+=flatLayer.getTileWidth();
                    continue;
                }
                TiledMapTile tile = cell.getTile();
                FLAT_TILES.put(tile.getId(), true);

                x+=flatLayer.getTileWidth();
            }
            y+=flatLayer.getTileWidth();
        }
    }

    /**Returns whether the given kind of tile is flat, i.e., its shadow can be represented by the shadow of a generic quadrangle*/
    public static boolean isTileFlat(int tileID) {
        //Check if we've already said this tile is flat, and if we haven't, assume it's not for the future
        return FLAT_TILES.getIfAbsentPut(tileID, false);
    }


    @Override
    public void addShadowCastersToList(List<ShadowCaster> centralList) {this.addShadowCastersToList(centralList, 1.0f);}
    public void addShadowCastersToList(List<ShadowCaster> centralList, float unitScale) {
//        System.out.println("Layer depth: " + this.getBaseDepth());
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


                float w = tile.getTextureRegion().getRegionWidth();
                float h = tile.getTextureRegion().getRegionHeight();

                Vector3 origin = new Vector3(x1, y1, this.getBaseDepth());
                Vector3 u = new Vector3(this.getBaseU()).scl(w);
                Vector3 v = new Vector3(this.getBaseV()).scl(h);
                ShadowCaster cellCaster = new ShadowCaster(tileRegion,

                        origin,
                        u,
                        v,
                        isTileFlat(tile.getId()));
                centralList.add(cellCaster);
                x+=layer.getTileWidth();
            }
            y+=layer.getTileWidth();
        }
    }

    public TiledMapTileLayer getMapLayer() {
        return mapLayer;
    }

    public float getBaseDepth() {
        if (this.waver) {
            float newDepth = baseDepth /*+ 10 * (float) (Math.sin(3 * GameScreen.elapsedTime))*/;
//            System.out.println("Layer depth: ");
            return newDepth;
        }

        return baseDepth;
    }

    public void setBaseDepth(float baseDepth) {
        this.baseDepth = baseDepth;
    }

    public Vector3 getBaseNormVec() {
        return baseNormVec;
    }

    public Vector3 getBaseU() {
        return baseU;
    }

    public Vector3 getBaseV() {
        return baseV;
    }

    public boolean isRotate() {
        return rotate;
    }

    public WorldMap getParent() {
        return parent;
    }
}

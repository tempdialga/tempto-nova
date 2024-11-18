package com.mygdx.tempto.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.Map;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Null;
import com.mygdx.tempto.data.SavesToFile;
import com.mygdx.tempto.entity.physics.Collidable;
import com.mygdx.tempto.entity.physics.PointProcedure;
import com.mygdx.tempto.entity.physics.SegmentProcedure;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.util.MiscFunctions;

import org.eclipse.collections.api.map.MapIterable;

public class StaticTerrainElement implements Entity, SavesToFile, Collidable {

    public Polygon polygon;//TODO: generalize, this is only a temporary thing to make sure map loading works
    public Color color;
    public String id;
    public InputAdapter debugInput; //TODO: remove once we add other things that actually are supposed to take user input
    private boolean edited; //Whether or not this terrain object has been edited and needs to be saved to the base file
    private boolean deleted; //If, as part of being edited, this object has been deleted

    public StaticTerrainElement(PolygonMapObject mapObject) {
        this(mapObject, null);
    }

    public StaticTerrainElement(PolygonMapObject mapObject, JsonValue persistentMapData) {
        this(mapObject.getPolygon(), mapObject.getProperties(), persistentMapData);
    }

    private StaticTerrainElement(Polygon shape, MapProperties properties, JsonValue persistentMapData) {
        this.polygon = shape;
        this.id = properties.get("id", String.class);//ID of the object as denoted in base map file; dictates how

        if (persistentMapData != null && persistentMapData.has(id)) {
            JsonValue thisData = persistentMapData.get(id); //Data in the value corresponding to this map object
            Color savedColor = Color.valueOf(thisData.getString("color"));//If it has the id, it *should* have been saved with this
            this.setColor(savedColor);//Set the color as it was saved in the map
        } else {
            this.setColor(Color.BLACK);
        }


        float[] points = this.polygon.getTransformedVertices();

        // Add a testing input adapter that, when the player confirms, will change color.
        this.debugInput = new InputAdapter(){
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == InputTranslator.GameInputs.CONFIRM) {
                    float r, g, b;
                    r = (float) Math.random();
                    g = (float) Math.random();
                    b = (float) Math.random();
                    StaticTerrainElement.this.setColor(new Color(r, g, b, 1));
                }
                return false;
            }
        };
    }


    public void setColor(Color newColor) {
        this.color = newColor;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public void update(float deltaTime, WorldMap world) {
    }

    @Override
    public void setParentWorld(WorldMap parent) {
        parent.addWorldInput(this.debugInput);
    }

    @Override
    public String getID() {
        return this.id;
    }

//    @Override
//    public boolean needToEditBaseFile() {
//        return this.edited;
//    }

    //@Override
    public void updateBaseFile(TiledMap baseMap, @Null MapLayer specifiedEntityLayer) {
        MapLayer toPlaceIn = specifiedEntityLayer; //The map layer to place entity data in
        if (specifiedEntityLayer == null) {
            boolean foundALayer = false;
            for (MapLayer layer : baseMap.getLayers()) {
                if (!(layer instanceof TiledMapImageLayer || layer instanceof TiledMapTileLayer)) {
                    toPlaceIn = layer;
                    break; // Don't need to look any further
                }
            }
        }
        if (toPlaceIn == null) { //If no such layer has been found, this isn't workable
            System.out.println("No Entity layer found!");
            throw new NullPointerException("No Entity layer given!");
        }

        //Search for existing version of this?

        //Create a new map data object
        PolygonMapObject baseMapObject = new PolygonMapObject(this.polygon);
        //baseMapObject.setPolygon(this.polygon);
        //Match its properties
        MapProperties props = baseMapObject.getProperties();
        props.put("id", this.id);
        props.put("x", this.polygon.getX());
        props.put("y", this.polygon.getY());
        //Save into the base map data
        toPlaceIn.getObjects().add(baseMapObject);
        
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    @Override
    public void writeSerializedValue(JsonValue host) {
        JsonValue thisValue = new JsonValue(JsonValue.ValueType.object);
        thisValue.addChild("color", new JsonValue(this.color.toString()));
        host.addChild(this.id, thisValue);
        System.out.println(host);
    }


    @Override
    public void forEachSegment(SegmentProcedure procedure) {
        Polygon pol = this.getPolygon();
        Vector2 A = new Vector2(), B = new Vector2();
        for (int i = 0; i < pol.getVertexCount(); i++) {
            int j = i + 1;
            if (j >= pol.getVertexCount()) j = 0;

            A = pol.getVertex(i, A);
            B = pol.getVertex(j, B);

            procedure.forEach(
                    A.x, A.y, //Coords of A
                    B.x, B.y, //Coords of B
                    0,0,0,0, //Polygon terrain static (for now)
                    i, j, //Indices of A and B
                    SegmentProcedure.CC_NORMAL //Polygon terrain is clockwise wound, so the normal vector to the vector A->B is rotated 90⁰ counterclockwise to A->B
            );

        }
    }

    @Override
    public void forEachPoint(PointProcedure procedure) {
        Polygon pol = this.getPolygon();
        Vector2 A = new Vector2();
        for (int i = 0; i < pol.getVertexCount(); i++) {

            A = pol.getVertex(i, A);

            procedure.forEach(
                    i, //Index of A
                    A.x, A.y, //Coords of A
                    0,0 //Polygon terrain static (for now)
            );

        }
    }

    @Override
    public Vector2 getPosAtIndex(float index, boolean exactPoint) {
        int exactIdx = (int) (index + 0.5f);
        Vector2 A = this.getPolygon().getVertex(exactIdx, new Vector2());
        if (exactPoint) return A;

        int nextIdx = exactIdx + 1;
        if (nextIdx >= this.getPolygon().getVertexCount()) nextIdx = 0;
        Vector2 B = this.getPolygon().getVertex(nextIdx, new Vector2());

        float T = index - (float) exactIdx;

        return MiscFunctions.interpolateLinearPath(A, B, T);
    }

    @Override
    public Vector2 getVelAtIndex(float index, boolean exactPoint) {
        return new Vector2(0,0);
    }

    @Override
    public Rectangle getBoundingRectangle() {
        return this.getPolygon().getBoundingRectangle();
    }
}

package com.mygdx.tempto.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.mygdx.tempto.data.SavesToFile;
import com.mygdx.tempto.maps.WorldMap;

public class StaticTerrainElement implements Entity, SavesToFile {

    public Polygon polygon;//TODO: generalize, this is only a temporary thing to make sure map loading works
    public Color color;
    public String id;
    public StaticTerrainElement(PolygonMapObject mapObject, JsonValue persistentMapData) {
        Polygon polygon = mapObject.getPolygon();
        this.id = mapObject.getProperties().get("id", String.class);//ID of the object as denoted in base map file; dictates how

        if (persistentMapData.has(id)) {
            JsonValue thisData = persistentMapData.get(id); //Data in the value corresponding to this map object
            Color savedColor = Color.valueOf(thisData.getString("color"));//If it has the id, it *should* have been saved with this
            this.setColor(savedColor);//Set the color as it was saved in the map
        } else {
            this.setColor(Color.BLACK);
        }


        this.polygon = polygon;
        float[] points = this.polygon.getTransformedVertices();
        //this.pos = new Vector2(points[0], points[1]);
    }

    public void setColor(Color newColor) {
        this.color = newColor;
    }


    @Override
    public void update(float deltaTime, WorldMap world) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) { //Debug thing that randomizes colors to see if it can save data properly
            float r, g, b;
            r = (float) Math.random();
            g = (float) Math.random();
            b = (float) Math.random();
            this.setColor(new Color(r, g, b, 1));
        }
    }

    @Override
    public void writeSerializedValue(JsonValue host) {
        JsonValue thisValue = new JsonValue(JsonValue.ValueType.object);
        thisValue.addChild("color", new JsonValue(this.color.toString()));
        host.addChild(this.id, thisValue);
        System.out.println(host);
    }
}

package com.mygdx.tempto.entity;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class StaticTerrainElement extends Entity{

    public Polygon polygon;//TODO: generalize, this is only a temporary thing to make sure map loading works

    public StaticTerrainElement(Polygon polygon) {
        this.polygon = polygon;
        float[] points = this.polygon.getTransformedVertices();
        this.pos = new Vector2(points[0], points[1]);
    }

}

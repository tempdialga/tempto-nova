package com.mygdx.tempto.entity.decoration;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.tempto.entity.Entity;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.rendering.LightSource;

public class TestLight implements Entity {

    public LightSource light;
    public String id;


    public TestLight(float x, float y, String id) {
        LightSource newLight = new LightSource(new Vector3(x, y, -10), new Color(1,0.9f, 0.5f, 1), 450, LightSource.SPHERE_APPROX, 1);
        this.light = newLight;
        this.id = id;
    }



    @Override
    public void update(float deltaTime, WorldMap world) {

    }

    @Override
    public void setParentWorld(WorldMap parent) {
    }

    @Override
    public String getID() {
        return this.id;
    }
}

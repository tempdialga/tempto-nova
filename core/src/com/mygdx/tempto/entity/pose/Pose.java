package com.mygdx.tempto.entity.pose;

import com.badlogic.gdx.math.Vector2;

import java.util.Map;

/**A base for classes that can pose {@link Posable} instances*/
public abstract class Pose {

    protected Map<String, Vector2> inputPoints;
    protected Map<String, Vector2> outputPoints;

    public Pose(Map<String, Vector2> inputPoints) {
        this.inputPoints = inputPoints;
    }

    public abstract void update(float deltaTime);

    public Vector2 getOutputPoint(String id) {return this.outputPoints.get(id);}


    public void assertKeysPresent(String... keys) {
        for (String key : keys) {
            if (!this.inputPoints.containsKey(key)) {
                throw new IllegalArgumentException("Input point with key \""+key+"\" missing from construction of "+this.getClass()+" pose");
            }
        }
    }
}

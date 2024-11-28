package com.mygdx.tempto.entity.pose;

import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

/**A base for classes that can pose {@link Posable} instances*/
public abstract class Pose {

    protected Map<String, Vector2> inputPoints;
    protected Map<String, Vector2> outputPoints;
    protected Posable subject;
    protected float T;

    public Pose(Posable subject, Map<String, Vector2> inputPoints) {
        this.inputPoints = inputPoints;
        this.subject = subject;
        this.T = 0;
    }

    public abstract void update(float deltaTime);

    public Vector2 getOutputPoint(String id) {return this.outputPoints.get(id);}

    public float getT() {return this.T;}
    public void setT(float T) {this.T = T;}

    public void assertKeys(String... keys) {
        for (String key : keys) {
            if (!this.inputPoints.containsKey(key)) {
                throw new IllegalArgumentException("Input point with key \""+key+"\" missing from construction of "+this.getClass()+" pose");
            }
        }
    }
}

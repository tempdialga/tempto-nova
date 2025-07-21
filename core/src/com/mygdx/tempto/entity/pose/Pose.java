package com.mygdx.tempto.entity.pose;

import com.badlogic.gdx.math.Vector2;
import com.mygdx.tempto.util.MiscFunctions;

import java.util.HashMap;
import java.util.Set;

/**A wrapper for a set of points with labels that can be quickly interpolated with other poses*/
public record Pose(HashMap<String, Vector2> points) {

    /**Scales all the points in this pose by the given x and y values. DOES NOT CREATE A NEW POSE. Returns self for chaining.*/
    public Pose scale(float x, float y) {
        for (Vector2 point : this.points.values()) {
            point.scl(x, y);
        }
        return this;
    }

    /**Alias of {@link #scale(float, float)}.*/
    public Pose scale(Vector2 factors) {
        return this.scale(factors.x, factors.y);
    }

    /**Alias of {@link #scale(float, float)}, but scales both components by the same factor.*/
    public Pose scale(float uniformFactor) {
        return this.scale(uniformFactor, uniformFactor);
    }

    /**Shifts the x and y coordinates of every point in this pose by the given values. DOES NOT CREATE A NEW POSE. Returns self for chaining.*/
    public Pose shift(float x, float y) {
        for (Vector2 point : this.points.values()) {
            point.add(x, y);
        }
        return this;
    }

    /**Alias of {@link #shift(float, float)}.*/
    public Pose shift(Vector2 amount) {
        return this.shift(amount.x, amount.y);
    }

    public Vector2 get(String pointName) {
        return this.points.get(pointName);
    }

    public Pose interpolate(Pose other, float weight) {
        return Pose.interpolate(this, other, weight);
    }

    public static Pose interpolate(Pose a, Pose b, float b_weight) {
        Set<String> aPointIDs = a.points.keySet();
        Set<String> bPointIDs = b.points.keySet();

        if (!MiscFunctions.setsHaveEqualElements(aPointIDs, bPointIDs)) throw new IllegalArgumentException("Poses a and b must have the same points! a: "+a.points.keySet()+", b: "+b.points.keySet());

        HashMap<String, Vector2> newPoints = new HashMap<>();
        for (String id : aPointIDs) {
            Vector2 point = new Vector2(a.points.get(id)).lerp(b.points.get(id), b_weight);
            newPoints.put(id, point);
        }

        return new Pose(newPoints);
    }
}

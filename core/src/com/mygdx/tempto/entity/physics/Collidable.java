package com.mygdx.tempto.entity.physics;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**An interface for anything that entities can collide with, such as terrain.*/
public interface Collidable {

    /**Runs the given {@link SegmentProcedure} on each applicable, consistently indexed segment of the collidable.
     * Consistently indexed means that the given indices for points A and B, and indices on the range [A, A+1) can be used with methods like {@link #getPosAtIndex(float)}*/
    void forEachSegment(SegmentProcedure procedure);

    /**Runs the given {@link PointProcedure} on each applicable, consistently indexed point of the collidable.
     * Consistently indexed means that the given index of the point can be used with methods like {@link #getPosAtIndex(float, boolean)}.
     * */
    void forEachPoint(PointProcedure procedure);

    /**Returns the position corresponding to the given index, which can be a non-integer number.
     * @param index The position to query. Will interpolate on the segment starting at floor(index).
     * @param exactPoint If true, instead of interpolating on a segment, will floor the index and use that point.*/
    Vector2 getPosAtIndex(float index, boolean exactPoint);
    default Vector2 getPosAtIndex(float index){
        return getPosAtIndex(index, false);
    }

    /**Returns the position corresponding to the given index, which can be a non-integer number.
     * @param index The position to query. Will interpolate on the segment starting at floor(index).
     * @param exactPoint If true, instead of interpolating on a segment, will floor the index and use that point.*/
    Vector2 getVelAtIndex(float index, boolean exactPoint);
    default Vector2 getVelAtIndex(float index){
        return getVelAtIndex(index, false);
    }

    Rectangle getBoundingRectangle();


}

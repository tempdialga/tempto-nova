package com.mygdx.tempto.entity.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Null;

public interface PointProcedure {

    /**Provides for the definition of instructions to carry out with a point of the given x, y, and velocity.
     * @param index The consistent index of the given point
     * @param x The current x position of the point
     * @param y The current y position of the point
     * @param v_x The current x velocity of the point
     * @param v_y The current y velocity of the point
     * @param orthogonals A set of vectors describing directions the point faces
     * This is provided for pruning purposes, such as ignoring collisions with points on a polygon collidable where it only makes sense to be able to hit the point from one side
     * When in doubt, ignore {@param orthogonal}*/
    void forEach(int index, float x, float y, float v_x, float v_y, @Null Vector2... orthogonals);
}
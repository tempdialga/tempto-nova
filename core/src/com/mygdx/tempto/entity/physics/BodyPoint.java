package com.mygdx.tempto.entity.physics;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Null;
import com.mygdx.tempto.util.MiscFunctions;

import java.util.ArrayList;
import java.util.Vector;

/**A physics utility class for colliding with */
public class BodyPoint {

    public static final float DEFAULT_COLLISION_BUFFER = 1f/16f;

    public static final int POINT = 0;
    public static final int CIRCLE = 1;
    public static final int SQUARE = 2;
    public static final int BAR = 3;

    //// Self (static) information ////
    protected int shape;
    protected float radius;

    //// State (dynamic) information ////
    protected Vector2 pos;
    /**The position of the point at the end of its last update call*/
    protected Vector2 lastFramePos;
    /**The velocity of the point. By default, not applied by the point itself on each {@link}*/
    protected Vector2 vel;


    public BodyPoint(int shape, float radius, Vector2 pos) {
        this.shape = shape;
        this.radius = radius;
        this.pos = pos;
        this.lastFramePos = new Vector2(pos);
    }

    /**The central update loop to be run on each given point. Active changes in position and velocity like gravity should be applied by the governing entity.
     *
     * Checks for collisions with given collidables on the point's path between {@link #lastFramePos} and {@link #pos}, if collidables is not null.
     *
     * @param deltaTime The time passed since the last frame update.
     * @param collidables The collidables/terrain that the point can collide with. If the point should not collide (e.g. collisions will be handled manually), should be null.
     *
     * */
    public void update(float deltaTime, @Null ArrayList<Collidable> collidables){

    }





    /**Check's the given point's movement for, but does not apply, collisions with the given collidables. Instead, returns a record {@link PointOnSegmentCollision} of information about the first collision found.
     * If it would not collide with anything, returns null.
     * */
    public PointOnSegmentCollision findCollision(ArrayList<Collidable> collidables) {
        final PointOnSegmentCollision[] firstCollision = {null};

        Vector2 before = this.lastFramePos;
        Vector2 after = this.pos;
        Vector2 change = new Vector2(after).sub(before);
        Vector2 buffer = new Vector2(change).nor().scl(DEFAULT_COLLISION_BUFFER);
        Vector2 beforeBuffered = new Vector2(before).sub(buffer);

        for (Collidable coll : collidables) {
            switch(this.shape) {
                case POINT -> {
                    coll.forEachSegment(new SegmentProcedure() {
                        @Override
                        public void forEach(float ax, float ay, float bx, float by, float av_x, float av_y, float bv_x, float bv_y, int indexA, int indexB, int normDirection) {
                            Vector2 collisionPoint = new Vector2();
                            if (Intersector.intersectSegments(
                                    beforeBuffered.x, beforeBuffered.y,
                                    after.x, after.y,
                                    ax, ay,
                                    bx, by,
                                    collisionPoint
                            )) {
                                float T = MiscFunctions.tOnLinearPath(before, after, collisionPoint);
                                float collIndex = ((float) indexA) + MiscFunctions.tOnLinearPath(new Vector2(ax, ay), new Vector2(bx, by), collisionPoint);

                                if (firstCollision[0] == null || firstCollision[0].collisionT() > T) {
                                    firstCollision[0] = new PointOnSegmentCollision(BodyPoint.this, coll, collIndex, T, collisionPoint);
                                }
                            }
                        }
                    });
                }
            }
        }

        return firstCollision[0];
    }

    /**Ends the frame by resetting {@link #lastFramePos} to the current {@link #pos} value*/
    public void endFrame(){
        this.lastFramePos.set(this.pos);
    }

    /**Accelerates the point by the given acceleration vector, for the given change in time*/
    public void accelerate(float deltaTime, Vector2 acceleration) {
        this.vel.add(new Vector2(acceleration).scl(deltaTime));
    }

    public void applyVelocity(Vector2 vel, float deltaTime) {
        this.pos.add(vel.x*deltaTime, vel.y*deltaTime);
    }

    /**A record storing information about a hypothetical collision between a point and target*/
    public record PointOnSegmentCollision(BodyPoint point, Collidable target, float collisionIndex, float collisionT, Vector2 collisionPos) {}

    public Vector2 getPos() {
        return pos;
    }
}

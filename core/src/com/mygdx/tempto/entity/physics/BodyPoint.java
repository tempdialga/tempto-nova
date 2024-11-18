package com.mygdx.tempto.entity.physics;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Null;
import com.mygdx.tempto.util.MiscFunctions;

import java.util.ArrayList;

/**A physics utility class for colliding with */
public class BodyPoint {

    public static final float DEFAULT_COLLISION_BUFFER = 1f/32f;

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





    /**Check's the given point's movement for, but does not apply, collisions with the given collidables. Instead, returns a record {@link PointCollision} of information about the first collision found.
     * If it would not collide with anything, returns null.
     * */
    public PointCollision findCollision(ArrayList<Collidable> collidables) {
        final PointCollision[] firstCollision = {null};

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
                                    Vector2 norm = new Vector2(bx, by).sub(ax, ay).nor().rotate90(normDirection);
                                    Vector2 movement = new Vector2(after).sub(before);
                                    if (normDirection == NO_NORMAL) {
                                        //If no norm direction specified, go with the opposite of whatever direction the point is going
                                        norm = MiscFunctions.projectAontoB(movement, norm).nor().scl(-1);
                                    }

                                    //Only log collision if movement is against normal
                                    if (norm.dot(movement) < 0) {
                                        firstCollision[0] = new PointCollision(BodyPoint.this, coll, collIndex, T, collisionPoint, collisionPoint, norm);
                                    }
                                }
                            }
                        }
                    });
                }
                case CIRCLE -> {
                    coll.forEachSegment(new SegmentProcedure() {
                        @Override
                        public void forEach(float ax, float ay, float bx, float by, float av_x, float av_y, float bv_x, float bv_y, int indexA, int indexB, int normDirection) {
                            Vector2 norm = new Vector2(bx, by).sub(ax, ay).nor().rotate90(normDirection);
                            Vector2 movement = new Vector2(after).sub(before);
                            if (normDirection == NO_NORMAL) {
                                //If no norm direction specified, go with the opposite of whatever direction the point is going
                                norm = MiscFunctions.projectAontoB(movement, norm).nor().scl(-1);
                            }
                            if (norm.dot(movement) >= 0) {
                                //Only search for collisions going against the normal direction
                                return;
                            }

                            Vector2 radius = new Vector2(norm).scl(-1*BodyPoint.this.radius);
                            Vector2 closestBefore = new Vector2(beforeBuffered).add(radius);
                            Vector2 closestAfter = new Vector2(after).add(radius);


                            Vector2 contactPoint = new Vector2();
                            if (Intersector.intersectSegments(
                                    closestBefore.x, closestBefore.y,
                                    closestAfter.x, closestAfter.y,
                                    ax, ay,
                                    bx, by,
                                    contactPoint
                            )) {
                                float T = MiscFunctions.tOnLinearPath(closestBefore, closestAfter, contactPoint);
                                float collIndex = ((float) indexA) + MiscFunctions.tOnLinearPath(new Vector2(ax, ay), new Vector2(bx, by), contactPoint);

                                if (firstCollision[0] == null || firstCollision[0].collisionT() > T) {
                                    firstCollision[0] = new PointCollision(BodyPoint.this, coll, collIndex, T, contactPoint, new Vector2(contactPoint.sub(radius)), norm);
                                }
                            }
                        }
                    });
                    coll.forEachPoint(new PointProcedure() {
                        @Override
                        public void forEach(int index, float x, float y, float v_x, float v_y, Vector2... orthogonals) {

                            if (Intersector.distanceSegmentPoint(
                                    beforeBuffered.x, beforeBuffered.y,
                                    after.x, after.y,
                                    x, y
                            ) < BodyPoint.this.radius) {
                                //Find where it hits, in terms of perpendicular to and parallel to the path
                                float perpToPath = Intersector.distanceLinePoint(
                                        beforeBuffered.x, beforeBuffered.y,
                                        after.x, after.y,
                                        x,y
                                );
                                float paraToPath = (float) Math.sqrt(BodyPoint.this.radius*BodyPoint.this.radius - perpToPath*perpToPath);


                                Vector2 pathDir = new Vector2(beforeBuffered).sub(after).nor(); //Pointing backwards
                                int sideOfPath = Intersector.pointLineSide(beforeBuffered.x, beforeBuffered.y, after.x, after.y, x,y);
                                Vector2 perpDir = new Vector2(pathDir).rotate90(sideOfPath);

                                Vector2 radius = new Vector2(perpDir.scl(perpToPath)).add(pathDir.scl(paraToPath)); //Pointing away from contact

                                Vector2 pointPos = new Vector2(x, y).add(radius);
                                float T = MiscFunctions.tOnLinearPath(before, after, pointPos);

                                if (firstCollision[0] == null || firstCollision[0].collisionT() > T) {
                                    firstCollision[0] = new PointCollision(BodyPoint.this, coll, index, T, new Vector2(x, y), pointPos, radius.nor());
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

    /**A record storing information about a hypothetical collision between a point and segment target*/
    public record PointCollision(BodyPoint point, Collidable target, float collisionIndex, float collisionT, Vector2 contactPos, Vector2 pointPos, Vector2 normalToSurface) {}

    public Vector2 getPos() {
        return pos;
    }

    public Vector2 getLastFramePos() {
        return lastFramePos;
    }

    public int getShape() {
        return shape;
    }

    public float getRadius() {
        return radius;
    }
}

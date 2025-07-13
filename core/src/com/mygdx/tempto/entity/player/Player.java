package com.mygdx.tempto.entity.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.tempto.entity.Entity;
import com.mygdx.tempto.entity.physics.BodyPoint;
import com.mygdx.tempto.entity.physics.Collidable;
import com.mygdx.tempto.entity.physics.SegmentProcedure;
import com.mygdx.tempto.entity.pose.Posable;
import com.mygdx.tempto.entity.pose.PoseCatalog;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.rendering.RendersToDebug;
import com.mygdx.tempto.rendering.RendersToWorld;
import com.mygdx.tempto.util.MiscFunctions;

import org.eclipse.collections.impl.list.fixed.ArrayAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import space.earlygrey.shapedrawer.ShapeDrawer;

/**A class for the player entity.
 * Usage Notes:
 *  Processed with other entities, added to list of world inputs.
 *
 * Design Notes (Last updated 2025-1-17):
 *
 * */
public class Player extends InputAdapter implements Entity, RendersToWorld, RendersToDebug, Posable {

    ///// Bodily attributes
    static final float FOOT_RADIUS = 2.0f;
    static final float LEG_LENGTH = 15f;

    ///// Other constants / notes
    static final int LEFT_FOOT = -1;
    static final int RIGHT_FOOT = 1;
    static final int NEITHER = 0;


    /**These are values that are set by design (leg height, walk stride distance, etc.) but that might need to be tinkered with on the fly, for development*/
    HashMap<String, Float> attributes;

    /**Arbitrary pieces of information that a pose might need to save, but might not justify storing a whole explicit variable just for that one pose*/
    HashMap<String, Object> poseData;

    /**Which foot is "actively" being used. This interpretation might depend pose to pose.*/
    int activeFoot;

    /**The position and velocity of the center of mass. These are controlled first, and then the specific movements of body parts follow.*/
    BodyPoint massCenter;
    /**The foot (could plausibly be hand) acting as the first point of contact with other stuff, like the ground or a wall or a rope etc
     *  TODO: replace this with a method or reference or something*/
    BodyPoint primaryContact;

    //// Physical components of the body ////
    BodyPoint leftFoot, rightFoot;


    /**The overall velocity of the whole player, as opposed to the nitty gritty velocity of each point*/
    Vector2 overallVel;

    /**Parent world*/
    WorldMap parent;

    /**The current way that the player is moving; e.g. walking, running, jumping, etc*/
    MovementState currentState;

    //// Rendering Utilities ////
    /**The ShapeDrawer the entity should use for rendering. The player sets this to its parent's {@link WorldMap#tempFinalPassShapeDrawer}, unless it lacks a parent, in which case it can be set manually by */
    ShapeDrawer shapeDrawer;

    public Player(Vector2 centerPos, WorldMap parent) {
        this.massCenter = new BodyPoint(BodyPoint.POINT, 0, centerPos);

        Vector2 footStartPos = new Vector2(centerPos).sub(0, LEG_LENGTH);
        Vector2 backFootPos = new Vector2(centerPos).sub(0, LEG_LENGTH*0.8f);
        this.leftFoot = new BodyPoint(BodyPoint.CIRCLE, FOOT_RADIUS, footStartPos);
        this.rightFoot = new BodyPoint(BodyPoint.CIRCLE, FOOT_RADIUS, backFootPos);

//        this.primaryContact = new BodyPoint(BodyPoint.CIRCLE, FOOT_RADIUS, new Vector2(centerPos).sub(0, LEG_LENGTH));
        this.overallVel = new Vector2();
        this.setParentWorld(parent);
        this.attributes = new HashMap<>();
        this.poseData = new HashMap<>();
        this.activeFoot = LEFT_FOOT;
        this.poseData.put("stepRadii", new Vector2(25, 15));

        PoseCatalog.PLAYER_STAND.loadFileData();
        PoseCatalog.PLAYER_STAND.writeToFile();

        this.currentState = MovementState.WALK;
        this.currentState.switchToState(null, this);
    }

    /***/
    @Override
    public void update(float deltaTime, WorldMap world) {
        this.overallVel.sub(0, world.getGravity()*deltaTime);
        this.leftFoot.applyVelocity(this.overallVel, deltaTime);
        this.rightFoot.applyVelocity(this.overallVel, deltaTime);
        this.massCenter.applyVelocity(this.overallVel, deltaTime);

        // State based movement
        MovementState newState = this.currentState.nextMoveState(this);
        if (newState != null) {
            MovementState lastState = this.currentState;
            this.currentState = newState;
            this.currentState.switchToState(lastState, this);
        }
        this.currentState.movePlayer(deltaTime, this);



        BodyPoint activeFoot = this.getActiveFoot();
        System.out.println("Active foot at: "+activeFoot.getPos()+", vs hip at "+this.massCenter.getPos());
        BodyPoint otherFoot = this.getOtherFoot(activeFoot);

        //See if the foot collided with anything, for now (2025-1-17) no slipping
        BodyPoint.PointCollision collision = activeFoot.findCollision(world.getCollidables());
        if (collision != null) {
            Vector2 obstruction = new Vector2(collision.pointPos()).sub(activeFoot.getPos());
            Vector2 newVel = collision.contactVel();
            this.overallVel.set(newVel);
            this.massCenter.getPos().add(obstruction);
            activeFoot.getPos().add(obstruction);
            otherFoot.getPos().add(obstruction);
        }
        this.massCenter.endFrame();
        activeFoot.endFrame();
        otherFoot.endFrame();
    }

    @Override
    public void setParentWorld(WorldMap parent) {
        this.parent = parent;
        parent.addWorldInput(1, new InputAdapter(){
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)) {
                    Player.this.overallVel.set(0,0);
                    Vector2 centerPos = parent.screenToWorldCoords(screenX, screenY);

                    Player.this.massCenter.getPos().set(centerPos);
                    Player.this.massCenter.endFrame();

                    BodyPoint activeFoot, otherFoot;
                    activeFoot = Player.this.getActiveFoot();
                    otherFoot = Player.this.getOtherFoot(activeFoot);
                    activeFoot.getPos().set(centerPos).sub(0, LEG_LENGTH);
                    otherFoot.getPos().set(centerPos).sub(0, LEG_LENGTH*0.8f);
                    activeFoot.endFrame();
                    otherFoot.endFrame();


//                    Player.this.massCenter = new BodyPoint(BodyPoint.POINT, 0, centerPos);
//                    Player.this.primaryContact = new BodyPoint(BodyPoint.CIRCLE, FOOT_RADIUS, new Vector2(centerPos).sub(0, LEG_LENGTH));
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public String getID() {
        return "player";
    }

    @Override
    public void renderToWorld(Batch batch, OrthographicCamera worldCamera) {
        this.shapeDrawer = this.parent.tempFinalPassShapeDrawer;
        ShapeDrawer drawer = this.shapeDrawer;

        drawer.setColor(Color.YELLOW);
        drawer.filledCircle(this.massCenter.getPos(), 1.0f);
        drawer.setColor(Color.CYAN);
        BodyPoint activeFoot = this.getActiveFoot();
        drawer.filledCircle(activeFoot.getPos(), activeFoot.getRadius());
    }

    @Override
    public void debugRender(ShapeRenderer drawer) {
        drawer.setColor(Color.YELLOW);
        Vector2 mc = this.massCenter.getPos();
        drawer.circle(mc.x, mc.y, 2.0f);
//        drawer.filledCircle(this.massCenter.getPos(), 2.0f);
        drawer.setColor(Color.CYAN);
        Vector2 left = this.leftFoot.getPos();
        drawer.circle(left.x, left.y, 1.5f);
        drawer.setColor(Color.CORAL);
        Vector2 right = this.rightFoot.getPos();
        drawer.circle(right.x, right.y, 1.5f);


        drawer.setColor(Color.GOLD);
        ArrayList<Vector2> points = (ArrayList<Vector2>) this.poseData.get("steps");
        for (Vector2 point : points) {
            drawer.circle(point.x, point.y, 1.3f);
        }

    }

    @Override
    public float getAttribute(String attrName) {
        return this.attributes.get(attrName);
    }


    /**Returns whichever of the two feet are currently "in front", i.e. the one that's currently swinging forward, or the one being held out in front in the air.*/
    public BodyPoint getActiveFoot() {
        int activeFoot = this.activeFoot;
        if (activeFoot == RIGHT_FOOT) {
            return this.rightFoot;
        } else if (activeFoot == LEFT_FOOT || activeFoot == NEITHER) {
            return this.leftFoot;
        } else {
            throw new IllegalArgumentException("Flag activeFoot had value of "+activeFoot+", but only meaningful values are "+LEFT_FOOT+" (left), "+RIGHT_FOOT+" (right), or "+NEITHER+" (neither)");
        }
    }

    public BodyPoint getOtherFoot() {
        return this.getOtherFoot(this.getActiveFoot());
    }

    public BodyPoint getOtherFoot(BodyPoint activeFoot) {
        return (activeFoot == this.rightFoot) ? this.leftFoot : this.rightFoot;
    }


    public void setShapeDrawer(ShapeDrawer shapeDrawer) {
        this.shapeDrawer = shapeDrawer;
    }

    public ShapeDrawer getShapeDrawer() {
        return shapeDrawer;
    }

    /***/


    /**A set of instructions to manipulate the player every frame:
     * - How to move the body in a frame
     * - After moving, what state the player should be in next frame
     * - What to do, if after moving in another movement state, the player switches to this movement state
     *As enums, these should not store data themselves, rather in the provided {@link Player} instance, or TODO: a specific data encapsulating class?*/
    public enum MovementState {

        WALK() {
            public static ArrayList<Vector2> calcWalkSteps(Player player) {
                ArrayList<Collidable> colls = player.parent.getCollidables();
                ArrayList<Vector2> points = new ArrayList<>();

                Vector2 stepRadii = (Vector2) player.poseData.get("stepRadii");
                Vector2 stepOrigin = player.getActiveFoot().getPos();

                for (Collidable coll : colls) {
                    coll.forEachSegment(new SegmentProcedure() {
                        @Override
                        public void actOnSegment(float ax, float ay, float bx, float by, float av_x, float av_y, float bv_x, float bv_y, int indexA, int indexB, int normalDirection) {
                            points.add(new Vector2(ax, ay));

                            //Imagine an ellipse around the current foot, and add any intersections between the segment and that ellipse as viable points
                            float rad = 40;
                            Vector2[] inters = new Vector2[2];
                            int numInters = MiscFunctions.segmentIntersectsEllipse(
                                    ax, ay,
                                    bx, by,
                                    stepOrigin.x, stepOrigin.y,
                                    stepRadii.x, stepRadii.y,
                                    inters
                            );
                            for (int i = 0; i < numInters; i++) {
                                points.add(inters[i]);
                            }
                        }
                    });
                }
                return points;
            }

            public static float walkStepDifficulty(Vector2 step, BodyPoint plantedFoot, BodyPoint footToMove, Player player) {
                Vector2 rad = (Vector2) player.poseData.get("stepRadii");
                float idealXRad2 = rad.x*rad.x;
                float m = rad.x / rad.y;
                float equivRadius2 = new Vector2(step).sub(plantedFoot.getPos()).scl(1, m).len2();
                return Math.abs(equivRadius2-idealXRad2);
            }

            public static Vector2 bestWalkStep(ArrayList<Vector2> possibleSteps, BodyPoint plantedFoot, BodyPoint footToMove, Player player) {
                float minStepDifficulty = Float.MAX_VALUE;
                Vector2 easiestStep = new Vector2(footToMove.getPos());
                for (Vector2 step : possibleSteps) {
                    float diff = walkStepDifficulty(step, plantedFoot, footToMove, player);
                    if (diff < minStepDifficulty) {
                        easiestStep = step;
                        minStepDifficulty = diff;
                    }
                }
                return easiestStep;
            }

            public static Vector2 bestHipPos(BodyPoint frontFoot, BodyPoint backFoot) {
                return new Vector2(frontFoot.getPos()).add(backFoot.getPos()).scl(0.5f).add(0, LEG_LENGTH*0.6f);
            }

            @Override
            public MovementState nextMoveState(Player player) {
                // If calling for a new step, reset by switching to this state again
                if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                    return this;
                }
                return null;
            }

            @Override
            public void switchToState(MovementState previousState, Player player) {
                // Find viable steps from the last active foot
                player.poseData.put("steps", calcWalkSteps(player));
                // Switch active foot to the other one
                player.activeFoot *= -1;
                player.poseData.put("alrMoved", false);
            }

            @Override
            public void movePlayer(float deltaTime, Player player) {
                HashMap<String, Object> data = player.poseData;
                if (data.containsKey("alrMoved") && (boolean) data.get("alrMoved")) {
                    // Already moved
                    // Do nothing?
                } else {
                    // Teleport foot directly to desired position, if able
                    BodyPoint moving = player.getActiveFoot();
                    BodyPoint planted = player.getOtherFoot(moving);
                    Vector2 newPos = bestWalkStep((ArrayList<Vector2>) data.get("steps"), planted, moving, player);
                    if (new Vector2(newPos).sub(planted.getPos()).len() < LEG_LENGTH || true) {
                        moving.getPos().set(newPos).add(0, FOOT_RADIUS*2.25f);
                        moving.endFrame();
                        Vector2 hipPos = bestHipPos(moving, planted);
                        player.massCenter.getPos().set(hipPos);
                    }
                    data.put("alrMoved", true);
                }
            }

        }

        ;
        public void movePlayer(float deltaTime, Player player){}

        /**Returns the next movement state to switch to, if any. Returns null if the player should stay on this movement state without switching*/
        public MovementState nextMoveState(Player player){return null;}
        public void switchToState(MovementState previousState, Player player){}



    }

}

package com.mygdx.tempto.entity.player;

import static com.mygdx.tempto.input.InputTranslator.GameInputs.*;

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
import com.mygdx.tempto.entity.pose.Pose;
import com.mygdx.tempto.entity.pose.PoseCatalog;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.rendering.RendersToDebug;
import com.mygdx.tempto.rendering.RendersToWorld;
import com.mygdx.tempto.util.MiscFunctions;
import com.mygdx.tempto.input.InputTranslator.GameInputs.*;

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

    /**Container of input data like current direction, last inputs, etc.*/
    PlayerInputData input;

    /**Listener to feed into {@link #input}*/
    InputAdapter inputListener;

    /**Which foot is "actively" being used. This interpretation might depend pose to pose.*/
    int activeFoot;

    /**The position and velocity of the center of mass. These are controlled first, and then the specific movements of body parts follow.*/
    BodyPoint massCenter;

    //// Physical components of the body ////
    BodyPoint leftFoot, rightFoot;
    BodyPoint leftHand, rightHand;
    BodyPoint hip, chest;

    BodyPoint[] points;

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

        this.leftHand = new BodyPoint(BodyPoint.POINT, 0, centerPos);
        this.rightHand = new BodyPoint(BodyPoint.POINT, 0, centerPos);
        this.hip = new BodyPoint(BodyPoint.POINT, 0, centerPos);
        this.chest = new BodyPoint(BodyPoint.POINT, 0, centerPos);

        this.points = new BodyPoint[] {
                this.massCenter, //Are we actually gonna use this point?
                this.leftFoot,
                this.rightFoot,
                this.leftHand,
                this.rightHand,
                this.hip,
                this.chest,
        };

//        this.primaryContact = new BodyPoint(BodyPoint.CIRCLE, FOOT_RADIUS, new Vector2(centerPos).sub(0, LEG_LENGTH));
        this.overallVel = new Vector2();
        this.setParentWorld(parent);
        this.attributes = new HashMap<>();
        this.poseData = new HashMap<>();
        this.activeFoot = LEFT_FOOT;
        this.poseData.put("stepRadii", new Vector2(16, 16));

        PoseCatalog.PLAYER_STAND.loadFileData();
        PoseCatalog.PLAYER_STAND.writeToFile();


        this.currentState = MovementState.WALK;
        this.currentState.switchToState(null, this);
    }

    /***/
    @Override
    public void update(float deltaTime, WorldMap world) {


//        System.out.println("Start of frame active: "+activeFoot.getPos() + ", other: "+otherFoot.getPos());


        this.overallVel.sub(0, world.getGravity()*deltaTime);
        for (BodyPoint point : this.points) {
            point.applyVelocity(this.overallVel, deltaTime);
        }
//        this.leftFoot.applyVelocity(this.overallVel, deltaTime);
//        this.rightFoot.applyVelocity(this.overallVel, deltaTime);
//        this.massCenter.applyVelocity(this.overallVel, deltaTime);

        // State based movement
        MovementState newState = this.currentState.nextMoveState(this);
        if (newState != null) {
            MovementState lastState = this.currentState;
            this.currentState = newState;
            this.currentState.switchToState(lastState, this);
        }
        this.currentState.movePlayer(deltaTime, this);

//        BodyPoint activeFoot = this.getActiveFoot();
        BodyPoint contactFoot = this.getActiveFoot();

        //See if the foot collided with anything, for now (2025-1-17) no slipping
        BodyPoint.PointCollision collision = contactFoot.findCollision(world.getCollidables());
        if (collision != null) {
            Vector2 obstruction = new Vector2(collision.pointPos()).sub(contactFoot.getPos());
            for (BodyPoint point : this.points) point.getPos().add(obstruction);

            Vector2 newVel = collision.contactVel();
            this.overallVel.set(newVel);
        }

        // Resolve any inherent overlaps
        contactFoot.resolveOverlap(world.getCollidables(), this.points);

        // Repeat for other foot if necessary
        BodyPoint otherFoot = this.getOtherFoot(contactFoot);
        BodyPoint.PointCollision secondaryCollision = contactFoot.findCollision(world.getCollidables());
        if (secondaryCollision != null) {
            Vector2 obstruction = new Vector2(secondaryCollision.pointPos()).sub(contactFoot.getPos());
            for (BodyPoint point : this.points) point.getPos().add(obstruction);

            Vector2 newVel = secondaryCollision.contactVel();
            this.overallVel.set(newVel);
        }
        contactFoot.resolveOverlap(world.getCollidables(), this.points);


        for (BodyPoint point : this.points) point.endFrame();
    }

    @Override
    public void setParentWorld(WorldMap parent) {
        this.parent = parent;
        this.input = new PlayerInputData();
        parent.addWorldInput(1, this.input);
        parent.addWorldInput(2, new InputAdapter(){
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

        // All points
        drawer.setColor(Color.RED);
        for (BodyPoint point : this.points) {
            Vector2 pos = point.getPos();
            drawer.circle(pos.x, pos.y, 1f);
        }


        drawer.setColor(Color.GOLD);
        ArrayList<Vector2> points = (ArrayList<Vector2>) this.poseData.get("steps");
        for (Vector2 point : points) {
            drawer.circle(point.x, point.y, 1.3f);
        }

        // Debug draw input direction to make sure we set it up right
        float inputVecLen = 3;
        Vector2 hipPos = this.massCenter.getPos();
        drawer.rectLine(hipPos, new Vector2(this.input.inputDirection).scl(inputVecLen).add(hipPos), 1);
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

    public BodyPoint getActiveHand(BodyPoint activeFoot) {
        return (activeFoot == this.rightFoot) ? this.rightHand : this.leftHand;
    }

    public BodyPoint getOtherHand(BodyPoint activeFoot) {
        return (activeFoot == this.rightFoot) ? this.leftHand : this.rightHand;
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

//                int desiredDirection = 1; //For now always assume right
//                int desiredDirection = -1; // Now try always left
                // it being the leg's length is a magic number; we'll have to test what step length feels best, and obviously that differs from left/right vs up/down
                Vector2 desiredDir = new Vector2(player.input.inputDirection).scl(LEG_LENGTH);
                desiredDir.y *= 0.4f;
                Vector2 desiredPos = new Vector2(desiredDir).add(plantedFoot.getPos());

                float diff2 = desiredPos.sub(step).len2();

//                float desiredX = plantedFoot.getPos().x + desiredDirection*LEG_LENGTH; //Magic number to get this right
//                float xDiff = step.x - desiredX;

                return Math.abs(equivRadius2-idealXRad2) + diff2;
            }

            public static Vector2 bestWalkStep(ArrayList<Vector2> possibleSteps, BodyPoint plantedFoot, BodyPoint footToMove, Player player) {
                float minStepDifficulty = Float.MAX_VALUE;
                Vector2 easiestStep = new Vector2(footToMove.getPos());
                for (Vector2 step : possibleSteps) {
                    if (Float.isNaN(step.x) || Float.isNaN(step.y)) continue;

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
                if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                    return this;
                }
                return null;
            }

            @Override
            public void switchToState(MovementState previousState, Player player) {
                // Find viable steps from the last active foot
                HashMap<String, Object> data = player.poseData;
                data.put("steps", calcWalkSteps(player));
                // Switch active foot to the other one
                player.activeFoot *= -1;
                data.put("alrMoved", false);

                // Start the act of moving the new active foot towards the next target
                BodyPoint moving = player.getActiveFoot();
                BodyPoint planted = player.getOtherFoot(moving);
                Vector2 newPos = bestWalkStep((ArrayList<Vector2>) data.get("steps"), planted, moving, player);
                newPos.add(0, moving.getRadius());
//                if (new Vector2(newPos).sub(planted.getPos()).len() < LEG_LENGTH || true) {
//                    moving.getPos().set(newPos).add(0, 0.1f);
//                    moving.endFrame();
//                    Vector2 hipPos = bestHipPos(moving, planted);
//                    player.massCenter.getPos().set(hipPos);
//                }

                data.put("moveStart", new Vector2(player.getActiveFoot().getPos()));
                data.put("moveProgress", 0f);
                data.put("nextTarget", newPos);
            }

            @Override
            public void movePlayer(float deltaTime, Player player) {
                HashMap<String, Object> data = player.poseData;
                if (data.containsKey("alrMoved") && (boolean) data.get("alrMoved")) {
                    // Already moved
                    // Do nothing?
                } else {
                    float lastMoveProg = (float) data.get("moveProgress");
                    float speed = 1.2f;
                    float newMoveProg = MiscFunctions.clamp(lastMoveProg + deltaTime*speed, 0, 1);

                    data.put("moveProgress", newMoveProg);

                    Vector2 resultPos = new Vector2((Vector2) data.get("moveStart")).lerp((Vector2) data.get("nextTarget"), newMoveProg);

                    float maxExtraHeight = 5;
                    float extraHeight = 1f - (float) Math.pow(2*Math.abs(newMoveProg-0.5f), 10f); //Go almost straight up and then down again at the end
                    extraHeight *= maxExtraHeight;

                    resultPos.add(0, extraHeight);
                    player.getActiveFoot().setPos(resultPos);
                    player.getActiveFoot().endFrame();

                    if (newMoveProg >= 1f) data.put("alrMoved", true);
                }

                //Set other points based on pose
                PoseCatalog walk = PoseCatalog.PLAYER_WALK1;
                BodyPoint moving = player.getActiveFoot();
                BodyPoint planted = player.getOtherFoot(moving);
                Vector2 plantedPos = planted.getPos();
                Vector2 movingRelPos = new Vector2(moving.getPos()).sub(plantedPos);
                int dirMult = movingRelPos.x>=0? 1 : -1;
                movingRelPos.x *= dirMult;

                Pose walkPoints = walk.getPoseForInput(movingRelPos).scale(dirMult, 1).shift(plantedPos);

                player.hip.setPos(walkPoints.get("hip"));
                player.chest.setPos(walkPoints.get("chest"));

                BodyPoint mfHand = player.getActiveHand(moving);
                BodyPoint pfHand = player.getOtherHand(planted);

//                System.out.println("mfHand before: "+mfHand.getPos());
                mfHand.setPos(walkPoints.get("mf_hand"));
//                System.out.println("mfHand: "+mfHand.getPos());
                pfHand.setPos(walkPoints.get("pf_hand"));
//                System.out.println("mfHand after setting pfHand: "+mfHand.getPos());
//                System.out.println("Front foot relative: "+movingRelPos+", planted: "+plantedPos+", hip: "+player.hip.getPos()+", chest: "+ player.chest.getPos()+", mfHand: "+mfHand.getPos()+", pfHand: "+pfHand.getPos());
            }

        }

        ;
        public void movePlayer(float deltaTime, Player player){}

        /**Returns the next movement state to switch to, if any. Returns null if the player should stay on this movement state without switching*/
        public MovementState nextMoveState(Player player){return null;}
        public void switchToState(MovementState previousState, Player player){}



    }


    public class PlayerInputData extends InputAdapter{

        /**Tracking booleans for if each button is pressed, since we just get input about when one goes up or down.
         * (e.g., if you hold down left and then right, but then let go of right, it should know to go back to left)*/
        private boolean leftHeld, rightHeld, upHeld, downHeld;

        /**A vector corresponding to the directions of buttons being pressed, i.e. x is -1, 1, or 0 if left/right/neither buttons are pressed, and likewise for y*/
        public Vector2 inputDirection;

        public PlayerInputData() {
            this.inputDirection = new Vector2();
        }

        @Override
        public boolean keyDown(int keycode) {
            switch(keycode) {
                case LEFT -> {
                    this.leftHeld = true;
                    this.inputDirection.x = -1;
                }
                case RIGHT -> {
                    this.rightHeld = true;
                    this.inputDirection.x = 1;
                }
                case UP -> {
                    this.upHeld = true;
                    this.inputDirection.y = 1;
                }
                case DOWN -> {
                    this.downHeld = true;
                    this.inputDirection.y = -1;
                }
                default -> {return false;}
            }
            return true;
        }

        @Override
        public boolean keyUp(int keycode) {
            switch(keycode) {
                case LEFT -> {
                    this.leftHeld = false;
                    this.inputDirection.x = this.rightHeld? 1 : 0;
                }
                case RIGHT -> {
                    this.rightHeld = false;
                    this.inputDirection.x = this.leftHeld? -1 : 0;
                }
                case UP -> {
                    this.upHeld = false;
                    this.inputDirection.y = this.downHeld? -1 : 0;
                }
                case DOWN -> {
                    this.downHeld = false;
                    this.inputDirection.y = this.upHeld? 1 : 0;
                }
                default -> {return false;}
            }
            return true;
        }
    }

}

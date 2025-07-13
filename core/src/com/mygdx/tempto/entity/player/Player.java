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

import org.lwjgl.opengl.NVTextureEnvCombine4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

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
    static final float LEG_LENGTH = 7f;

    /**These are values that are set by design (leg height, walk stride distance, etc.) but that might need to be tinkered with on the fly, for development*/
    HashMap<String, Float> attributes;

    /**Arbitrary pieces of information that a pose might need to save, but might not justify storing a whole explicit variable just for that one pose*/
    HashMap<String, Object> poseData;

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
        this.primaryContact = new BodyPoint(BodyPoint.CIRCLE, FOOT_RADIUS, new Vector2(centerPos).sub(0, LEG_LENGTH));
        this.overallVel = new Vector2();
        this.setParentWorld(parent);
        this.attributes = new HashMap<>();
        this.poseData = new HashMap<>();
        PoseCatalog.PLAYER_STAND.loadFileData();
        PoseCatalog.PLAYER_STAND.writeToFile();
    }

    /***/
    @Override
    public void update(float deltaTime, WorldMap world) {
        this.overallVel.sub(0, world.getGravity()*deltaTime);
        this.primaryContact.applyVelocity(this.overallVel, deltaTime);
        this.massCenter.applyVelocity(this.overallVel, deltaTime);

        this.currentState = MovementState.WALK;
        this.currentState.movePlayer(deltaTime, this);

        //See if the foot collided with anything, for now (2025-1-17) no slipping
        BodyPoint.PointCollision collision = this.primaryContact.findCollision(world.getCollidables());
        if (collision != null) {
            Vector2 obstruction = new Vector2(collision.pointPos()).sub(this.primaryContact.getPos());
            Vector2 newVel = collision.contactVel();
            this.overallVel.set(newVel);
            this.massCenter.getPos().add(obstruction);
            this.primaryContact.getPos().add(obstruction);
        }
        this.massCenter.endFrame();
        this.primaryContact.endFrame();
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
                    Player.this.massCenter = new BodyPoint(BodyPoint.POINT, 0, centerPos);
                    Player.this.primaryContact = new BodyPoint(BodyPoint.CIRCLE, FOOT_RADIUS, new Vector2(centerPos).sub(0, LEG_LENGTH));
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
        drawer.filledCircle(this.primaryContact.getPos(), this.primaryContact.getRadius());
    }

    @Override
    public void debugRender(ShapeRenderer drawer) {
        drawer.setColor(Color.YELLOW);
        Vector2 mc = this.massCenter.getPos();
        drawer.circle(mc.x, mc.y, 2.0f);
//        drawer.filledCircle(this.massCenter.getPos(), 2.0f);

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
                for (Collidable coll : colls) {
                    coll.forEachSegment(new SegmentProcedure() {
                        @Override
                        public void actOnSegment(float ax, float ay, float bx, float by, float av_x, float av_y, float bv_x, float bv_y, int indexA, int indexB, int normalDirection) {
                            points.add(new Vector2(ax, ay));
                        }
                    });
                }
                return points;
            }
            @Override
            public void movePlayer(float deltaTime, Player player) {
                player.poseData.put("steps", calcWalkSteps(player));
            }
        }

        ;
        public void movePlayer(float deltaTime, Player player){}
        public MovementState nextMoveState(Player player){return this;}
        public void switchToState(MovementState previousState, Player player){}



    }

}

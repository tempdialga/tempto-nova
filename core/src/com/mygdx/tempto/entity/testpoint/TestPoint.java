package com.mygdx.tempto.entity.testpoint;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.tempto.entity.Entity;
import com.mygdx.tempto.entity.physics.BodyPoint;
import com.mygdx.tempto.entity.pose.Posable;
import com.mygdx.tempto.entity.pose.Pose;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.rendering.RendersToWorld;
import com.mygdx.tempto.util.MiscFunctions;

import space.earlygrey.shapedrawer.ShapeDrawer;

public class TestPoint implements Entity, RendersToWorld, Posable {

    public static final String TEST_POINT_ID_BASE = "testPoint";

    protected BodyPoint body;
    protected Vector2 vel;
    protected WorldMap parent;
    protected String ID;
    protected Pose currentPose;

    public TestPoint(Vector2 pos, WorldMap parent) {
        this.setParentWorld(parent);
        this.body = new BodyPoint(BodyPoint.CIRCLE, 2, new Vector2(pos));
        this.vel = new Vector2();
        parent.addWorldInput(new InputAdapter(){
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (TestPoint.this.parent.isEditing()) return false;

                if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                    Vector2 clickPos = TestPoint.this.parent.screenToWorldCoords(screenX, screenY);
                    TestPoint.this.currentPose = new GlideToPoint(
                            TestPoint.this,
                            new Vector2(TestPoint.this.body.getPos()),
                            clickPos,
                            5
                    );
                } else {
                    TestPoint.this.currentPose = null;
                    Vector2 clickPos = TestPoint.this.parent.screenToWorldCoords(screenX, screenY);
                    TestPoint.this.vel.set(0, 0);
                    TestPoint.this.body.getPos().set(clickPos);
                    TestPoint.this.body.endFrame();
                }
                return true;
            }
        });
    }

    @Override
    public void update(float deltaTime, WorldMap world) {
        if (this.currentPose instanceof GlideToPoint glide) {
//            Vector2 before = glide.getOutputPoint(GlideToPoint.MOVING_FOOT_OUTPUT);
            this.currentPose.update(deltaTime);
            Vector2 after = glide.getOutputPoint(GlideToPoint.MOVING_FOOT_OUTPUT);
            this.body.getPos().set(after);
            this.vel.set(glide.getVel());

            if (glide.getT() >= 1) {
                if (glide.isReversed()) {
                    this.currentPose = null;
                } else {
                    glide.reverse();
                }
            }
        } else {
            this.vel.y -= world.getGravity() * deltaTime;
            this.body.applyVelocity(this.vel, deltaTime);
        }

        BodyPoint.PointCollision collision = this.body.findCollision(world.getCollidables());

        int maxSlipIterations = 20;
        int maxIterations = 1000;
        int c = 0;

        while (collision != null) {
//            System.out.println("Collision at "+collision.collisionPos()+", going from "+this.body.getLastFramePos()+" to "+this.body.getPos());
            Vector2 obstruction = new Vector2(collision.pointPos()).sub(this.body.getPos());
            Vector2 normal = collision.normalToSurface();

            if (c < maxSlipIterations) {
                Vector2 normalObstruction = MiscFunctions.projectAontoB(obstruction, normal);
                this.body.getPos().add(normalObstruction);
                this.body.getPos().add(normal.x*BodyPoint.DEFAULT_COLLISION_BUFFER, normal.y*BodyPoint.DEFAULT_COLLISION_BUFFER);
            } else {
                this.body.getPos().add(obstruction);
            }



            Vector2 normalVel = MiscFunctions.projectAontoB(this.vel, normal);

            //Elastic collision
            float restit_coeff = 0.9f;
            this.vel.sub(normalVel.scl(2*restit_coeff));


            collision = this.body.findCollision(world.getCollidables());
            if (c > maxSlipIterations && c < maxSlipIterations+10) {
                System.out.println(c+" iterations needed, more than allowed for slipping");
            }
            if (c++ > maxIterations) {
                System.out.println(">"+maxIterations+" iterations to resolve collision");
                break;
            }
        }

        this.body.resolveOverlap(world.getCollidables());
        this.body.endFrame();

    }



    @Override
    public void setParentWorld(WorldMap parent) {
        this.parent = parent;
        this.ID = parent.nextAvailableProceduralID(TEST_POINT_ID_BASE);
    }

    @Override
    public String getID() {
        return this.ID;
    }

    @Override
    public void renderToWorld(SpriteBatch batch, OrthographicCamera worldCamera) {
        ShapeDrawer drawer = this.parent.shapeDrawer;
        drawer.setColor(Color.WHITE);
        float radius = this.body.getRadius();
        if (radius <= 0) radius = 2;
        drawer.filledCircle(this.body.getPos(), radius);
    }
}

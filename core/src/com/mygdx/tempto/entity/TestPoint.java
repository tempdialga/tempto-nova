package com.mygdx.tempto.entity;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.tempto.entity.physics.BodyPoint;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.rendering.RendersToWorld;
import com.mygdx.tempto.util.MiscFunctions;

import space.earlygrey.shapedrawer.ShapeDrawer;

public class TestPoint implements Entity, RendersToWorld {

    public static ShapeDrawer drawer;

    public static final String TEST_POINT_ID_BASE = "testPoint";

    protected BodyPoint body;
    protected Vector2 vel;
    protected WorldMap parent;
    protected String ID;

    public TestPoint(Vector2 pos, WorldMap parent) {
        this.setParentWorld(parent);
        this.body = new BodyPoint(BodyPoint.CIRCLE, 2, new Vector2(pos));
        this.vel = new Vector2();
        parent.addWorldInput(new InputAdapter(){
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (TestPoint.this.parent.isEditing()) return false;

                Vector2 clickPos = TestPoint.this.parent.screenToWorldCoords(screenX, screenY);
                TestPoint.this.vel.set(0,0);
                TestPoint.this.body.getPos().set(clickPos);
                TestPoint.this.body.endFrame();
                return true;
            }
        });
    }

    @Override
    public void update(float deltaTime, WorldMap world) {
        this.vel.y -= world.getGravity()*deltaTime;
        this.body.applyVelocity(this.vel, deltaTime);

        BodyPoint.PointCollision collision = this.body.findCollision(world.getCollidables());

        int maxIterations = 1000;
        int c = 0;

        while (collision != null) {
//            System.out.println("Collision at "+collision.collisionPos()+", going from "+this.body.getLastFramePos()+" to "+this.body.getPos());
            Vector2 obstruction = new Vector2(collision.pointPos()).sub(this.body.getPos());
            Vector2 normal = collision.normalToSurface();
            Vector2 normalObstruction = MiscFunctions.projectAontoB(obstruction, normal);

            this.body.getPos().add(normalObstruction);
            this.body.getPos().add(normal.x*BodyPoint.DEFAULT_COLLISION_BUFFER, normal.y*BodyPoint.DEFAULT_COLLISION_BUFFER);

            Vector2 normalVel = MiscFunctions.projectAontoB(this.vel, normal);

            //Elastic collision
            float restit_coeff = 0.9f;
            this.vel.sub(normalVel.scl(2*restit_coeff));
            collision = this.body.findCollision(world.getCollidables());

            if (c++ > maxIterations) {
                System.out.println(">"+maxIterations+" iterations to resolve collision");
                break;
            }
        }

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
        if (drawer == null) {
            drawer = new ShapeDrawer(batch);
            drawer.setTextureRegion(new TextureRegion(this.parent.blankTexture));

        }
        drawer.setColor(Color.WHITE);
        float radius = this.body.getRadius();
        if (radius <= 0) radius = 2;
        drawer.filledCircle(this.body.getPos(), radius);
    }
}

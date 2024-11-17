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
        this.body = new BodyPoint(BodyPoint.POINT, 0, new Vector2(pos));
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

        BodyPoint.PointOnSegmentCollision collision = this.body.findCollision(world.getCollidables());

        if (collision != null) {
            this.body.getPos().set(collision.collisionPos());
            this.vel.set(0,0);
        }
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
        drawer.filledCircle(this.body.getPos(), 2);
    }
}

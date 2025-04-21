package com.mygdx.tempto.entity.testpoint;

import com.badlogic.gdx.math.Vector2;
import com.mygdx.tempto.entity.pose.Posable;
import com.mygdx.tempto.entity.pose.Pose;
import com.mygdx.tempto.util.MiscFunctions;

import java.util.Map;

public class GlideToPoint extends Pose {

    public static final String MOVING_FOOT_START = "MF_START";
    public static final String MOVING_FOOT_END = "MF_END";

    public static final String MOVING_FOOT_OUTPUT = "MF_O";

    private Vector2 start;
    private Vector2 end;

    private float speed;
    private Vector2 vel;


    protected float T;

    private boolean reversed;
    public GlideToPoint(Vector2 start, Vector2 end, float speed) {
        super(Map.of(MOVING_FOOT_START, start, MOVING_FOOT_END, end));
        this.assertKeysPresent(MOVING_FOOT_START, MOVING_FOOT_END);

        this.start = start;
        this.end = end;
        this.T = 0;

        this.outputPoints = Map.of(MOVING_FOOT_OUTPUT, new Vector2(start));

        this.speed = start.dst(end)*speed;
        this.vel = new Vector2(this.end).sub(this.start).nor().scl(this.speed);
    }

    @Override
    public void update(float deltaTime) {
        float TtoMove = this.speed * deltaTime / this.end.dst(this.start);
        this.setT(MiscFunctions.clamp(this.T + TtoMove, 0, 1));

        this.getOutputPoint(MOVING_FOOT_OUTPUT).set(MiscFunctions.interpolateLinearPath(this.start, this.end, this.T));
    }

    public float getSpeed() {
        return speed;
    }

    public Vector2 getVel() {
        return vel;
    }

    public boolean isReversed() {
        return reversed;
    }

    public void reverse() {
        this.reversed = !this.reversed;
        Vector2 temp = new Vector2(this.start);
        this.start.set(this.end);
        this.end.set(temp);

        this.vel.scl(-1);
        this.T = 1-this.T;
    }


    public float getT() {return this.T;}
    public void setT(float T) {this.T = T;}
}

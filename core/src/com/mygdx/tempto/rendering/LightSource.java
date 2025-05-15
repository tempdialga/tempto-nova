package com.mygdx.tempto.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

public record LightSource(Vector3 pos, Color color, float spread, int shape, float bodyRadius) {
    public static final int SPHERE_APPROX = 0;
}

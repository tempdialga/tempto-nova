package com.mygdx.tempto.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

public record LightSource(Vector3 pos, Color color, float radius) {
}

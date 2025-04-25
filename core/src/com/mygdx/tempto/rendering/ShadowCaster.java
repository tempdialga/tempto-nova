package com.mygdx.tempto.rendering;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector3;

import java.util.HashMap;

/**A class representing a parallelogram which casts shadows, with a given texture region and its given coordinates.
 * @param shadowTexture The texture region the shadow should sample.
 * @param origin Point in world space corresponding to (0,0) on the texture, i.e. a (x, y, and then z of depth)
 * @param u Vectors from origin to each of the corresponding axes of the shadow texture region, i.e. ab and ac
 * @param v Vectors from origin to each of the corresponding axes of the shadow texture region, i.e. ab and ac
 */
public record ShadowCaster (TextureRegion shadowTexture, Vector3 origin, Vector3 u, Vector3 v) {

    /**Some public static details for debugging / record keeping*/

    public static final HashMap<ShadowCaster, Polygon> CASTER_RANGES = new HashMap<>();
    public static int numRangesVisible = 0;
}

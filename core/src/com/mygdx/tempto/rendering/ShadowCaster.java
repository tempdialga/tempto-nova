package com.mygdx.tempto.rendering;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector3;

import java.util.HashMap;

/**
 * A class representing a parallelogram which casts shadows, with a given texture region and its given coordinates.
 *
 * @param shadowTexture The texture region the shadow should sample.
 * @param origin        Point in world space corresponding to (0,0) on the texture, i.e. a (x, y, and then z of depth)
 * @param u             Vectors from origin to each of the corresponding axes of the shadow texture region, i.e. ab and ac
 * @param v             Vectors from origin to each of the corresponding axes of the shadow texture region, i.e. ab and ac
 * @param flat          Whether the tile is "flat" or not. If true, the game won't sample any textures for the shadow caster, and will just draw shadows of a basic quad.
 */
public record ShadowCaster (TextureRegion shadowTexture, Vector3 origin, Vector3 u, Vector3 v,
                            boolean flat, boolean triangle) implements Comparable<ShadowCaster> {

    /**Some public static details for debugging / record keeping*/

    public static final HashMap<ShadowCaster, Polygon> CASTER_RANGES = new HashMap<>();
    public static int numRangesVisible = 0;

    public ShadowCaster(TextureRegion shadowTexture, Vector3 origin, Vector3 u, Vector3 v, boolean flat) {
        this(shadowTexture, origin, u, v, flat, false);
    }

    @Override
    public int compareTo(ShadowCaster o) {
        return Float.compare(o.origin.z, this.origin.z);
    }

    public int shadowShader(LightSource light) {
        boolean useFlat = this.flat;
        if (this.triangle()) {
            if (true) return AltShadeBatch.NINE_SAMPLE_TRI;
            boolean useNineSamp = true;
            if (useNineSamp) {
                if (useFlat & false) {

                } else {
                    return AltShadeBatch.NINE_SAMPLE_TRI;
                }
            } else {
                if (useFlat && false) {
                    return AltShadeBatch.ONE_FLAT_TRI;
                } else {
                    return AltShadeBatch.ONE_SAMPLE_TRI;
                }
            }
        }
//        useFlat = true;

        if (light.bodyRadius() > 0 && true) {
            if (useFlat) {
                return AltShadeBatch.NINE_FLAT;
            } else {
                return AltShadeBatch.NINE_SAMPLE;
            }
        } else {
            if (useFlat) {
                return AltShadeBatch.ONE_FLAT;
            } else {
                return AltShadeBatch.ONE_SAMPLE;
            }
        }
    }
}

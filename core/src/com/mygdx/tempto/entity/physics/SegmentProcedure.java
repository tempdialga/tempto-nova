package com.mygdx.tempto.entity.physics;

/**An interface designed to interact with {@link Collidable} entities, to request each segment.*/
public interface SegmentProcedure {

    /**Directions that a segment could be normal to, where NO_NORMAL means none, CC_NORMAL means 90 degrees counterclockwise from A->B, and CL_NORMAL clockwise.
     * For example, a clockwise wound polygon terrain would be CC_NORMAL.*/
    int NO_NORMAL = 0, CC_NORMAL = 1, CL_NORMAL = -1;

    /**Performs some kind of interaction with the given information about a segment of a {@link Collidable} implementor, where each segment is from one point to another, points are consistently indexed, and point A can be assumed to lead to point B:
     *
     * @param ax X of the first point (A)
     * @param ay Y of the first point (A)
     * @param bx X of the second point (B)
     * @param by Y of the second point (B)
     * @param av_x X velocity of the first point (A)
     * @param av_y Y velocity of the first point (A)
     * @param bv_x X velocity of the second point (B)
     * @param bv_y Y Velocity of the second point (B)
     * @param indexA Index of the first point (A), where a float on the range (indexA, indexA+1) can always be assumed to represent a point between A and B
     * @param indexB Index of the second point (B). Not as relevant as indexA, since the index range (indexA, indexA+1) is used to represent the segment between the two. For example usage, a colliding entity might want to later independently reference either endpoint of the spectrum.
     * */
    void actOnSegment(float ax, float ay, float bx, float by, float av_x, float av_y, float bv_x, float bv_y, int indexA, int indexB, int normalDirection);

}

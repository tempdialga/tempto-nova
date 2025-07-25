package com.mygdx.tempto.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Null;
import com.mygdx.tempto.entity.physics.Collidable;

import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**A class with miscellaneous utility functions*/
public class MiscFunctions {
    /**Returns either the value, or, if the value does not fit in the given boundaries, the boundary closest to the value.
     * Assumption: max > min*/
    public static int clamp(int value, int min, int max) {
        return Math.max(Math.min(value, max), min);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(Math.min(value, max), min);
    }

    /**Returns if the given string represents an integer value*/
    public static boolean isInteger(String str) {
        if (str == null) return false; //Can't be an integer if there's nothing

        int length = str.length();
        if (length == 0) return false; //Can't be an integer if there's nothing

        int i = 0;
        if (str.charAt(0) == '-') { //If it starts with a minus sign it still might be an integer
            if (length == 1) return false; //As long as there's stuff after it
            i = 1;
        }
        for (; i < length; i++) { //For each remaining character:
            char c = str.charAt(i);
            if (c < '0' || c > '9') return false; //If it's not a character between 0 and 9 it isn't an integer (an analogous method to check hex would just use different chars)
        }
        return true; //If every character is an integer from 0-9 permitting a minus sign in front, it should be an integer :)
    }

    public static final float DEFAULT_MERGE_TOLERANCE = 0.1f;

    /**Returns the union, not including holes, of the two given clockwise wound polygons.
     * The return is a new object, and the two existing polygons are not modified.
     */
    public static Polygon polygonUnion(Polygon a, Polygon b) {
        return polygonUnion(a, b, DEFAULT_MERGE_TOLERANCE);
    }
    /**Returns the union, not including holes, of the two given clockwise wound polygons.
     * The return is a new object, and the two existing polygons are not modified.
     * @param tolerance How far apart points or segments can be from each other before they are considered to overlap
     */
    public static Polygon polygonUnion(Polygon a, Polygon b, float tolerance) {
        // Brief utility class to store information about points and intersections
        class UnionPoint {
            public Vector2 pos;
            public float index;
            public boolean isIntersection;
            public UnionPoint match;
            public ArrayList<UnionPoint> parent;

            public UnionPoint(Vector2 pos, float index) {
                this.pos = pos;
                this.index = index;
            }
            public void setMatch(UnionPoint match) {
                this.match = match;
                this.isIntersection = true;
            }

            /**Assigns the given polygon as this point's parent, returns this UnionPoint*/
            public UnionPoint setParent(ArrayList<UnionPoint> parent) {
                this.parent = parent;
                return this;
            }



            /**Adds this point to an arraylist of points, ordered by increasing index TODO: if necessary, make this faster with a binary search*/
            public void addToPolygon(ArrayList<UnionPoint> polygon) {
                this.parent = polygon;
                for (int i = 0; i < polygon.size(); i++) { //Proceed through points at increasing indices
                    UnionPoint atIndexNow = polygon.get(i);
                    if (atIndexNow.index > this.index) { //As soon as a point is found which has a higher index than this one, put it before that point
                        polygon.add(i, this);
                        return;
                    }
                }
                //If this point has a higher index than everything else, add it last
                polygon.add(this);
            }

            /**Looks for a point in the given polygon at the same coordinates as the given point, and returns it.
             * Returns null if none are found. TODO: if necessary, optimize with a binary search*/
            public static UnionPoint getPointAtPosition(ArrayList<UnionPoint> polygon, Vector2 point) {
                for (UnionPoint polyPoint : polygon) {
                    if (polyPoint.pos.epsilonEquals(point)) {
                        return polyPoint;
                    }
                }
                return null;
            }

            @Override
            public String toString() {
                return "{idx: "+this.index+", "+this.pos+", inter: "+this.isIntersection+"}";
            }
        }



        // Create ArrayLists to represent the vertices of A and B, including intersections with the other polyons
        ArrayList<UnionPoint> allAPoints = new ArrayList<>();
        ArrayList<UnionPoint> allBPoints = new ArrayList<>();

        // Populate them with the original points
        for (int i = 0; i < a.getVertexCount(); i++) allAPoints.add(new UnionPoint(a.getVertex(i, new Vector2()), i).setParent(allAPoints));
        System.out.println("Polygon A:" + allAPoints);
        for (int i = 0; i < b.getVertexCount(); i++) allBPoints.add(new UnionPoint(b.getVertex(i, new Vector2()), i).setParent(allBPoints));
        System.out.println("Polygon B:" + allBPoints);

        ArrayList<UnionPoint> originalAPoints = new ArrayList<>(allAPoints);
        ArrayList<UnionPoint> originalBPoints = new ArrayList<>(allBPoints);

        // Populate them with any intersections between the polygons
        //float tolerance = 0.1f; //How close together points or a point and a segment can be before we assume they're the same point.

        //First, check each pair of points for if they overlap
        for (int ai = 0; ai < a.getVertexCount(); ai++) {
            //The current point of A
            Vector2 aPoint = a.getVertex(ai, new Vector2());
//            //The next point after A
//            int aj = ai + 1;
//            if (aj >= a.getVertexCount()) aj = 0;
//            Vector2 aNextPoint = a.getVertex(aj, new Vector2());

            for (int bi = 0; bi < b.getVertexCount(); bi++) {
                System.out.println("Checking a: " + ai + ", b: " + bi + ", and their next points");
                //The current point of B
                Vector2 bPoint = b.getVertex(bi, new Vector2());

//                //The next point after B
//                int bj = bi + 1;
//                if (bj >= b.getVertexCount()) bj = 0;
//
//                Vector2 bNextPoint = b.getVertex(bj, new Vector2());

                //Check if a and b overlap with each other
                {
                    if (aPoint.epsilonEquals(bPoint, tolerance)) { //If a point on a is right on b
                        //The lists of a and b both still have original indices, so we can grab by index
                        originalAPoints.get(ai).setMatch(originalBPoints.get(bi));
                        originalBPoints.get(bi).setMatch(originalAPoints.get(ai));
                        System.out.println("A and B overlap, B: " + originalBPoints.get(bi));
                        continue;
                    }
//
//                    if (aNextPoint.epsilonEquals(bNextPoint, tolerance)) { //If the next points overlap
//                        //The lists of a and b both still have original indices, so we can grab by index
//                        originalAPoints.get(aj).setMatch(originalBPoints.get(bj));
//                        originalBPoints.get(bj).setMatch(originalAPoints.get(aj));
//                        System.out.println("A and B overlap, B: " + originalBPoints.get(bi));
//                        continue;
//                    }
//
//                    if (aNextPoint.epsilonEquals(bPoint, tolerance)) { //Next A, and current B
//                        //The lists of a and b both still have original indices, so we can grab by index
//                        originalAPoints.get(aj).setMatch(originalBPoints.get(bi));
//                        originalBPoints.get(bi).setMatch(originalAPoints.get(aj));
//                        System.out.println("A and B overlap, B: " + originalBPoints.get(bi));
//                        continue;
//                    }
//
//                    if (aPoint.epsilonEquals(bNextPoint, tolerance)) { //Current A, and next B
//                        //The lists of a and b both still have original indices, so we can grab by index
//                        originalAPoints.get(ai).setMatch(originalBPoints.get(bj));
//                        originalBPoints.get(bj).setMatch(originalAPoints.get(ai));
//                        System.out.println("A and B overlap, B: " + originalBPoints.get(bi));
//                        continue;
//                    }
                }
            }
        }
        //Second, once having checked for points that overlapped as is, check for points that overlap with segments
        for (int ai = 0; ai < a.getVertexCount(); ai++) {
            //The current point of A
            Vector2 aPoint = a.getVertex(ai, new Vector2());

            //The next point after A
            int aj = ai + 1;
            if (aj >= a.getVertexCount()) aj = 0;
            Vector2 aNextPoint = a.getVertex(aj, new Vector2());

            for (int bi = 0; bi < b.getVertexCount(); bi++) {
                System.out.println("Checking a: " + ai + ", b: " + bi + ", and their next points");
                //The current point of B
                Vector2 bPoint = b.getVertex(bi, new Vector2());

                //The next point after B
                int bj = bi + 1;
                if (bj >= b.getVertexCount()) bj = 0;

                Vector2 bNextPoint = b.getVertex(bj, new Vector2());
                {//Then, if they don't, check if a overlaps the segment following b
                    if (Intersector.distanceSegmentPoint(bPoint, bNextPoint, aPoint) < tolerance) {

                        //Create a new point for B
                        Vector2 bSeg = new Vector2(bNextPoint).sub(bPoint);
                        Vector2 bToA = new Vector2(aPoint).sub(bPoint);
                        float bSegLen = bSeg.len();
                        float t = bSeg.dot(bToA) / (bSegLen * bSegLen); // How far from b to bNext this intersection is
                        //If the t is 0 or 1, it's a point on point and should have already been found
                        if (!(t == 0 || t == 1)) {

                            UnionPoint bIntersect = new UnionPoint(aPoint, bi + t); // UnionPoint to go on polygon B

                            // Find the corresponding point on A, and match the two together
                            UnionPoint correspondingA = UnionPoint.getPointAtPosition(allAPoints, aPoint);
                            bIntersect.setMatch(correspondingA);
                            correspondingA.setMatch(bIntersect);

                            // Add new B point to B's expansion
                            bIntersect.addToPolygon(allBPoints);
                            System.out.println("A on segment B, B: " + bIntersect);
                            continue;
                        }
                    }

                    //Then, if they don't, check if b overlaps the segment following a
                    if (Intersector.distanceSegmentPoint(aPoint, aNextPoint, bPoint) < tolerance) {

                        //Create a new point for B
                        Vector2 aSeg = new Vector2(aNextPoint).sub(aPoint);
                        Vector2 aToB = new Vector2(bPoint).sub(aPoint);
                        float aSegLen = aSeg.len();
                        float t = aSeg.dot(aToB) / (aSegLen * aSegLen); // How far from b to bNext this intersection is
                        //If the t is 0 or 1, it's a point on point and should have already been found
                        if (!(t == 0 || t == 1)) {

                            UnionPoint aIntersect = new UnionPoint(bPoint, ai + t); // UnionPoint to go on polygon B

                            // Find the corresponding point on B, and match the two together
                            UnionPoint correspondingB = UnionPoint.getPointAtPosition(allBPoints, bPoint);
                            aIntersect.setMatch(correspondingB);
                            correspondingB.setMatch(aIntersect);

                            // Add new A point to A's expansion
                            aIntersect.addToPolygon(allAPoints);
                            System.out.println("B on segment A, B added: " + correspondingB);

                            continue;
                        }
                    }
                    /*
                    //Then, if they don't, check if the next b overlaps the segment following a
                    if (Intersector.distanceSegmentPoint(aPoint, aNextPoint, bNextPoint) < tolerance) {

                        //Represent the segment following A, and where BNext is on that segment
                        Vector2 aSeg = new Vector2(aNextPoint).sub(aPoint);
                        Vector2 aToB = new Vector2(bNextPoint).sub(aPoint);
                        float aSegLen = aSeg.len();
                        float t = aSeg.dot(aToB) / (aSegLen * aSegLen); // How far from a to aNext this intersection is

                        UnionPoint aIntersect = new UnionPoint(bNextPoint, ai + t); // UnionPoint to go on polygon A

                        // Find the corresponding point on B, and match the two together
                        UnionPoint correspondingB = UnionPoint.getPointAtPosition(allBPoints, bNextPoint);
                        aIntersect.setMatch(correspondingB);
                        correspondingB.setMatch(aIntersect);

                        // Add new A point to A's expansion
                        aIntersect.addToPolygon(allBPoints);
                        System.out.println("B on segment A, B referenced: " + correspondingB);

                        continue;
                    }

                    //Then, if they don't, check if the next a overlaps the segment following b
                    if (Intersector.distanceSegmentPoint(bPoint, bNextPoint, aNextPoint) < tolerance) {

                        //Represent the segment following A, and where BNext is on that segment
                        Vector2 bSeg = new Vector2(bNextPoint).sub(bPoint);
                        Vector2 bToA = new Vector2(aNextPoint).sub(bPoint);
                        float aSegLen = bSeg.len();
                        float t = bSeg.dot(bToA) / (aSegLen * aSegLen); // How far from a to aNext this intersection is

                        UnionPoint bIntersect = new UnionPoint(aNextPoint, bi + t); // UnionPoint to go on polygon B

                        // Find the corresponding point on B, and match the two together
                        UnionPoint correspondingB = UnionPoint.getPointAtPosition(allBPoints, bNextPoint);
                        aIntersect.setMatch(correspondingB);
                        correspondingB.setMatch(aIntersect);

                        // Add new A point to A's expansion
                        aIntersect.addToPolygon(allBPoints);
                        System.out.println("B on segment A, B referenced: " + correspondingB);

                        continue;
                    }*/
                }
            }
        }
        //Third, check for if any segments overlap
        for (int ai = 0; ai < a.getVertexCount(); ai++) {
            //The current point of B
            Vector2 aPoint = a.getVertex(ai, new Vector2());
            //The next point after A
            int aj = ai+1;
            if (aj >= a.getVertexCount()) aj = 0;
            Vector2 aNextPoint = a.getVertex(aj, new Vector2());

            for (int bi = 0; bi < b.getVertexCount(); bi++) {
                System.out.println("Checking a: " + ai + ", b: " + bi + " for segment overlaps");
                //The current point of B
                Vector2 bPoint = b.getVertex(bi, new Vector2());

                //The next point after B
                int bj = bi+1;
                if (bj >= b.getVertexCount()) bj = 0;

                Vector2 bNextPoint = b.getVertex(bj, new Vector2());

                //Check if the segments following a and b intersect with eachother
                Vector2 intersectionABSegments = new Vector2();
                if (Intersector.intersectSegments(aPoint, aNextPoint, bPoint, bNextPoint, intersectionABSegments)) {

                    // Identify where the intersection lies each on a's segment and on b's segment
                    Vector2 iFromA = new Vector2(intersectionABSegments).sub(aPoint);
                    Vector2 iFromB = new Vector2(intersectionABSegments).sub(bPoint);
                    Vector2 aSeg = new Vector2(aNextPoint).sub(aPoint);
                    Vector2 bSeg = new Vector2(bNextPoint).sub(bPoint);
                    float aT = iFromA.len() / aSeg.len();
                    float bT = iFromB.len() / bSeg.len();
                    // If either of the T's are 1 or 0, this means the intersection will have already been found by other means
                    if (aT == 0 || aT == 1 || bT == 0 || bT == 1) continue;
                    // Establish points for polygons a and b
                    UnionPoint aUP = new UnionPoint(intersectionABSegments, ai+aT);
                    UnionPoint bUP = new UnionPoint(intersectionABSegments, bi+bT);
                    aUP.setMatch(bUP);
                    bUP.setMatch(aUP);
                    // Add to respective polygons
                    aUP.addToPolygon(allAPoints);
                    bUP.addToPolygon(allBPoints);
                    System.out.println("Segments A and B overlap, B added: " + bUP);
                }
            }
        }

        System.out.println("B coordinates including intersections: " + allBPoints);

        // Find a point on one that isn't contained on the other, and start from there
        UnionPoint startPoint = null;
        float[] baseAVerts = a.getTransformedVertices(), baseBVerts = b.getTransformedVertices();
        for (UnionPoint aUP : allAPoints) { //For each point in A,
            if ((!aUP.isIntersection) &&  //If the point isn't an intersection,
                    (!Intersector.isPointInPolygon(baseBVerts, 0, baseBVerts.length, aUP.pos.x, aUP.pos.y))) { //And isn't inside of B,
                //If there hasn't been found a start point yet, or if this new point is farther left than the existing start point,
                if (startPoint == null || aUP.pos.x < startPoint.pos.x) {
                    //We can start from there
                    startPoint = aUP;
                }
            }
        }
        if (startPoint == null) { //If all the points on A are intersections or contained by B, check b's points
            for (UnionPoint bUP : allBPoints) { //For each point in A,
                if ((!bUP.isIntersection) &&  //If the point isn't an intersection,
                        (!Intersector.isPointInPolygon(baseAVerts, 0, baseAVerts.length, bUP.pos.x, bUP.pos.y))) { //And isn't inside of B,
                    //If there hasn't been found a start point yet, or if this new point is farther left than the existing start point,
                    if (startPoint == null || bUP.pos.x < startPoint.pos.x) {
                        //We can start from there
                        startPoint = bUP;
                    }
                }
            }
        }

        //Starting from the initial point, wind around both polygons to build the union polygon
        FloatArray finalVerts = new FloatArray(baseAVerts.length+baseBVerts.length); //The vertices of the final polygon
        UnionPoint currentPoint = startPoint;// Record the current point
        int prevIndex = startPoint.parent.indexOf(currentPoint)-1;
        if (prevIndex < 0) prevIndex = startPoint.parent.size()-1;
        UnionPoint lastPoint = startPoint.parent.get(prevIndex);// Record the last point, starting with the previous point to the starting point. Since it's not an intersection, going back 1 on the same polygon should work
        boolean loopedBackToFirst = false;
        System.out.println("First point index: " + startPoint.index + ", on polygon " + startPoint.parent);
        int numIterations = 0;
        int maxIterations = 100;
        while (!loopedBackToFirst) {//TODO: when adding an edge polygon, this loop gets stuck
            numIterations++;
            System.out.println("Num iterations: " + numIterations);
            if (numIterations >= maxIterations) {
                loopedBackToFirst = true;
                Gdx.app.exit();
            }
            //Identify what the next point would be
            UnionPoint nextPoint;
            //Identify the next point on this polygon, but don't use that quite yet:
            ArrayList<UnionPoint> currentPolygon = currentPoint.parent;
            int nextIdxCurrentPolygon = currentPolygon.indexOf(currentPoint) + 1;
            if (nextIdxCurrentPolygon >= currentPolygon.size()) nextIdxCurrentPolygon = 0;
            UnionPoint nextOnCurrent = currentPolygon.get(nextIdxCurrentPolygon);
            //Before using it, check if we should switch to another point instead
            if (currentPoint.isIntersection) {//If the current point is an intersection, determine which of the next points after it is outermost
                //Next point on the other polygon
                UnionPoint altPoint = currentPoint.match;//Find the matching point on the other polygon
                ArrayList<UnionPoint> altPolygon = altPoint.parent;
                int nextIdxAltPolygon = altPolygon.indexOf(altPoint) + 1;
                if (nextIdxAltPolygon >= altPolygon.size()) nextIdxAltPolygon = 0;
                UnionPoint nextOnAlt = altPolygon.get(nextIdxAltPolygon);

                //Compare the two to find the outermost one
                Vector2 toPrevious = new Vector2(lastPoint.pos).sub(currentPoint.pos).nor(); // Direction coming from the previous one
                Vector2 toCurrentNext = new Vector2(nextOnCurrent.pos).sub(currentPoint.pos).nor();
                float outernessCurrentNext = (toPrevious.dot(toCurrentNext)*0.5f + 0.5f) * Math.signum(toCurrentNext.crs(toPrevious)); //Scale the dot product to get a range from 0 to 1 where 0 means the vector keeps going in the same direction, and 1 means it goes back the opposite. Combine with cross product to get range of -1 turning all the way around right and 1 turning all the way around left
                Vector2 toAltNext = new Vector2(nextOnAlt.pos).sub(currentPoint.pos).nor();
                float outernessAltNext = (toPrevious.dot(toAltNext)*0.5f + 0.5f) * Math.signum(toAltNext.crs(toPrevious));

                //If the next point on the current one is more "out" or "turns left more" than the alt, keep on the same polygon
                if (outernessCurrentNext > outernessAltNext) {
                    nextPoint = nextOnCurrent;
                } else { //And if not, switch to the alternate polygon
                    nextPoint = nextOnAlt;
                }
            } else { //If the current point isn't an intersection, just skip straight to the next point on the same polygon
                nextPoint = nextOnCurrent;
            }

            //Add the current point
            finalVerts.add(currentPoint.pos.x, currentPoint.pos.y);
            System.out.println("New point index: " + currentPoint.index + ", on polygon: " + currentPolygon);
            //If the next point would be back to the first, call it there. If not, switch to the next point and repeat
            if (nextPoint == startPoint) {
                loopedBackToFirst = true;
            } else {
                lastPoint = currentPoint;
                currentPoint = nextPoint;
            }
        }
        // Check for any redundant points
        finalVerts.shrink();
        FloatArray prunedFinalVerts = new FloatArray();
        int numVerts = finalVerts.size/2;
        for (int i = 0; i < numVerts; i++) { //For each point, check if it's not on the line between the points before and after it, and if so add it
            //Point before
            int h = i-1;
            if (h < 0) h = numVerts-1;
            float hx = finalVerts.get(h*2), hy = finalVerts.get(h*2+1);
            //Point after
            int k = i+1;
            if (k >= numVerts) k = 0;
            float kx = finalVerts.get(k*2), ky = finalVerts.get(k*2+1);
            //The point itself
            float ix = finalVerts.get(i*2), iy = finalVerts.get(i*2+1);

            //If the point isn't outright on that segment
            float maxDistToSegment = tolerance/10f;
            if (Intersector.distanceSegmentPoint(hx, hy, kx, ky, ix, iy) >= maxDistToSegment) {
                prunedFinalVerts.add(ix, iy);
            }
        }

        // After building the vertices, save them to a new polygon and return that
        return new Polygon(prunedFinalVerts.shrink());
    }

    /***/
    public static Polygon addPolygonBtoA(Polygon a, Polygon b) {
        return null;
    }

    public static Vector2 projectAontoB(Vector2 a, Vector2 b) {
        Vector2 bNorm = new Vector2(b).nor();
        return bNorm.scl(a.dot(bNorm));
    }

    /**Returns the parameter T such that value = start + T*(end-start)*/
    public static float parameterize(float start, float end, float value) {
        return parameterizeWithDistance(start, start-end, value);
    }

    public static float parameterizeWithDistance(float start, float distance, float value) {
        return (value-start)/distance;
    }

    public static float tOnLinearPath(Vector2 start, Vector2 end, Vector2 position) {
        return tOnLinearPath(start, end, position, false);
    }
    public static float tOnLinearPath(Vector2 start, Vector2 end, Vector2 position, boolean snapToPath) {
        //Convert to coordinates relative to the start of the path
        Vector2 posRel = new Vector2(position).sub(start);
        Vector2 endRel = new Vector2(end).sub(start);
        //If A can't be assumed to be already on the exact path from start to end, find the projection onto that path
        if (snapToPath) posRel = projectAontoB(posRel, endRel);

        //Divide the square lengths to take square root once instead of twice
        return (float) Math.sqrt(posRel.len2() / endRel.len2());
    }

    /**Returns a point at the given T, linearly interpolated, between the given start and end points.*/
    public static Vector2 interpolateLinearPath(Vector2 start, Vector2 end, float T) {
        return new Vector2(end).sub(start).scl(T).add(start);
    }

    /**Concatenates LibGDX {@link Vector2} instances into a column {@link org.ejml.simple.SimpleMatrix}, optionally with a 1 added at the end.*/
    public static SimpleMatrix concatVector2sColumn(Vector2[] vectors, boolean add1Entry) {
        int columnLength = vectors.length*2 + (add1Entry ? 1 : 0);

        SimpleMatrix columnVec = SimpleMatrix.filled(columnLength, 1, 0);
        for (int i = 0; i < vectors.length; i++) {
            Vector2 vec = vectors[i];
            columnVec.set(i*2, 0, vec.x);
            columnVec.set(i*2+1, 0, vec.y);
        }
        if (add1Entry) columnVec.set(columnLength-1, 0, 1);

        return columnVec;
    }

    public static SimpleMatrix concatVector2sColumn(Vector2[] vectors) {
        return concatVector2sColumn(vectors, false);
    }

    public static void expandClockwiseConvexPolygon(Polygon target, float dist) {
        float[] oldVerts = target.getTransformedVertices();
        float[] newVerts = new float[oldVerts.length];
        System.arraycopy(oldVerts, 0, newVerts, 0, oldVerts.length);

        for (int jx = 0; jx < oldVerts.length; jx += 2) {
            int jy = jx+1;
            int ix = jx-2; if (ix < 0) ix = oldVerts.length-2;
            int iy = ix+1;
            int kx = jx+2; if (kx >= oldVerts.length) kx = 0;
            int ky = kx+1;

            Vector2 x;
        }
    }

    /**
     * @param xRad The radius along x coordinates of the ellipse.
     * @param yRad The radius along y coordinates of the ellipse.
     * @param intersections An array in which to store resulting intersections, if any are found. Returned as is if none are found,
     *                      a Vector2 at idx 0 if one is found, and a Vector2 at both indices if two are found.
     *
     * @return The number of intersection points of the circle with the given
     * Demonstration: https://www.desmos.com/calculator/fcdbphe3sy
     * */
    public static int segmentIntersectsEllipse(
            float ax, float ay,
            float bx, float by,
            float ex, float ey,
            float xRad, float yRad,
            @Null Vector2[] intersections
    ) {
        // Should not be called on a segment with no dx or dy
        assert ax != bx || ay != by;
        // If a return array is given, should be exactly 2 long (This could be changed if a use case presents itself)
        assert intersections == null || intersections.length == 2;

        float m = xRad/yRad;
        float m2 = m*m;

        // Shift coordinates so ellipse is centered at origin
        ax -= ex;
        ay -= ey;
        bx -= ex;
        by -= ey;

        float ama = ax*ax + m2*ay*ay;
        float abmab = 2*(ax*bx + m2*ay*by);
        float bmb = bx*bx + m2*by*by;

        float a = ama - abmab + bmb;
        float b = -2*ama + abmab;
        float c = ama - xRad*xRad;

        float discrim = b*b - 4*a*c;

        // No intersections
        if (discrim < 0) {
            return 0;
        }

        // Exactly 1 intersection on the line, test if its within the segment
        if (discrim == 0) {
            float t = -0.5f * b / a;
            if (t >= 0 && t <= 1) {
                if (intersections != null) intersections[0] = new Vector2((1-t)*ax + t*bx, (1-t)*ay + t*by).add(ex, ey);
                return 1;
            } else {
                return 0;
            }
        }

        // 2 intersections, test each to see if it's within the segment
        int numIntersects = 0;

        float sqrtDiscrim = (float) Math.sqrt(discrim);
        float tPlus = (0.5f/a) * (-b-sqrtDiscrim);
        if (tPlus >= 0 && tPlus <= 1) {
            if (intersections != null) intersections[numIntersects] = new Vector2((1-tPlus)*ax + tPlus*bx, (1-tPlus)*ay + tPlus*by).add(ex, ey);
            numIntersects++;
        }

        float tMinus = (0.5f/a) * (-b+sqrtDiscrim);
        if (tMinus >= 0 && tMinus <= 1) {
            if (intersections != null) intersections[numIntersects] = new Vector2((1-tMinus)*ax + tMinus*bx, (1-tMinus)*ay + tMinus*by).add(ex, ey);
            numIntersects++;
        }

        return numIntersects;
    }

    /**Compares two sets a and b, and returns true if a has elements equal to b's elements. I.e., the sets are equal. Defined to allow string comparisons.*/
    public static <T> boolean setsHaveEqualElements(Set<T> a, Set<T> b) {
        if (a.size() != b.size()) return false;

        for (T ai : a) {
            boolean aiInB = false;
            for (T bi : b) {
                if (ai.equals(bi)) {
                    aiInB = true;
                    break;
                }
            }
            if (!aiInB) return false;
        }

        return true;
    }
}

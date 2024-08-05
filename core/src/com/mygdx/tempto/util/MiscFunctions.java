package com.mygdx.tempto.util;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;

import java.util.ArrayList;
import java.util.Vector;

/**A class with miscellaneous utility functions*/
public class MiscFunctions {
    /**Returns either the value, or, if the value does not fit in the given boundaries, the boundary closest to the value.
     * Assumption: max > min*/
    public static int clamp(int value, int min, int max) {
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

    /**Returns the union, not including holes, of the two given clockwise wound polygons.
     * The return is a new object, and the two existing polygons are not modified.
     */
    public static Polygon polygonUnion(Polygon a, Polygon b) {
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
        }



        // Create ArrayLists to represent the vertices of A and B, including intersections with the other polyons
        ArrayList<UnionPoint> allAPoints = new ArrayList<>();
        ArrayList<UnionPoint> allBPoints = new ArrayList<>();

        // Populate them with the original points
        for (int i = 0; i < a.getVertexCount(); i++) allAPoints.add(new UnionPoint(a.getVertex(i, new Vector2()), i).setParent(allAPoints));
        for (int i = 0; i < b.getVertexCount(); i++) allBPoints.add(new UnionPoint(b.getVertex(i, new Vector2()), i).setParent(allBPoints));

        // Populate them with any intersections between the polygons
        float tolerance = 0.1f; //How close together points or a point and a segment can be before we assume they're the same point.
        for (int ai = 0; ai < a.getVertexCount(); ai++) {
            Vector2 aPoint = a.getVertex(ai, new Vector2());
            for (int bi = 0; bi < b.getVertexCount(); bi++) {
                Vector2 bPoint = b.getVertex(bi, new Vector2());

                //First, check if these points on a and b overlap
                if (aPoint.epsilonEquals(bPoint, tolerance)) { //If a point on a is right on a point on b
                    //The lists of a and b both still have original indices, so we can grab by index
                    allAPoints.get(ai).setMatch(allBPoints.get(bi));
                    allBPoints.get(bi).setMatch(allAPoints.get(ai));
                    continue;
                }

                //Then, if they don't, check if a overlaps the segment following b
                int bj = bi + 1;
                if (bj >= b.getVertexCount()) bj = 0;
                Vector2 bNextPoint = b.getVertex(bj, new Vector2());
                if (Intersector.distanceSegmentPoint(bPoint, bNextPoint, aPoint) < tolerance) {
                    //Create a new point for B
                    Vector2 bSeg = new Vector2(bNextPoint).sub(bPoint);
                    Vector2 bToA = new Vector2(aPoint).sub(bPoint);
                    float bSegLen = bSeg.len();
                    float t = bSeg.dot(bToA) / (bSegLen*bSegLen); // How far from b to bNext this intersection is

                    UnionPoint bIntersect = new UnionPoint(aPoint, bi+t); // UnionPoint to go on polygon B

                    // Find the corresponding point on A, and match the two together
                    UnionPoint correspondingA = UnionPoint.getPointAtPosition(allAPoints, aPoint);
                    bIntersect.setMatch(correspondingA);
                    correspondingA.setMatch(bIntersect);

                    // Add new B point to B's expansion
                    bIntersect.addToPolygon(allBPoints);
                    continue;
                }

                //Then, if they don't, check if b overlaps the segment following a
                int aj = ai + 1;
                if (aj >= a.getVertexCount()) aj = 0;
                Vector2 aNextPoint = a.getVertex(aj, new Vector2());
                if (Intersector.distanceSegmentPoint(aPoint, aNextPoint, bPoint) < tolerance) {
                    //Create a new point for B
                    Vector2 aSeg = new Vector2(aNextPoint).sub(aPoint);
                    Vector2 aToB = new Vector2(bPoint).sub(aPoint);
                    float aSegLen = aSeg.len();
                    float t = aSeg.dot(aToB) / (aSegLen*aSegLen); // How far from b to bNext this intersection is

                    UnionPoint aIntersect = new UnionPoint(bPoint, ai+t); // UnionPoint to go on polygon B

                    // Find the corresponding point on B, and match the two together
                    UnionPoint correspondingB = UnionPoint.getPointAtPosition(allBPoints, bPoint);
                    aIntersect.setMatch(correspondingB);
                    correspondingB.setMatch(aIntersect);

                    // Add new A point to A's expansion
                    aIntersect.addToPolygon(allBPoints);
                    continue;
                }

                //Then finally, if they don't, check if the segments following a and b intersect with eachother
                Vector2 intersectionABSegments = new Vector2();
                if (Intersector.intersectSegments(aPoint, aNextPoint, bPoint, bNextPoint, intersectionABSegments)) {
                    // Identify where the intersection lies each on a's segment and on b's segment
                    Vector2 iFromA = new Vector2(intersectionABSegments).sub(aPoint);
                    Vector2 iFromB = new Vector2(intersectionABSegments).sub(bPoint);
                    Vector2 aSeg = new Vector2(aNextPoint).sub(aPoint);
                    Vector2 bSeg = new Vector2(bNextPoint).sub(bPoint);
                    float aT = iFromA.len() / aSeg.len();
                    float bT = iFromB.len() / bSeg.len();
                    // Establish points for polygons a and b
                    UnionPoint aUP = new UnionPoint(intersectionABSegments, ai+aT);
                    UnionPoint bUP = new UnionPoint(intersectionABSegments, bi+bT);
                    aUP.setMatch(bUP);
                    bUP.setMatch(aUP);
                    // Add to respective polygons
                    aUP.addToPolygon(allAPoints);
                    bUP.addToPolygon(allBPoints);
                }
            }
        }

        // Find a point on one that isn't contained on the other, and start from there
        UnionPoint startPoint = null;
        float[] baseAVerts = a.getTransformedVertices(), baseBVerts = b.getTransformedVertices();
        for (UnionPoint aUP : allAPoints) { //For each point in A,
            if ((!aUP.isIntersection) &&  //If the point isn't an intersection,
                    (!Intersector.isPointInPolygon(baseBVerts, 0, baseBVerts.length, aUP.pos.x, aUP.pos.y))) { //And isn't inside of B,
                //We can start from there
                startPoint = aUP;
                break;
            }
        }
        if (startPoint == null) { //If all the points on A are intersections or contained by B, check b's points
            for (UnionPoint bUP : allBPoints) { //For each point in A,
                if ((!bUP.isIntersection) &&  //If the point isn't an intersection,
                        (!Intersector.isPointInPolygon(baseAVerts, 0, baseAVerts.length, bUP.pos.x, bUP.pos.y))) { //And isn't inside of B,
                    //We can start from there
                    startPoint = bUP;
                    break;
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
        while (!loopedBackToFirst) {
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
            //If the next point would be back to the first, call it there. If not, switch to the next point and repeat
            if (nextPoint == startPoint) {
                loopedBackToFirst = true;
            } else {
                lastPoint = currentPoint;
                currentPoint = nextPoint;
            }
        }

        // After building the vertices, save them to a new polygon and return that
        return new Polygon(finalVerts.shrink());
    }

    /***/
    public static Polygon addPolygonBtoA(Polygon a, Polygon b) {
        return null;
    }
}

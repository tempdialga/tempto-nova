package com.mygdx.tempto.entity;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Null;
import com.mygdx.tempto.data.CentralTextureData;
import com.mygdx.tempto.data.SavesToFile;
import com.mygdx.tempto.entity.decoration.TileLayer;
import com.mygdx.tempto.entity.physics.Collidable;
import com.mygdx.tempto.entity.physics.PointProcedure;
import com.mygdx.tempto.entity.physics.SegmentProcedure;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.rendering.AltDepthBatch;
import com.mygdx.tempto.rendering.RendersToDebug;
import com.mygdx.tempto.rendering.RendersToWorld;
import com.mygdx.tempto.rendering.ShadowCaster;
import com.mygdx.tempto.rendering.TileLayerDepthRenderer;
import com.mygdx.tempto.util.MiscFunctions;
import com.mygdx.tempto.view.GameScreen;

import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.lwjgl.Sys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import space.earlygrey.shapedrawer.ShapeDrawer;

public class StaticTerrainElement implements Entity, SavesToFile, Collidable, RendersToWorld, RendersToDebug {

    public float depth; //z coordinate in pixels, 0 at the camera and increasing into the screen
    public Polygon polygon;//TODO: generalize, this is only a temporary thing to make sure map loading works
    public Color color;
    public Color baseDepthColor;
    public String id;
    public InputAdapter debugInput; //TODO: remove once we add other things that actually are supposed to take user input
    private boolean edited; //Whether or not this terrain object has been edited and needs to be saved to the base file
    private boolean deleted; //If, as part of being edited, this object has been deleted
    private WorldMap parent;

    //Rendering
    /**A series of 6-length float arrays representing this element's polygon split into triangles*/
    public float[] triangles;

    public StaticTerrainElement(PolygonMapObject mapObject) {
        this(mapObject, null);
    }

    public StaticTerrainElement(PolygonMapObject mapObject, JsonValue persistentMapData) {
        this(mapObject.getPolygon(), mapObject.getProperties(), persistentMapData);
    }

    private StaticTerrainElement(Polygon shape, MapProperties properties, JsonValue persistentMapData) {
        this.polygon = shape;
        this.id = properties.get("id", String.class);//ID of the object as denoted in base map file; dictates how

        if (persistentMapData != null && persistentMapData.has(id)) {
            JsonValue thisData = persistentMapData.get(id); //Data in the value corresponding to this map object
            Color savedColor = Color.valueOf(thisData.getString("color"));//If it has the id, it *should* have been saved with this
            this.setColor(savedColor);//Set the color as it was saved in the map
        } else {
            this.setColor(Color.BLACK);
        }

        // Add a testing input adapter that, when the player confirms, will change color.
        this.debugInput = new InputAdapter(){
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == InputTranslator.GameInputs.CONFIRM) {
                    float r, g, b;
                    r = (float) Math.random();
                    g = (float) Math.random();
                    b = (float) Math.random();
                    StaticTerrainElement.this.setColor(new Color(r, g, b, 1));
                }
                return false;
            }
        };

        // Formally set polygon vertices
        this.setPolygonVertices(shape.getTransformedVertices());

        // The depth being -8 is a magic number, and TODO: should be figured out
        this.depth = -8;
        this.baseDepthColor = TileLayerDepthRenderer.unpackedDepthColor(this.depth, new Vector3(0,0,-1), 0f);
    }

    /**Sets the polygon's transformed world vertices to the given array, and sets its position to (0,0).
     * */
    public void setPolygonVertices(float[] newWorldVertices) {
        float[] vertsCopy = new float[newWorldVertices.length];
        System.arraycopy(newWorldVertices, 0, vertsCopy, 0, newWorldVertices.length);

        this.polygon.setPosition(0,0);
        this.polygon.setVertices(vertsCopy);

        this.updateTriangles();

        System.out.println("Polygon vertices set to: " + Arrays.toString(this.polygon.getTransformedVertices()));
    }


    public void setColor(Color newColor) {
        this.color = newColor;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public void update(float deltaTime, WorldMap world) {
    }

    @Override
    public void setParentWorld(WorldMap parent) {
        parent.addWorldInput(this.debugInput);
        this.parent = parent;
    }

    @Override
    public String getID() {
        return this.id;
    }


    //@Override
    public void updateBaseFile(TiledMap baseMap, @Null MapLayer specifiedEntityLayer) {
        MapLayer toPlaceIn = specifiedEntityLayer; //The map layer to place entity data in
        if (specifiedEntityLayer == null) {
            boolean foundALayer = false;
            for (MapLayer layer : baseMap.getLayers()) {
                if (!(layer instanceof TiledMapImageLayer || layer instanceof TiledMapTileLayer)) {
                    toPlaceIn = layer;
                    break; // Don't need to look any further
                }
            }
        }
        if (toPlaceIn == null) { //If no such layer has been found, this isn't workable
            System.out.println("No Entity layer found!");
            throw new NullPointerException("No Entity layer given!");
        }

        //Search for existing version of this?

        //Create a new map data object
        PolygonMapObject baseMapObject = new PolygonMapObject(this.polygon);
        //baseMapObject.setPolygon(this.polygon);
        //Match its properties
        MapProperties props = baseMapObject.getProperties();
        props.put("id", this.id);
        props.put("x", this.polygon.getX());
        props.put("y", this.polygon.getY());
        //Save into the base map data
        toPlaceIn.getObjects().add(baseMapObject);
        
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    @Override
    public void writeSerializedValue(JsonValue host) {
        JsonValue thisValue = new JsonValue(JsonValue.ValueType.object);
        thisValue.addChild("color", new JsonValue(this.color.toString()));
        host.addChild(this.id, thisValue);
        System.out.println(host);
    }


    @Override
    public void forEachSegment(SegmentProcedure procedure) {
        Polygon pol = this.getPolygon();
        Vector2 A = new Vector2(), B = new Vector2();
        for (int i = 0; i < pol.getVertexCount(); i++) {
            int j = i + 1;
            if (j >= pol.getVertexCount()) j = 0;

            A = pol.getVertex(i, A);
            B = pol.getVertex(j, B);

            procedure.actOnSegment(
                    A.x, A.y, //Coords of A
                    B.x, B.y, //Coords of B
                    0,0,0,0, //Polygon terrain static (for now)
                    i, j, //Indices of A and B
                    SegmentProcedure.CC_NORMAL //Polygon terrain is clockwise wound, so the normal vector to the vector A->B is rotated 90⁰ counterclockwise to A->B
            );

        }
    }

    @Override
    public void forEachPoint(PointProcedure procedure) {
        Polygon pol = this.getPolygon();
        Vector2 A = new Vector2();
        for (int i = 0; i < pol.getVertexCount(); i++) {

            A = pol.getVertex(i, A);

            procedure.forEach(
                    i, //Index of A
                    A.x, A.y, //Coords of A
                    0,0 //Polygon terrain static (for now)
            );

        }
    }

    @Override
    public Vector2 getPosAtIndex(float index, boolean exactPoint) {
        int exactIdx = (int) (index + 0.5f);
        Vector2 A = this.getPolygon().getVertex(exactIdx, new Vector2());
        if (exactPoint) return A;

        int nextIdx = exactIdx + 1;
        if (nextIdx >= this.getPolygon().getVertexCount()) nextIdx = 0;
        Vector2 B = this.getPolygon().getVertex(nextIdx, new Vector2());

        float T = index - (float) exactIdx;

        return MiscFunctions.interpolateLinearPath(A, B, T);
    }

    @Override
    public Vector2 getVelAtIndex(float index, boolean exactPoint) {
        return new Vector2(0,0);
    }

    @Override
    public Rectangle getBoundingRectangle() {
        return this.getPolygon().getBoundingRectangle();
    }

    public void updateTriangles() {
        float[] verts = this.polygon.getTransformedVertices();
        int numTris = verts.length / 2 - 2; //2 floats per vertex, 2 less than total vertices
        this.triangles = new float[6*numTris];
        int nextTriIdx = 0;

        float[] vertCopy = new float[verts.length];
        System.arraycopy(verts, 0, vertCopy, 0, vertCopy.length);
        FloatArrayList dynPoints = new FloatArrayList(vertCopy);//dynamic list of the points
        while (dynPoints.size() > 6) {//While there is more than one triangle remaining
            //System.out.println("coordinates remaining: " + dynPoints.size());
            int numCoordsBefore = dynPoints.size();
            for (int i = 0; i < dynPoints.size(); i+=2) {
                //Iterate through each coordinate pair, then if a triangle of it and the next two can be readily cut off, stop there, remove it, and rinse and repeat

                int j = i + 2;
                if (j >= dynPoints.size()) {j = 0;}//If the points are at the end of the list, wrap back around to the start
                int k = j + 2;
                if (k >= dynPoints.size()) {k = 0;}

                float ax, ay, bx, by, cx, cy;
                ax = dynPoints.get(i);
                ay = dynPoints.get(i+1);
                bx = dynPoints.get(j);
                by = dynPoints.get(j+1);
                cx = dynPoints.get(k);
                cy = dynPoints.get(k+1);

                Vector2 aToB = new Vector2(bx, by).sub(ax, ay);
                Vector2 bToC = new Vector2(cx, cy).sub(bx, by);
                if (aToB.rotate90(1).dot(bToC) >=0) {//If the angle of abc is convex, skip it. Checked by testing if a vector facing away from surface AB is in the same direction as the direct vector from b to c
                    continue;//Convex, so the triangle wouldn't make sense
                }

                //a-b and b-c are already edges, so if a-c doesn't intersect anything we should be clear to cut this triangle off
                //Start iterating through the rest of the points
                int iter = k;
                boolean triangleCanBeCut = true;

                for (int l = 0; l < dynPoints.size() - 6; l+=2) { //For each remaining point besides the three in question
                    iter += 2;
                    if (iter >= dynPoints.size()) {iter = 0;}

                    float otherX = dynPoints.get(iter);
                    float otherY = dynPoints.get(iter + 1);
                    if (Intersector.isPointInTriangle(otherX, otherY, ax, ay, bx, by, cx, cy)) {
                        triangleCanBeCut = false;
                        break;//Stop checking other segments, this triangle can't be cut
                    }
                }

                if (triangleCanBeCut) {//If no other segments intersected A & C, cut the triangle out
                    float[] newTriangle = new float[]{ax, ay, bx, by, cx, cy};//Create a float array representing the triangle to be cut
                    System.arraycopy(newTriangle, 0, this.triangles, nextTriIdx, 6);
                    nextTriIdx+=6;
//                    this.triangles.add(newTriangle);
                    dynPoints.removeAtIndex(j);//Remove bx
                    dynPoints.removeAtIndex(j);//Remove by
                    break;
                }
            }

            if (dynPoints.size() == 6) {//Just one triangle left
                System.arraycopy(dynPoints.toArray(), 0, this.triangles, nextTriIdx, 6);
                nextTriIdx+=6;
            }

            int numCoordsAfter = dynPoints.size();

            if (numCoordsBefore == numCoordsAfter) {//If no triangle was successfully removed, it will never be able to remove a triangle
                break;
            }
        }
    }


    @Override
    public void renderToWorld(Batch batch, OrthographicCamera worldCamera) {
//        ShapeDrawer drawer = this.parent.tempFinalPassShapeDrawer;
//        float[] tris = this.triangles;
//        for (int i = 0; i < tris.length-4; i+= 2) {
//            float x1 = tris[i], y1 = tris[i+1],
//                    x2 = tris[i+2], y2 = tris[i+3],
//                    x3 = tris[i+4], y3 = tris[i+5];
//            drawer.filledTriangle(x1, y1, x2, y2, x3, y3, this.color);
//        }
    }

    @Override
    public void renderToDepthMap(AltDepthBatch depthBatch, OrthographicCamera worldCamera) {
//        ShapeDrawer drawer = this.parent.tempDepthShapeDrawer;
//        float[] tris = this.triangles;
//        for (int i = 0; i < tris.length-4; i+= 2) {
//            float x1 = tris[i], y1 = tris[i+1],
//                    x2 = tris[i+2], y2 = tris[i+3],
//                    x3 = tris[i+4], y3 = tris[i+5];
//            drawer.filledTriangle(x1, y1, x2, y2, x3, y3, this.baseDepthColor);
//        }
    }

    @Override
    public void debugRender(ShapeRenderer drawer) {
        System.out.println("Debug rendering polygon");
        drawer.setColor(this.color);
        float[] tris = this.triangles;
        for (int i = 0; i < tris.length-4; i += 2) {
            float x1 = tris[i], y1 = tris[i+1],
                    x2 = tris[i+2], y2 = tris[i+3],
                    x3 = tris[i+4], y3 = tris[i+5];

            drawer.triangle(x1, y1, x2, y2, x3, y3);
        }
    }

    @Override
    public void addShadowCastersToList(List<ShadowCaster> centralList) {
        //Front face
        float[] tris = this.triangles;
        for (int i = 0; i < tris.length-4; i+= 2) {
            float x1 = tris[i], y1 = tris[i+1],
                    x2 = tris[i+2], y2 = tris[i+3],
                    x3 = tris[i+4], y3 = tris[i+5];

            ShadowCaster caster = new ShadowCaster(CentralTextureData.getRegion("maps/collisionTexture"), new Vector3(x1, y1, this.depth), new Vector3(x3-x1, y3-y1, 0), new Vector3(x2-x1,y2-y1,0), true, true);
            centralList.add(caster);
        }
        //Sides along edges extending back into the background
        int numSides = 0;
        float[] verts = this.polygon.getTransformedVertices();
        for (int i = 0; i < verts.length; i+=2) {
            int j = i+2;
            if (j >= verts.length) j = 0;
            float x1 = verts[i], y1 = verts[i+1],
                    x2 = verts[j], y2 = verts[j+1];
            ShadowCaster caster = new ShadowCaster(CentralTextureData.getRegion("maps/collisionTexture"), new Vector3(x1, y1, this.depth), new Vector3(x2-x1,y2-y1,0), new Vector3(0,0, 16), true, false);
            centralList.add(caster);
            numSides++;
        }
//        System.out.println("Added " + numSides + " casters on the sides");
    }
}

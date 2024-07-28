package com.mygdx.tempto.editing;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.tempto.entity.Entity;
import com.mygdx.tempto.entity.StaticTerrainElement;
import com.mygdx.tempto.gui.PauseMenu;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.util.MiscFunctions;

import java.lang.reflect.Array;
import java.net.IDN;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

/**A set of implementations of {@link MapEditingTool}, instantiated once per game, which offer basic functionality such as editing */
public enum Tools {

    TERRAIN_EDITOR(new MapEditingTool() {

        /**The piece of terrain currently being edited. Can be null, if none is currently selected. When a new */
        StaticTerrainElement currentlyEditing = null;
        /**The temporary set of points being used to represent the terrain while it's being edited (This way it only sets the polygon after a valid change occurs)*/
        float[] proposedVertices = null;
        /**The utility object to highlight the terrain, etc.*/
        ShapeDrawer shapeDrawer;

        //Terrain editor specific map edits
        public class PlaceTerrainPolygon extends MapEdit {

            StaticTerrainElement terrainElement;
            PolygonMapObject terrainInBaseFile;

            public PlaceTerrainPolygon(StaticTerrainElement terrainElement, PolygonMapObject terrainInBaseFile) {
                this.terrainElement = terrainElement;
                this.terrainInBaseFile = terrainInBaseFile;
            }

            @Override
            public void undoEdit(WorldMap map) {
                map.removeEntity(this.terrainElement);
                map.getEntityLayer().getObjects().remove(this.terrainInBaseFile);
            }

            @Override
            public void redoEdit(WorldMap map) {
                map.addEntity(this.terrainElement);
                map.getEntityLayer().getObjects().add(this.terrainInBaseFile);
            }
        }

        @Override
        public void touchDown(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam) {
            if (this.proposedVertices == null) return; // Only try to edit the terrain if one is currently selected
            System.out.println("Adding point");
            //Identify the world point they clicked on
            Vector3 worldCoords = worldCam.unproject(new Vector3(screenX, screenY, 0));
            //coordinates before clicking
            float[] prevVerts = this.proposedVertices;
            //Finalize the position of the last point
            prevVerts[prevVerts.length-2]=worldCoords.x;
            prevVerts[prevVerts.length-1]=worldCoords.y;
            //Replace the vertices with a set with room for a new point
            float[] newVerts = new float[prevVerts.length+2]; //1 point longer
            System.arraycopy(prevVerts, 0, newVerts, 0, prevVerts.length); // Plug the existing points in
            newVerts[prevVerts.length] = worldCoords.x; //Add a new point
            newVerts[prevVerts.length+1] = worldCoords.y;
            this.proposedVertices = newVerts;
        }

        @Override
        public void touchUp(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam) {

        }

        @Override
        public void buttonDown(int gameInput) {
            //When prompted to add a new terrain, start doing so
            if (gameInput == InputTranslator.GameInputs.NEW_ITEM) {
                if (this.proposedVertices == null) {//If not creating a new terrain shape, start making one
                    this.proposedVertices = new float[]{0,0}; //Initialize with dummy location to start
                }
            } else if (gameInput == InputTranslator.GameInputs.CONFIRM) {
                //If asked to finish that piece of terrain, stop editing it and add it to the map
                if (this.proposedVertices != null && this.proposedVertices.length >= 8) { //If it is editing a piece of terrain, and it has 3+ vertices proposed, add it (check for 4 because the last one is the next one to propose, not one that's actually been proposed yet)

                    // Finalize the vertices as they are, create a polygon out of them
                    float[] confirmedVertices = new float[this.proposedVertices.length-2];
                    System.arraycopy(this.proposedVertices, 0, confirmedVertices, 0, confirmedVertices.length);
                    Polygon terrainPolygon = new Polygon(confirmedVertices);

                    // Create a PolygonMapObject that would encode this in the base map file
                    PolygonMapObject baseMapObject = new PolygonMapObject(terrainPolygon);
                    MapProperties props = baseMapObject.getProperties();
                    props.put("id", nextAvailableID());
                    props.put("x", terrainPolygon.getX());
                    props.put("y", terrainPolygon.getY());

                    // Create a new polygon terrain using those
                    this.currentlyEditing = new StaticTerrainElement(baseMapObject, null);

                    // Create an edit which adds that polygon terrain to the map and map file
                    PlaceTerrainPolygon addThisTerrain = new PlaceTerrainPolygon(this.currentlyEditing, baseMapObject);
                    this.editStack.addEdit(addThisTerrain);

                    // Get rid of the current vertices
                    this.proposedVertices = null;
                }
            } else if (gameInput == InputTranslator.GameInputs.CANCEL) {
                //Stop editing whatever it is you're editing
                this.proposedVertices = null;
                this.currentlyEditing = null;
            }
        }

        public String nextAvailableID() {
            int IDNumber = 0; //Each id consists of some identifier + a number; to make an id first we need to see if any objects currently have ids using the same pre-number portion (and then go one number higher)

            WorldMap mapToEdit = this.editStack.getMap();
            String baseID = mapToEdit.getMapID() + "_terrain_"; // Identified as map + terrain + number

            // TODO: do we want to check the entity layer, or the entire base file, instead?
            for (Entity entity : mapToEdit.getEntities()) {
                String entityID = entity.getID(); //Check each entity's id:
                if (entityID.startsWith(baseID) && entityID.length() > baseID.length()) { //If the first part of string matches (and there is a modifier)
                    String modifier = entityID.substring(baseID.length()); // Find the modifier (most likely an id number)
                    if (MiscFunctions.isInteger(modifier)) {
                        int entityIDNum = Integer.parseInt(modifier);
                        if (entityIDNum >= IDNumber) {
                            IDNumber = entityIDNum + 1;
                        }
                    }
                }
            }
            String newTerrainID = baseID + IDNumber;
            return newTerrainID;
        }

        @Override
        public void buttonUp(int gameInput) {

        }

        @Override
        public void touchDragged(int screenX, int screenY, int pointer, OrthographicCamera worldCam) {

        }

        @Override
        public void mouseMoved(int screenX, int screenY, OrthographicCamera worldCam) {

        }

        @Override
        public void renderToScreen(SpriteBatch batch, OrthographicCamera screenCamera, float aspectRatio) {

        }
        @Override
        public void renderToWorld(SpriteBatch batch, OrthographicCamera worldCamera) {


            //Ensure the shape drawer is initialized
            if (this.shapeDrawer == null) {
                TextureRegion region = new TextureRegion(this.editStack.getMap().blankTexture, 1, 1); //Use blank texture for shape
                this.shapeDrawer = new ShapeDrawer(batch, region);
            }

            if (this.proposedVertices == null) return; // Don't render the terrain if none is selected

            this.shapeDrawer.setColor(Color.YELLOW);
            int numVerts = this.proposedVertices.length/2;
            float lineWidth = 1;

            Vector3 mousePos = worldCamera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));//Find the world position they're looking at
            float[] verts = this.proposedVertices; //Update the display of the proposed last vertex
            verts[verts.length-2]=mousePos.x;
            verts[verts.length-1]=mousePos.y;

            //this.shapeDrawer.filledCircle(mousePos.x, mousePos.y, lineWidth);

            if (numVerts == 0) return; //If there's nothing, don't start drawing
            if (numVerts == 1) { //Just the proposed of the first
                this.shapeDrawer.circle(verts[0], verts[1], lineWidth);
            } else { //More than 1 point
                if (numVerts == 2) { //Two points
//                float[] verts = this.currentlyEditing.polygon.getTransformedVertices();
                    this.shapeDrawer.line(verts[0], verts[1], verts[2], verts[3], lineWidth);
                } else { //More than 2, we got a polygon
                    this.shapeDrawer.polygon(this.proposedVertices, lineWidth, JoinType.POINTY);
                }
            }
        }
    }),

    /**The idle editor behavior, where map objects can be selected*/
    IDLE(new MapEditingTool() {
        /**When idling, if the mouse hovers over an entity, it might be selectable*/
        Entity possibleSelection;
        /**If applicable, details on how the entity would be selected.
         * The exact implementation of this might vary, but for a polygon terrain, this would be which indices to select*/
        int[] selectionDetails;
        /**The utility object to highlight the terrain, etc.*/
        ShapeDrawer shapeDrawer;

        /**The different ways of selecting objects. Currently, only REPLACE is implemented, and TODO: this might be better as an editor-wide thing */
        final int REPLACE = 0, ADD = 1, SUBTRACT = 2;
        int selectionMode = REPLACE;
        @Override
        public void touchDown(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam) {

        }

        @Override
        public void touchUp(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam) {

        }

        @Override
        public void buttonDown(int gameInput) {
        }

        @Override
        public void buttonUp(int gameInput) {

        }

        @Override
        public void touchDragged(int screenX, int screenY, int pointer, OrthographicCamera worldCam) {

        }

        @Override
        public void mouseMoved(int screenX, int screenY, OrthographicCamera worldCam) {
            // Reset whether it can be selected
            this.possibleSelection = null;

            // Identify where the mouse is in the world
            Vector3 worldCoords = worldCam.unproject(new Vector3(screenX, screenY, 0));

            ArrayList<Entity> entities = this.editStack.getMap().getEntities();
            float selectionDist = Float.MAX_VALUE; // For deciding which object is selected, sometimes it might come down to distance
            float maxSelectDist = 5; // The maximum distance, in world coordinates, the mouse can be away from a vertex to select it
            // Iterate through each entity, and if it might be selected, highlight it
            for (Entity entity : entities) { //Different elements can be selected in different ways
                if (entity instanceof StaticTerrainElement) {
                    // For a static terrain element, that just means hovering close to one of the vertices, or a segment
                    float[] vertices = ((StaticTerrainElement) entity).polygon.getTransformedVertices();
                    for (int i = 0; i < vertices.length/2; i++) { // For each point on the terrain:
                        float x = vertices[i*2], y = vertices[i*2 + 1]; //Identify coordinates of that point
                        float dx = worldCoords.x-x, dy = worldCoords.y-y; //Displacement from that point to the mouse

                        float dist = (float)Math.sqrt(dx * dx + dy * dy); //How far from the mouse it is

                        if (dist < maxSelectDist && dist < selectionDist) { //If it's an acceptable distance away and closer than any other points to select
                            selectionDist = dist;
                            this.possibleSelection = entity; // This entity has been selected
                            this.selectionDetails = new int[]{i}; // And this index of that entity in particular
                        }
                    }
                }
            }
        }

        @Override
        public void renderToScreen(SpriteBatch batch, OrthographicCamera screenCamera, float aspectRatio) {

        }

        @Override
        public void renderToWorld(SpriteBatch batch, OrthographicCamera worldCamera) {
            //Ensure the shape drawer is initialized
            if (this.shapeDrawer == null) {
                TextureRegion region = new TextureRegion(this.editStack.getMap().blankTexture, 1, 1); //Use blank texture for shape
                this.shapeDrawer = new ShapeDrawer(batch, region);
            }

            //If something is selected, highlight it
            if (this.possibleSelection == null) return;

            if (this.possibleSelection instanceof StaticTerrainElement) { // If it's a polygon terrain highlight the polygon being selected and especially the point in question
                // Vertices of the selected polygon
                float[] vertices = ((StaticTerrainElement) this.possibleSelection).polygon.getTransformedVertices();

                // Draw the polygon in darker yellow
                this.shapeDrawer.setColor(Color.GOLD);
                float lineWidth = 1;
                this.shapeDrawer.polygon(vertices, lineWidth, JoinType.POINTY);

                // Then draw the specific point in bright yellow
                this.shapeDrawer.setColor(Color.YELLOW);
                int idx = this.selectionDetails[0]; //TODO: Make this highlight multiple points
                this.shapeDrawer.filledCircle(vertices[idx*2], vertices[idx*2+1], 2*lineWidth);
            }
        }
    })
    ;
    public MapEditingTool toolInstance;

    Tools(MapEditingTool toolInstance) {
        this.toolInstance = toolInstance;
        this.toolInstance.setName(this.name());
    }

    public MapEditingTool getInstance() {
        return this.toolInstance;
    }
}

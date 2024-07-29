package com.mygdx.tempto.editing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import com.mygdx.tempto.entity.Entity;
import com.mygdx.tempto.entity.StaticTerrainElement;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.util.MiscFunctions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Vector;

import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

/**A set of implementations of {@link MapEditingTool}, instantiated once per game, which offer basic functionality such as editing */
public enum Tools {

    ADD_TERRAIN(new MapEditingTool() {

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
        public void activate() {

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

    /**Select specific vertices of polygon entities*/
    SELECT_VERTICES(new MapEditingTool() {

        /**The entity (ies?) currently selected*/
        ArrayList<Entity> currentlySelected;
        /**The details about each entity selected*/
        HashMap<Entity, IntArray> currentlySelectedDetails;

        /**When idling, if the mouse hovers over an entity, it might be selectable*/
        Entity possibleSelection;
        /**If applicable, details on how the entity would be selected.
         * The exact implementation of this might vary, but for a polygon terrain, this would be which indices to select*/
        IntArray possibleSelectionDetails;
        /**The utility object to highlight the terrain, etc.*/
        ShapeDrawer shapeDrawer;

        /**The different ways of selecting objects. Currently, only REPLACE is implemented, and TODO: this might be better as an editor-wide thing */
        final int REPLACE = 0, ADD = 1, SUBTRACT = 2;
        int selectionMode = REPLACE;
        /**If the last click down occured outside of any objects to select, the location of clicking down is saved. This way, when the*/
        Vector2 dragSelectStart;

        @Override
        public void touchDown(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam) {
            ArrayList<Entity> worldEntities = this.editStack.getMap().getEntities();
            for (Entity entity : worldEntities) {

                if (entity instanceof StaticTerrainElement terrainElement) {
                    float[] verts = terrainElement.polygon.getTransformedVertices();
                    System.out.println("World contains terrain " + terrainElement.getID() + " at vertices " + Arrays.toString(verts));
                }
            }

            Vector3 worldCoords3D = worldCam.unproject(new Vector3(screenX, screenY, 0));
            Vector2 worldCoords = new Vector2(worldCoords3D.x, worldCoords3D.y);


            if (this.currentlySelected == null) this.currentlySelected = new ArrayList<>();
            if (this.currentlySelectedDetails == null) this.currentlySelectedDetails = new HashMap<>();

            // Identify mode of selection
            this.selectionMode = REPLACE;
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                this.selectionMode = ADD;
            } else if (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)) {
                this.selectionMode = SUBTRACT;
            }


            if (this.possibleSelection != null) { //If it is currently hovering over something, add/replace/subtract to the selection
                this.dragSelectStart = null; //Not dragging to select, ergo don't save the starting position
                switch (this.selectionMode) {
                    case (REPLACE) -> { //If desiring to replace the current selection, clear the lists and replace with just the new selection
                        // Unique to replace mode: They might be trying to move the vertices. Try to move the vertices, and then if it can't do that, replace the selection with this one vertex
                        // If the new selection is contained in the existing selection, it might be trying to move.
                        // But if the new selection is different, it is definitely trying to replace the selection.
                        boolean itemAndAllDetailsSelected = true;
                        if (!this.currentlySelected.contains(this.possibleSelection)) {
                            itemAndAllDetailsSelected = false;
                        } else {
                            boolean allDetailsAlreadySelected = true;
                            IntArray alreadySelectedDetails = this.currentlySelectedDetails.get(this.possibleSelection);
                            for (int detail : this.possibleSelectionDetails.shrink()) {
                                // If any new details are selected, interpret this as trying to replace the selection unambiguously
                                if (!alreadySelectedDetails.contains(detail)) {
                                    allDetailsAlreadySelected = false;
                                    break;
                                }
                            }
                            //Any new details? Then it wasn't already present, replace the existing selection with this one
                            if (!allDetailsAlreadySelected) {
                                itemAndAllDetailsSelected = false;
                            }
                        }

                        if (!itemAndAllDetailsSelected) {
                            this.currentlySelected.clear();
                            this.currentlySelected.add(this.possibleSelection);
                            this.currentlySelectedDetails.clear();
                            this.currentlySelectedDetails.put(this.possibleSelection, this.possibleSelectionDetails);
                        }


                        //Switch to tool for dragging vertices of terrain around, which in turn, if dragged a substantial amount, will then move the vertices around by that much
                        this.toolContext.put("selected", this.currentlySelected);
                        this.toolContext.put("selectedDetails", this.currentlySelectedDetails);
                        this.toolContext.put("dragStart", worldCoords);
                        this.toolContext.put("lastSelected", this.possibleSelection);
                        this.toolContext.put("lastSelectedDetails", this.possibleSelectionDetails);
                        this.switchToTool(Tools.DRAG_TERRAIN_VERTICES.toolInstance);
                    }
                    case (ADD) -> { //If adding, only add it to the existing selection
                        if (!this.currentlySelected.contains(this.possibleSelection)) this.currentlySelected.add(this.possibleSelection);
                        // If some of its vertices were already selected, just add this one instead
                        if (this.currentlySelectedDetails.containsKey(this.possibleSelection)) {
                            IntArray alreadySelected = this.currentlySelectedDetails.get(this.possibleSelection);
                            for (int detail : this.possibleSelectionDetails.shrink()) { //Add each detail that isn't there already, add it
                                if (!alreadySelected.contains(detail)) alreadySelected.add(detail);
                            }
                        } else { //If it's a new piece of terrain, just go ahead and put the new list in
                            this.currentlySelectedDetails.put(this.possibleSelection, this.possibleSelectionDetails);
                        }
                    }
                    case (SUBTRACT) -> { //If subtracting, remove from existing selection
                        // If the item was present
                        if (this.currentlySelected.contains(this.possibleSelection)) {
                            IntArray detailsSelected = this.currentlySelectedDetails.get(this.possibleSelection);
                            detailsSelected.removeAll(this.possibleSelectionDetails);
                            detailsSelected.shrink();
                            //If it's the last selection detail on that item, remove it altogether
                            if (detailsSelected.size <= 0) {
                                System.out.println("Removing item with id: " + this.possibleSelection.getID());
                                this.currentlySelected.remove(this.possibleSelection);
                                this.currentlySelectedDetails.remove(this.possibleSelection);
                            }
                        }
                    }
                }
            } else { // If not hypothetically selecting something, reset the selection
                this.currentlySelected.clear();
                this.currentlySelectedDetails.clear();
            }
        }

        @Override
        public void touchUp(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam) {
            if (this.dragSelectStart != null) { //If it was dragging to select something
                Vector3 worldCoords = worldCam.unproject(new Vector3(screenX, screenY, 0));

                // Create a rectangle from where the selection started to where it ended
                float width = worldCoords.x-this.dragSelectStart.x, height = worldCoords.y-this.dragSelectStart.y;
                Rectangle selectionRectangle = new Rectangle(this.dragSelectStart.x, this.dragSelectStart.y, width, height);

                // Select everything in that rectangle
                switch(this.selectionMode){

                }
            }
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
                if (entity instanceof StaticTerrainElement terrainElement) {
                    // Check to see if this entity is even remotely selectable, and if not, ignore it
                    Rectangle boundingBox = terrainElement.polygon.getBoundingRectangle();
                    boundingBox.x -= maxSelectDist;
                    boundingBox.y -= maxSelectDist;
                    boundingBox.width += 2*maxSelectDist;
                    boundingBox.height += 2*maxSelectDist;
                    // If a box stretched a little farther than the terrain in every direction doesn't contain the mouse, it won't be selected
                    if (!boundingBox.contains(worldCoords.x, worldCoords.y)) continue;

                    // For a static terrain element, that just means hovering close to one of the vertices, or a segment, or the whole thing
                    float[] vertices = terrainElement.polygon.getTransformedVertices();
                    boolean foundAVertex = false;
                    for (int i = 0; i < vertices.length/2; i++) { // For each point on the terrain:
                        float x = vertices[i*2], y = vertices[i*2 + 1]; //Identify coordinates of that point
                        float dx = worldCoords.x-x, dy = worldCoords.y-y; //Displacement from that point to the mouse

                        float dist = (float)Math.sqrt(dx * dx + dy * dy); //How far from the mouse it is

                        if (dist < maxSelectDist && dist < selectionDist) { //If it's an acceptable distance away and closer than any other points to select
                            selectionDist = dist;
                            this.possibleSelection = entity; // This entity has been selected
                            this.possibleSelectionDetails = new IntArray(new int[]{i}); // And this index of that entity in particular
                            foundAVertex = true;
                        }
                    }
                    // If a specific vertex was found, don't need to see if a segment or the whole thing is selected
                    if (foundAVertex) continue;
                    boolean foundASegment = false;
                    for (int i = 0; i < vertices.length/2; i++) { // Iterate through each segment:
                        float x1 = vertices[i*2], y1 = vertices[i*2 + 1]; //Identify coordinates of that point
                        int j = i + 1;
                        if (j >= vertices.length/2) j = 0;
                        float x2 = vertices[j*2], y2 = vertices[j*2+1]; //Identify coordinates of the next point

                        Vector2 nearestOnSegment = Intersector.nearestSegmentPoint(x1, y1, x2, y2, worldCoords.x, worldCoords.y, new Vector2());

                        float dist = nearestOnSegment.sub(worldCoords.x, worldCoords.y).len(); //Displacement from that point to the mouse, and thus distance

                        if (dist < maxSelectDist && dist < selectionDist) { //If it's an acceptable distance away and closer than any other points to select
                            selectionDist = dist;
                            this.possibleSelection = entity; // This entity has been selected
                            this.possibleSelectionDetails = new IntArray(new int[]{i, j}); // And these indices of that entity in particular
                            foundASegment = true;
                        }
                    }

                    //If a specific segment or vertex was found, don't try and select the whole polygon
                    if (foundASegment) continue;
                    //If it couldn't select an individual point or segment, see if it can select the whole polygon
                    if (Intersector.isPointInPolygon(vertices, 0, vertices.length, worldCoords.x, worldCoords.y)) {
                        //Give selecting polygons least priority
                        float dist = maxSelectDist;
                        if (dist <= selectionDist) { //If basically nothign else could be found
                            selectionDist = dist;
                            this.possibleSelection = entity;
                            //Create an int array representing every single vertex on the polygon
                            int[] allVertices = new int[vertices.length/2];
                            for (int i = 0; i < allVertices.length; i++) allVertices[i]=i;
                            //Save that as the selection details
                            this.possibleSelectionDetails = new IntArray(allVertices);
                        }
                    }
                }
            }
        }

        @Override
        public void activate() {
            // Check if any vertices and terrain bits have already been selected
            if (this.toolContext.containsKey("selected")) {
                this.currentlySelected = (ArrayList<Entity>) this.toolContext.get("selected");
                this.currentlySelectedDetails = (HashMap<Entity, IntArray>) this.toolContext.get("selectedDetails");

                //If it was just on terrain dragging, check if it actually dragged the terrain, or if it actually wanted to replace the selection
                if (this.toolContext.get("lastTool").equals(DRAG_TERRAIN_VERTICES.name())) {
                    boolean movedVertices = (boolean) this.toolContext.get("movedVertices?");
                    System.out.println("Moved vertices? " + movedVertices);
                    if (!movedVertices) { //If it didn't move vertices, then it was actually just trying to select specifically that last point
                        Entity lastSelected = (Entity) this.toolContext.get("lastSelected");
                        IntArray lastSelectedDetails = (IntArray) this.toolContext.get("lastSelectedDetails");
                        this.currentlySelected.clear();
                        this.currentlySelected.add(lastSelected);
                        this.currentlySelectedDetails.clear();
                        this.currentlySelectedDetails.put(lastSelected, lastSelectedDetails);
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

            // Extreme debugging measure bc i don't know wtf is going on with this
            if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
                for (Entity entity : this.currentlySelected) {
                    System.out.println("Currently selected: " + entity.getID());
                    System.out.println("Contained in map? " + this.editStack.getMap().getEntities().contains(entity));
                    if (entity instanceof StaticTerrainElement terrainElement) {
                        float[] verts = terrainElement.polygon.getTransformedVertices();
                        Vector2 toCam = new Vector2(worldCamera.position.x, worldCamera.position.y).sub(verts[0], verts[1]);
                        for (int i = 0; i < verts.length/2; i++) {
                            verts[i*2]+= toCam.x;
                            verts[i*2+1]+= toCam.y;
                        }
                        terrainElement.polygon.setVertices(verts);
                        System.out.println("Snapping to camera!");
                        System.out.println("New vertices: " + Arrays.toString(verts));
                    }
                }
            }


            if (this.currentlySelected != null) {
                // Highlight whatever is already selected
                for (Entity entity : this.currentlySelected) {
                    if (entity instanceof StaticTerrainElement terrain) {
                        // Vertices of the selected polygon
                        float[] vertices = terrain.polygon.getTransformedVertices();
                        // Which vertices to highlight in particular
                        IntArray selectedVerts = this.currentlySelectedDetails.get(entity);

                        //Highlight the polygon in darker green
                        this.shapeDrawer.setColor(new Color(0, 0.5f, 0, 1));
                        float lineWidth = 1;
                        this.shapeDrawer.polygon(vertices, lineWidth, JoinType.POINTY);

                        this.shapeDrawer.setColor(Color.LIME);
                        // For each selected vertex, highlight it in lime green
                        for (int idx : selectedVerts.shrink()) {
                            this.shapeDrawer.filledCircle(vertices[idx * 2], vertices[idx * 2 + 1], 2 * lineWidth);
                        }
                    }
                }
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
                for (int idx : this.possibleSelectionDetails.shrink()) {
                    this.shapeDrawer.filledCircle(vertices[idx * 2], vertices[idx * 2 + 1], 2 * lineWidth);
                }
            }
        }
    }),

    /**Switched to after starting to drag a vertex. Switches back to idle after letting go.*/
    DRAG_TERRAIN_VERTICES(new MapEditingTool() {

        public ArrayList<Entity> selected;
        public HashMap<Entity, IntArray> selectedVertices;

        /**Specific to point selection: Since clicking to drag vs clicking to select only a point is ambiguous, the last point clicked is saved and handed back*/
        public Entity lastSelected;
        public IntArray lastSelectedDetails;

        /**Where the current click/drag started.*/
        public Vector2 dragStart;
        /**Where the current click/drag is now compared to when it started*/
        public Vector2 currentDisplacement = new Vector2();
        /**The minimum distance the mouse has to move for the drag to be registered as dragging any vertices (as opposed to just picking vertices*/
        public static final float MIN_MOVE_DIST = 5;
        /**Whether, at any point while the tool was active, the mouse was far enough from where it started that we can assume the user actually wants to move the vertices*/
        public boolean movedMouse = false;

        /**Util to render polygons*/
        ShapeDrawer shapeDrawer;

        /**Edit that takes a selection of terrain elements, and moves them from prior to new vertices*/
        public class ChangeTerrainVertices extends MapEdit{
            ArrayList<StaticTerrainElement> terrainElements;
            HashMap<StaticTerrainElement, float[]> beforeVerts;
            HashMap<StaticTerrainElement, float[]> afterVerts;

            public ChangeTerrainVertices(ArrayList<StaticTerrainElement> terrainElements, HashMap<StaticTerrainElement, float[]> priorVertices, HashMap<StaticTerrainElement, float[]> afterVerts) {
                this.terrainElements = new ArrayList<>(terrainElements);//Copy the selection of terrain elements to apply changes to
                this.beforeVerts = new HashMap<>(priorVertices); //Make a copy of the vertices from before

                this.afterVerts = new HashMap<>(afterVerts); //Make a copy of the vertices from after
            }

            @Override
            public void undoEdit(WorldMap map) {
                // For each terrain element, get the previous vertices it held before the edit, and then set it to those
                for (StaticTerrainElement terrainElement : this.terrainElements) {
                    float[] prevVerts = this.beforeVerts.get(terrainElement);
                    terrainElement.polygon.setVertices(prevVerts);
                }
            }

            @Override
            public void redoEdit(WorldMap map) {
                // For each terrain element, get the future vertices it holds after the edit, and then set it to those
                for (StaticTerrainElement terrainElement : this.terrainElements) {
                    System.out.println("Editing terrain id: " + terrainElement.getID());
                    float[] nextVerts = this.afterVerts.get(terrainElement);
                    System.out.println(Arrays.toString(nextVerts));
                    terrainElement.polygon.setVertices(nextVerts);
                }
//                ArrayList<Entity> entities = map.getEntities();
//                for (Entity entity : entities) {
//                    System.out.println("map contains: " + entity.getID());
//                }
            }
        }

        @Override
        public void touchDown(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam) {

        }

        @Override
        public void touchUp(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam) {
            if (this.movedMouse) { //If the mouse has moved at all, edit the vertices of all selected terrain elements
                //How much to move each vertex
                Vector3 touchEnd = worldCam.unproject(new Vector3(screenX, screenY, 0));
                this.currentDisplacement.set(touchEnd.x, touchEnd.y).sub(this.dragStart);

                ArrayList<StaticTerrainElement> toChange = new ArrayList<>();
                HashMap<StaticTerrainElement, float[]> priorVertices = new HashMap<>();
                HashMap<StaticTerrainElement, float[]> afterVertices = new HashMap<>();
                for (Entity entity : this.selected) {
                    if (entity instanceof StaticTerrainElement terrain) {
                        // Register to change that terrain
                        toChange.add(terrain);
                        // Save what the vertices were like before the change
                        priorVertices.put(terrain, terrain.polygon.getVertices().clone());
                        // Change the vertices of this terrain element according to which vertices were chosen to edit, and how far the mouse has been dragged
                        float[] newVerts = terrain.polygon.getVertices().clone();
                        int[] indicesToChange = this.selectedVertices.get(entity).shrink(); // Which vertices to change
                        for (int idx : indicesToChange) {
                            int xi = idx*2, yi = idx*2+1;//Indices of that point in the vertices
                            // Move that point by the amount dragged
                            newVerts[xi] += this.currentDisplacement.x;
                            newVerts[yi] += this.currentDisplacement.y;
                        }
                        // Save the changed vertices
                        afterVertices.put(terrain, newVerts);
                    }
                }
                // Save the change that was made, and apply it to the stack
                ChangeTerrainVertices newEdit = new ChangeTerrainVertices(toChange, priorVertices, afterVertices);
                this.editStack.addEdit(newEdit);
            }

            // Whether or not the terrain was changed, switch back to vertex selection tool
            HashMap<String, Object> context = this.toolContext;
            context.put("selected", this.selected);
            context.put("selectedDetails", this.selectedVertices);
            context.put("movedVertices?", this.movedMouse);
            if (this.lastSelected != null) {
                context.put("lastSelected", this.lastSelected);
                context.put("lastSelectedDetails", this.lastSelectedDetails);
            }
            this.switchToTool(Tools.SELECT_VERTICES.toolInstance);
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
            Vector3 worldCoords = worldCam.unproject(new Vector3(screenX, screenY, 0));

            //Identify how far the mouse currently is from where the drag started
            this.currentDisplacement.set(worldCoords.x, worldCoords.y).sub(this.dragStart);
            float dist = this.currentDisplacement.len();
            //If it's far enough, register that the user actually wants to move vertices
            if (dist > MIN_MOVE_DIST) {
                this.movedMouse = true;
            }
        }

        @Override
        public void activate() {
            if (this.toolContext.containsKey("selected")) {
                this.selected = (ArrayList<Entity>) this.toolContext.get("selected");
                this.selectedVertices = (HashMap<Entity, IntArray>) this.toolContext.get("selectedDetails");

                this.dragStart = (Vector2) this.toolContext.get("dragStart");
                this.movedMouse = false;

                if (this.toolContext.get("lastTool").equals(SELECT_VERTICES.name())) { //If it just came from selecting vertices, which frankly it probably did, register which one it just clicked
                    this.lastSelected = (Entity) this.toolContext.get("lastSelected");
                    this.lastSelectedDetails = (IntArray) this.toolContext.get("lastSelectedDetails");
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
            this.shapeDrawer.setColor(Color.LIME);
            float lineWidth = 1f;

            //For each object being affected, render its hypothetical change
            for (Entity entity : this.selected) {
                if (entity instanceof StaticTerrainElement terrain) { //For terrain, that looks like moving each affected vertex by the amount dragged
                    // Change the vertices of this terrain element according to which vertices were chosen to edit, and how far the mouse has been dragged
                    float[] newVerts = terrain.polygon.getTransformedVertices().clone();
                    int[] indicesToChange = this.selectedVertices.get(entity).shrink(); // Which vertices to change
                    for (int idx : indicesToChange) {
                        int xi = idx*2, yi = idx*2+1;//Indices of that point in the vertices
                        // Move that point by the amount dragged
                        newVerts[xi] += this.currentDisplacement.x;
                        newVerts[yi] += this.currentDisplacement.y;
                    }
                    // Render the changed vertices
                    this.shapeDrawer.polygon(newVerts, lineWidth, JoinType.POINTY);
                }
            }
        }
    }),
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

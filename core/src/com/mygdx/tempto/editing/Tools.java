package com.mygdx.tempto.editing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.mygdx.tempto.entity.Entity;
import com.mygdx.tempto.entity.StaticTerrainElement;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.util.MiscFunctions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;
import java.util.function.Predicate;

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
//            if (gameInput == InputTranslator.GameInputs.NEW_ITEM) {
//                if (this.proposedVertices == null) {//If not creating a new terrain shape, start making one
//                }
//            } else
            if (gameInput == InputTranslator.GameInputs.CONFIRM) {
                //If asked to finish that piece of terrain, stop editing it and add it to the map
                if (this.proposedVertices != null && this.proposedVertices.length >= 8) { //If it is editing a piece of terrain, and it has 3+ vertices proposed, add it (check for 4 because the last one is the next one to propose, not one that's actually been proposed yet)

                    // Finalize the vertices as they are, create a polygon out of them
                    float[] confirmedVertices = new float[this.proposedVertices.length-2];
                    System.arraycopy(this.proposedVertices, 0, confirmedVertices, 0, confirmedVertices.length);
                    Polygon terrainPolygon = new Polygon(confirmedVertices);

                    // Create a PolygonMapObject that would encode this in the base map file
                    PolygonMapObject baseMapObject = new PolygonMapObject(terrainPolygon);
                    MapProperties props = baseMapObject.getProperties();
                    props.put("id", nextAvailableTerrainID(this.editStack.getMap()));
                    props.put("x", terrainPolygon.getX());
                    props.put("y", terrainPolygon.getY());

                    // Create a new polygon terrain using those
                    this.currentlyEditing = new StaticTerrainElement(baseMapObject, null);

                    // Create an edit which adds that polygon terrain to the map and map file
                    PlaceTerrainPolygon addThisTerrain = new PlaceTerrainPolygon(this.currentlyEditing, baseMapObject);
                    this.editStack.addEdit(addThisTerrain);

                    // Switch to terrain vertex selector
                    ArrayList<Entity> selection = new ArrayList<>();
                    selection.add(this.currentlyEditing);
                    this.toolContext.put("selected", selection);
                    // Select all of its indices
                    IntArray vertIndices = new IntArray(confirmedVertices.length/2);
                    for (int i = 0; i < confirmedVertices.length/2; i++) vertIndices.add(i);
                    HashMap<Entity, IntArray> selectionDetails = new HashMap<>();
                    selectionDetails.put(this.currentlyEditing, vertIndices);
                    this.toolContext.put("selectedDetails", selectionDetails);

                    this.switchToTool(SELECT_VERTICES.toolInstance);
                }
            } else if (gameInput == InputTranslator.GameInputs.CANCEL) {
                //Stop editing whatever it is you're editing
                this.proposedVertices = null;
                this.currentlyEditing = null;
                //And switch back to the vertex selection
                this.toolContext.put("selected", new ArrayList<>());
                this.toolContext.put("selectedDetails", new HashMap<>());
                this.switchToTool(SELECT_VERTICES.toolInstance);
            }
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
            this.proposedVertices = new float[]{0,0}; //Initialize with dummy location to start
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
        /**If the suggested entity {@link #possibleSelectionDetails} was forcibly found using the tab key.
         * I.e., if multiple objects could have been selected, but the user used the tab key to specifically pick one of them.
         * This will keep priority on that possible selection until it can no longer be selected*/
        boolean forciblySelected;
        /**The utility object to highlight the terrain, etc.*/
        ShapeDrawer shapeDrawer;

        /**The different ways of selecting objects. Currently, only REPLACE is implemented, and TODO: this might be better as an editor-wide thing */
        final int REPLACE = 0, ADD = 1, SUBTRACT = 2;
        int selectionMode = REPLACE;
        /**If the last click down occured outside of any objects to select, the location of clicking down is saved. This way, when the*/
        Vector2 dragSelectStart;

        /**A kind of edit that can both remove vertices from terrain, or remove the terrain from the map and file altogether.*/
        public class DeleteTerrainOrVertices extends MapEdit {

            ArrayList<Entity> removedOutright; //The terrain pieces removed outright
            HashMap<Entity, MapObject> removedCoreFileObjects; //The MapObjects corresponding to the removed terrain pieces
            ArrayList<StaticTerrainElement> removeJustVertices; //The terrain pieces which only have vertices removed from them
            HashMap<StaticTerrainElement, float[]> beforeRemovingVerts; //The vertices of each terrain piece in removeJustVertices before the verts were removed
            HashMap<StaticTerrainElement, float[]> afterRemovingVerts; //The vertices of each modified terrain piece, but after removing them


            public DeleteTerrainOrVertices(ArrayList<Entity> removedOutright, HashMap<Entity, MapObject> removedCoreFileObjects, ArrayList<StaticTerrainElement> removeJustVertices, HashMap<StaticTerrainElement, float[]> beforeRemovingVerts, HashMap<StaticTerrainElement, float[]> afterRemovingVerts) {
                this.removedOutright = removedOutright;
                this.removedCoreFileObjects = removedCoreFileObjects;
                this.removeJustVertices = removeJustVertices;
                this.beforeRemovingVerts = beforeRemovingVerts;
                this.afterRemovingVerts = afterRemovingVerts;
            }

            @Override
            public void undoEdit(WorldMap map) {
                // Restore the ones removed outright
                for (Entity toRestore : this.removedOutright){
                    map.addEntity(toRestore);
                    map.getEntityLayer().getObjects().add(this.removedCoreFileObjects.get(toRestore));
                }
                // Restore the vertices of the ones that were only modified
                for (StaticTerrainElement toRestoreVertices : this.removeJustVertices) {
                    float[] ogVerts = this.beforeRemovingVerts.get(toRestoreVertices);
                    toRestoreVertices.polygon.setVertices(ogVerts);
                }
            }

            @Override
            public void redoEdit(WorldMap map) {
                // Remove the ones that were removed outright
                for (Entity toRemove : this.removedOutright){
                    map.removeEntity(toRemove);
                    map.getEntityLayer().getObjects().remove(this.removedCoreFileObjects.get(toRemove));
                }
                // Remove the vertices of the ones that were only modified
                for (StaticTerrainElement toRemoveVerts : this.removeJustVertices) {
                    float[] newVerts = this.afterRemovingVerts.get(toRemoveVerts);
                    toRemoveVerts.polygon.setVertices(newVerts);
                }
            }
        }

        @Override
        public void touchDown(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam) {


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
            if (gameInput == InputTranslator.GameInputs.BRUSH) { //Switch to terrain brush
                this.switchToTool(Tools.TERRAIN_BRUSH.toolInstance);
            } else if (gameInput == InputTranslator.GameInputs.NEW_ITEM) { //Create a new StaticTerrainElement
                //TODO: do we want to keep the previous selected items selected?
                this.switchToTool(Tools.ADD_TERRAIN.toolInstance);
            } else if (gameInput == InputTranslator.GameInputs.DELETE) { //Delete selected
                ArrayList<Entity> toDeleteOutright = new ArrayList<>();
                HashMap<Entity, MapObject> toDelteFromCoreFile = new HashMap<>();

                ArrayList<StaticTerrainElement> toOnlyRemoveVertices = new ArrayList<>();
                HashMap<StaticTerrainElement, float[]> priorVerts = new HashMap<>();
                HashMap<StaticTerrainElement, float[]> afterVerts = new HashMap<>();

                // Iterate through each selected entity and determine whether to remove vertices or remove outright
                for (Entity toModify : this.currentlySelected) {
                    boolean removeOutright = true;
                    if (toModify instanceof StaticTerrainElement terrainElement) { //If it's a piece of terrain, consider removing only some of the vertices
                        float[] vertsBefore = terrainElement.polygon.getVertices();
                        int numVertsBefore = vertsBefore.length/2;
                        IntArray vertsToRemove = this.currentlySelectedDetails.get(toModify);
                        int numVertsAfter = numVertsBefore-vertsToRemove.shrink().length;
                        if (numVertsAfter >= 3) { //If there would still be at least 3 vertices left, go ahead and trim
                            removeOutright = false; //Do not remove outright
                            FloatArray newVerts = new FloatArray(numVertsAfter*2); //Create a new array with the length of however many verts would remain
                            for (int i = 0; i < numVertsBefore; i++) {// For each vertex, add it if it's not listed as a vertex to remove
                                if (!vertsToRemove.contains(i)) {
                                    newVerts.add(vertsBefore[i*2]);
                                    newVerts.add(vertsBefore[i*2+1]);
                                }
                            }
                            float[] trimmedVerts = newVerts.shrink();

                            //Add to the lists of terrain objects to trim vertices from
                            toOnlyRemoveVertices.add(terrainElement);
                            priorVerts.put(terrainElement, vertsBefore);
                            afterVerts.put(terrainElement, trimmedVerts);

                        } else { //If there would be too few left, just remove the terrain outright
                            removeOutright = true;
                        }
                    }
                    if (removeOutright) {
                        toDeleteOutright.add(toModify);
                        MapObject inCoreFile = this.editStack.getMap().getMapObjectForEntity(toModify);
                        toDelteFromCoreFile.put(toModify, inCoreFile);
                    }
                }

                // Add this edit to the stack
                DeleteTerrainOrVertices deleteSelection = new DeleteTerrainOrVertices(
                    toDeleteOutright, toDelteFromCoreFile, toOnlyRemoveVertices, priorVerts, afterVerts
                );
                this.editStack.addEdit(deleteSelection);

                // Clear this selection TODO: Have this delete edit also restore the selection
                this.currentlySelected.clear();
                this.currentlySelectedDetails.clear();
            }
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
            //this.possibleSelection = null;

            // Identify where the mouse is in the world
            Vector3 worldCoords = worldCam.unproject(new Vector3(screenX, screenY, 0));

            ArrayList<Entity> entities = this.editStack.getMap().getEntities();
            float selectionDist = Float.MAX_VALUE; // For deciding which object is selected, sometimes it might come down to distance
            float maxSelectDist = TERRAIN_SELECTION_DISTANCE; // The maximum distance, in world coordinates, the mouse can be away from a vertex to select it

            // Identify all entities that can be selected
            HashMap<Entity, SelectionDetail> canSelectDetails = new HashMap<>();
            ArrayList<Entity> canSelect = canBeSelected(new Vector2(worldCoords.x, worldCoords.y), entities, new ArrayList<>(), canSelectDetails);
            // Focus on the terrain elements that can be selected
            ArrayList<StaticTerrainElement> selectableTerrain = new ArrayList<>();
            for (Entity entity : canSelect) {
                if (entity instanceof StaticTerrainElement terrain) {
                    selectableTerrain.add(terrain);
                }
            }


            // Find which entity is the most reasonable to select

            //If the last selection can't be selected anymore, don't consider it
            if (!canSelect.contains(this.possibleSelection)) {
                this.possibleSelection = null;
                //this.forciblySelected = false;
            }
            //If the current possible selection wasn't forcibly chosen, check if a different entity makes more sense to select
            if (this.possibleSelection == null){
                // Iterate through each entity, and if it should be selected over the current selection go for it
                for (StaticTerrainElement terrainElement : selectableTerrain) {
                    if (terrainElement == this.possibleSelection) continue; //No need to compare with itself
                    SelectionDetail entityDetails = canSelectDetails.get(terrainElement);
                    SelectionDetail currentDetails = canSelectDetails.get(this.possibleSelection);
                    // If the selection priority of the other entity is higher, look at that one first
                    if (this.possibleSelection == null || entityDetails.priority > currentDetails.priority) this.possibleSelection = terrainElement;
                }
            }
            //If the tab button is pressed (And there are other entities to select), switch to another entity which can be selected
            if (Gdx.input.isKeyJustPressed(Input.Keys.TAB) && !selectableTerrain.isEmpty()) {
                int currentIdx = selectableTerrain.indexOf(this.possibleSelection);
                int nextIdx = currentIdx+1;
                if (nextIdx >= selectableTerrain.size()) nextIdx = 0;
                this.possibleSelection = selectableTerrain.get(nextIdx);
                //this.forciblySelected = true;
            }

            //If settled on a terrain piece to look at, identify the details about it
            if (this.possibleSelection != null) this.possibleSelectionDetails = (IntArray) canSelectDetails.get(this.possibleSelection).otherDetails;

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
                    float[] nextVerts = this.afterVerts.get(terrainElement);
                    terrainElement.polygon.setVertices(nextVerts);
                }
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
                        // Ensure its vertices are based around the origin
                        terrain.polygon.setVertices(terrain.polygon.getTransformedVertices());
                        terrain.polygon.setPosition(0,0);
                        // Register to change that terrain
                        toChange.add(terrain);
                        // Save what the vertices were like before the change
                        priorVertices.put(terrain, terrain.polygon.getTransformedVertices().clone());
                        // Change the vertices of this terrain element according to which vertices were chosen to edit, and how far the mouse has been dragged
                        float[] newVerts = terrain.polygon.getTransformedVertices().clone();
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

    /**Wields a polygon like a brush, and adds or subtracts from existing {@link StaticTerrainElement}s*/
    TERRAIN_BRUSH(new MapEditingTool() {

        /**The polygon that follows the mouse and is added or subtracted from existing polygons*/
        Polygon brush;
        /**Rendering utility*/
        ShapeDrawer shapeDrawer;
        /**The pieces of terrain that could be subject to addition. Always includes a null instance, to represent creating a new terrain instead of adding to an existing one*/
        ArrayList<StaticTerrainElement> terrainCandiates;
        /**The terrain element currently speculated to add the brush to*/
        StaticTerrainElement mainCandidate;


        @Override
        public void touchDown(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam) {
            // On touchdown, add the brush to whatever terrain is desired, or if none are, as a new terrain itself
            MapEdit edit;
            if (this.mainCandidate != null) { // Candidate terrain specified: add to that terrain
                System.out.println("Adding brush to terrain: " + Arrays.toString(this.mainCandidate.polygon.getTransformedVertices()));
                edit = addToTerrain(this.brush, this.mainCandidate);
            } else { // Candidate terrain unspecified, add brush as a new terrain
                System.out.println("Adding brush as new terrain");
                edit = addTerrain(this.brush, this.editStack.getMap());
            }
            //Add the edit to the stack
            this.editStack.addEdit(edit);
//
//            //Check for the brush's total possible area of effect, by expanding around tolerance
//            Rectangle brushEffectArea = new Rectangle(this.brush.getBoundingRectangle());
//            float tol = MiscFunctions.DEFAULT_MERGE_TOLERANCE;
//            brushEffectArea.x-=tol;
//            brushEffectArea.y-=tol;
//            brushEffectArea.width+=tol*2;
//            brushEffectArea.height+=tol*2;
//
//            for (Entity entity : entities) {
//                if (entity instanceof StaticTerrainElement terrain) { // For each terrain element
//                    Polygon terrPoly = terrain.getPolygon(); // Check if it overlaps the brush
//                    if (!terrPoly.getBoundingRectangle().overlaps(brushEffectArea)) continue;
//                    if (Intersector.intersectPolygons(terrPoly, this.brush, null)
//                        || Intersector.intersectPolygonEdges(new FloatArray(terrPoly.getTransformedVertices()), new FloatArray(this.brush.getTransformedVertices()))) {
//                        affectedTerrains.add(terrain);
//                    }
//                }
//            }
//
//            //If any terrain are affected, perform a series of merges with them and the brush
//            if (!affectedTerrains.isEmpty()) {
//                //Pick the first affected terrain, and merge the brush into it
//                StaticTerrainElement firstTerrain = affectedTerrains.get(0);
//                Polygon union = MiscFunctions.polygonUnion(firstTerrain.polygon, this.brush);
//
//
//                ArrayList<MapEdit> subEdits = new ArrayList<>(1); //The edits made to merge the brush with any contacting terrain
//
//                //For any remaining terrain, merge their polygons into the first's union and remove them
//                for (int i = 1; i < affectedTerrains.size(); i++) {
//                    //Merge into existing union
//                    StaticTerrainElement terrain = affectedTerrains.get(i);
//                    union = MiscFunctions.polygonUnion(terrain.polygon, union);
//                    //Remove from map
//                    PolygonMapObject terrainInFile = (PolygonMapObject) this.editStack.getMap().getMapObjectForEntity(terrain);
//                    RemoveTerrainPolygon removeMerged = new RemoveTerrainPolygon(terrain, terrainInFile);
//                    subEdits.add(removeMerged);
//                }
//
//                //Replace the first terrain's vertices with the union of all of them
//
//                //Ensure the terrain is vertexed in absolute coordinates
//                firstTerrain.polygon.setVertices(firstTerrain.polygon.getTransformedVertices());
//                firstTerrain.polygon.setPosition(0,0);
//
//                SetTerrainVertices mergeIntoFirst = new SetTerrainVertices(firstTerrain,
//                        firstTerrain.polygon.getVertices().clone(),
//                        union.getVertices().clone());
//                subEdits.add(mergeIntoFirst);
//
//                //Combine all requisite edits into one big edit
//                ComboMapEdit brushMergeEdits = new ComboMapEdit(subEdits);
//                this.editStack.addEdit(brushMergeEdits);
//            } else { //If no overlaps found, add the brush polygon as is
//                Polygon brushPoly = new Polygon(this.brush.getTransformedVertices());
//
//                PlaceTerrainPolygon addBrush = addTerrain(brushPoly, this.editStack.getMap());
////
////                // Create a PolygonMapObject that would encode this in the base map file
////                PolygonMapObject baseMapObject = new PolygonMapObject(brushPoly);
////                MapProperties props = baseMapObject.getProperties();
////                props.put("id", nextAvailableTerrainID(this.editStack.getMap()));
////                props.put("x", brushPoly.getX());
////                props.put("y", brushPoly.getY());
////
////                // Create a new polygon terrain using those
////                StaticTerrainElement newTerrain = new StaticTerrainElement(baseMapObject, null);
////
////                // Create an edit which adds that polygon terrain to the map and map file
////                PlaceTerrainPolygon addThisTerrain = new PlaceTerrainPolygon(newTerrain, baseMapObject);
//                this.editStack.addEdit(addBrush);
//            }
//

        }

        @Override
        public void touchUp(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam) {

        }

        @Override
        public void buttonDown(int gameInput) {
            //If cancel/go back or vertex, go back to vertex selection
            if (gameInput == InputTranslator.GameInputs.CANCEL ||
                gameInput == InputTranslator.GameInputs.VERTEX) {
                switchToTool(SELECT_VERTICES.toolInstance);
            }
        }

        @Override
        public void buttonUp(int gameInput) {

        }

        @Override
        public void touchDragged(int screenX, int screenY, int pointer, OrthographicCamera worldCam) {

        }

        @Override
        public void mouseMoved(int screenX, int screenY, OrthographicCamera worldCam) {
            //Set the brush to follow the mouse
            Vector3 worldCoords = worldCam.unproject(new Vector3(screenX, screenY, 0));

            //If control is held, snap to the nearest tile
            if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                worldCoords.scl(1 / WorldMap.TILE_SIZE);
                worldCoords.x = (int) worldCoords.x;
                worldCoords.y = (int) worldCoords.y;
                worldCoords.scl(WorldMap.TILE_SIZE);
            }
            this.brush.setPosition(worldCoords.x, worldCoords.y);

            //Check if it could have selected any terrain pieces last time. This distinguishes between if the user couldn't pick any, or if they actively didn't want to (wanted to make a new terrain from the brush overlapping smt else)
            if (this.terrainCandiates == null) this.terrainCandiates = new ArrayList<>();
            boolean priorSelectionEmpty = this.terrainCandiates.size() <= 1;

            //Check which terrain pieces could be added to
            ArrayList<Entity> entities = this.editStack.getMap().getEntities();
            //Entities which overlap this brush
            ArrayList<Entity> overlappingEntities = canBeSelectedByPolygon(this.brush, entities, new ArrayList<>(), null);
            //Pick the terrain elements from that selection
            this.terrainCandiates.clear();
            for (Entity entity : overlappingEntities) if (entity instanceof StaticTerrainElement terrain) this.terrainCandiates.add(terrain);
            this.terrainCandiates.add(null);//Add a null instance to represent adding to nothing, i.e. making a new object
            boolean currentSelectionNotEmpty = this.terrainCandiates.size() > 1;

            //If the user moved mouse off of the current terrain, or from empty space onto a terrain, switch current selection
            if (!this.terrainCandiates.contains(this.mainCandidate) || (priorSelectionEmpty && currentSelectionNotEmpty)) {
                this.mainCandidate = this.terrainCandiates.get(0);
            }
            //Allow the user to scroll between them using the tab key
            if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
                int currentIdx = this.terrainCandiates.indexOf(this.mainCandidate);
                currentIdx++;
                if (currentIdx >= this.terrainCandiates.size()) currentIdx = 0;
                this.mainCandidate = this.terrainCandiates.get(currentIdx);
            }

        }

        @Override
        public void activate() {
            //Set the brush to a generic square
            float radius = WorldMap.TILE_SIZE;
            this.brush = new Polygon(new float[]{
                -radius,-radius,
                -radius, radius,
                 radius, radius,
                 radius,-radius
            });
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
            float lineWidth = 2.5f;

            //Draw the candidate
            if (this.mainCandidate != null) this.shapeDrawer.polygon(this.mainCandidate.polygon, lineWidth);

            //Draw the brush
            this.shapeDrawer.polygon(this.brush, lineWidth);


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

    //////// Common Edit Utilities ////////////////////////
    /**Moves the given {@link StaticTerrainElement} from one polygon to a new one*/
    private static class SetTerrainVertices extends MapEdit {

        StaticTerrainElement terrainElement;
        float[] before;
        float[] after;

        public SetTerrainVertices(StaticTerrainElement terrainElement, float[] before, float[] after) {
            this.terrainElement = terrainElement;
            this.before = before;
            this.after = after;
        }

        @Override
        public void undoEdit(WorldMap map) {
            this.terrainElement.polygon.setVertices(this.before);
        }

        @Override
        public void redoEdit(WorldMap map) {
            this.terrainElement.polygon.setVertices(this.after);
        }
    }

    /**Places a new piece of terrain in the map*/
    private static class PlaceTerrainPolygon extends MapEdit {

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

    /**Removes a piece of terrain from the map*/
    private static class RemoveTerrainPolygon extends MapEdit {

        StaticTerrainElement terrainElement;
        PolygonMapObject terrainInBaseFile;

        public RemoveTerrainPolygon(StaticTerrainElement terrainElement, PolygonMapObject terrainInBaseFile) {
            this.terrainElement = terrainElement;
            this.terrainInBaseFile = terrainInBaseFile;
        }

        @Override
        public void undoEdit(WorldMap map) {
            map.addEntity(this.terrainElement);
            map.getEntityLayer().getObjects().add(this.terrainInBaseFile);
        }

        @Override
        public void redoEdit(WorldMap map) {
            map.removeEntity(this.terrainElement);
            map.getEntityLayer().getObjects().remove(this.terrainInBaseFile);
        }
    }

    /**Utility method that searches the given {@link WorldMap} for the n*/
    public static String nextAvailableTerrainID(WorldMap mapToEdit) {
        int IDNumber = 0; //Each id consists of some identifier + a number; to make an id first we need to see if any objects currently have ids using the same pre-number portion (and then go one number higher)

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

    /**Utility method to add a polygon to a {@link StaticTerrainElement}*/
    private static SetTerrainVertices addToTerrain(Polygon toAdd, StaticTerrainElement terrain) {
        Polygon before = new Polygon(terrain.polygon.getTransformedVertices());
        float[] beforeVerts = before.getTransformedVertices();
        Polygon after = MiscFunctions.polygonUnion(before, toAdd);
        float[] afterVerts = after.getTransformedVertices();

        SetTerrainVertices edit = new SetTerrainVertices(terrain, beforeVerts, afterVerts);
        return edit;
    }

    /**Utility method to quickly build an edit for adding a piece of terrain from the given polygon*/
    private static PlaceTerrainPolygon addTerrain(Polygon form, WorldMap map) {
        //Copy the polygon, to avoid duplicating references
        form = new Polygon(form.getTransformedVertices());
        //Identify the id to give new terrain
        String id = nextAvailableTerrainID(map);
        //Generate map data
        PolygonMapObject mapData = new PolygonMapObject(form);
        MapProperties props = mapData.getProperties();
        props.put("x", form.getX());
        props.put("y", form.getY());
        props.put("id", id);
        //Create a corresponding StaticTerrainElement
        StaticTerrainElement terrain = new StaticTerrainElement(mapData);

        //Generate the map edit
        PlaceTerrainPolygon edit = new PlaceTerrainPolygon(terrain, mapData);

        return edit;
    }

    /**How far, in IGU, the cursor can be from a piece of terrain for it to still be selected. TODO: switch to some kind of screen-pixel distance*/
    public static final float TERRAIN_SELECTION_DISTANCE = 5f;
    /**A utility class to store details about a selected entity. Stores the selection priority, "distance" information, and an extra Object for any additional information required*/
    private static class SelectionDetail {

        /**The selected is a piece of terrain, but not any part of it in particular.*/
        public static final float TERRAIN_BASE = 1;
        /**The selected is a piece of terrain, and a specific edge thereof.*/
        public static final float TERRAIN_EDGE = 2;
        /**The selected is a piece of terrain, and a specific *point* thereof.*/
        public static final float TERRAIN_VERTEX = 3;

        public float priority;
        /**Additional information as necessary to interpret the selection. Examples for common entities:
         * {@link StaticTerrainElement}: An {@link IntArray} of selected vertices.*/
        public Object otherDetails;

        public SelectionDetail(float priority, Object otherDetails) {
            this.priority = priority;
            this.otherDetails = otherDetails;
        }
    }
    /**A utility method that identifies which entities can be selected from the given list of entities, by the given mouse location.
     * @param mouseLocation The location of the mouse, in IGU (In Game Units)
     * @param entities The list of entities to consider
     * @param selectedEntities The list to store selectable entities in. Will be the list returned at the end, unless null is given, in which case a new list will be created.
     * @param selectionDetails A hashmap in which to store {@link SelectionDetail} for each selected entity, if desired. If null, will not store such details.*/
    private static ArrayList<Entity> canBeSelected(Vector2 mouseLocation, ArrayList<Entity> entities, ArrayList<Entity> selectedEntities, HashMap<Entity, SelectionDetail> selectionDetails) {
        //Clear or handle the existing selection
        selectedEntities.clear();

        //Iterate through each entity, and determine for each if they should reasonably be selected
        for (Entity entity : entities) {
            //In the case of terrain, selection means the cursor is inside or near the terrain piece
            if (entity instanceof StaticTerrainElement terrain) {
                //Check if the cursor is even in the vicinity of the polygon
                float extra = TERRAIN_SELECTION_DISTANCE;
                Rectangle terrainSelectionRange = new Rectangle(terrain.polygon.getBoundingRectangle());
                terrainSelectionRange.height+= extra*2;
                terrainSelectionRange.width += extra*2;
                terrainSelectionRange.x -= extra;
                terrainSelectionRange.y -= extra;
                if (!terrainSelectionRange.contains(mouseLocation)) continue;

                float[] verts = terrain.polygon.getTransformedVertices();

                boolean selectedTerrainYet = false;
                //First, check if it's really close to a specific point
                for (int i = 0; i < verts.length/2; i++) {
                    //Coordinates of point
                    float ix = verts[i*2], iy = verts[i*2+1];
                    //Check if mouse is close to those coordinates
                    Vector2 toPoint = new Vector2(ix, iy).sub(mouseLocation);
                    if (toPoint.len() < extra) {
                        //Add the entity to possible selection
                        selectedEntities.add(terrain);
                        //If map is given, record details about the selection
                        if (selectionDetails != null) {
                            SelectionDetail details = new SelectionDetail(SelectionDetail.TERRAIN_VERTEX, new IntArray(new int[]{i}));
                            selectionDetails.put(terrain, details);
                        }
                        //Note that we've found a terrain
                        selectedTerrainYet = true;
                        break;
                    }
                }
                if (selectedTerrainYet) continue; //If a specific vertex was found, continue

                //Then, check if it's close to any of the edges
                for (int i = 0; i < verts.length/2; i++) {
                    //Points of the edge
                    float ix = verts[i*2], iy = verts[i*2+1];
                    int j = i+1;
                    if (j >= verts.length/2) j = 0;
                    float jx = verts[j*2], jy = verts[j*2+1];
                    //Check if mouse is close to that edge
                    float toEdge = Intersector.distanceSegmentPoint(ix, iy, jx, jy, mouseLocation.x, mouseLocation.y);
                    if (toEdge < extra) {
                        //Add the entity to possible selection
                        selectedEntities.add(terrain);
                        //If map is given, record details about the selection
                        if (selectionDetails != null) {
                            SelectionDetail details = new SelectionDetail(SelectionDetail.TERRAIN_VERTEX, new IntArray(new int[]{i, j}));
                            selectionDetails.put(terrain, details);
                        }
                        //Note that we've found a terrain
                        selectedTerrainYet = true;
                        break;
                    }
                }
                if (selectedTerrainYet) continue;//If a specific edge was found, continue

                //If not selecting a specific point or edge, check if it's just generally hovering over the terrain
                if (Intersector.isPointInPolygon(verts, 0, verts.length, mouseLocation.x, mouseLocation.y)) {
                    //Consider the entity selectable
                    selectedEntities.add(terrain);
                    //Record details about it
                    if (selectionDetails != null ){
                        IntArray allVerts = new IntArray(verts.length/2);
                        for (int i = 0; i < verts.length/2; i++) allVerts.add(i);

                        SelectionDetail details = new SelectionDetail(SelectionDetail.TERRAIN_BASE, allVerts);
                        selectionDetails.put(terrain, details);
                    }
                    continue;
                }
            }
        }

        return selectedEntities;
    }

    /**Analogous to {@link #canBeSelected(Vector2, ArrayList, ArrayList, HashMap)}, but by overlap with a polygonal selection region.
     * Some priorities are different, e.g. all TerrainElements have the same priority.*/
    private static ArrayList<Entity> canBeSelectedByPolygon(Polygon selectionBounds, ArrayList<Entity> entities, ArrayList<Entity> selectedEntities, HashMap<Entity, SelectionDetail> selectionDetails) {
        selectedEntities.clear();

        // Rectangle to use to screen entities that aren't even close
        Rectangle selectionScreen = selectionBounds.getBoundingRectangle();
        float tol = MiscFunctions.DEFAULT_MERGE_TOLERANCE;
        selectionScreen.x -= tol; selectionScreen.y -= tol;
        selectionScreen.width += 2*tol; selectionScreen.height += 2*tol;

        // Iterate through each entity, and check if it would be selected by the given polygon
        for (Entity entity : entities) {
            // For terrain elements, the polygons should overlap, and the points contained in the selection are details
            if (entity instanceof StaticTerrainElement terrain) {
                // Quickly check if these polygons could reasonably overlap
                if (!terrain.polygon.getBoundingRectangle().overlaps(selectionScreen)) continue;
                // Check if the terrain polygon and selection intersect
                if (Intersector.intersectPolygons(terrain.polygon, selectionBounds, null)
                    || Intersector.intersectPolygonEdges(new FloatArray(terrain.polygon.getTransformedVertices()), new FloatArray(selectionBounds.getTransformedVertices()))) { // TODO: Should we store and use the overlap?
                    //Add the terrain to selection
                    selectedEntities.add(terrain);
                    //If no details are required, then stop here
                    if (selectionDetails == null) continue;
                    //Then, identify which points on the polygon, if any, fall in the selection
                    float[] verts = terrain.polygon.getTransformedVertices();
                    IntArray selectedVerts = new IntArray();
                    for (int i = 0; i < verts.length/2; i++) {
                        float x = verts[i*2], y = verts[i*2+1];
                        //If this point falls inside the selection, add the index to details
                        if (Intersector.isPointInPolygon(verts, 0, verts.length, x, y)) {
                            selectedVerts.add(i);
                        }
                    }
                }
            }
        }

        return selectedEntities;
    }
}

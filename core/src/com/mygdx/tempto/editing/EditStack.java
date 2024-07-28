package com.mygdx.tempto.editing;

import com.mygdx.tempto.maps.WorldMap;

import java.util.ArrayList;

/**A set of edits to the map that can be undone and redone*/
public class EditStack {

    private WorldMap map;
    /**The list of edits themselves*/
    private ArrayList<MapEdit> edits;
    /**What edit the timeline is currently on. This index of {@link #edits} has been performed, and would be the next to be undone. If there aren't any future actions to redo, this would be the last index of the list.*/
    private int editIndex;

    /**Creates a stack of edits corresponding to the given {@link WorldMap}*/
    public EditStack(WorldMap map) {
        this.map = map;
        this.edits = new ArrayList<>();
        this.editIndex = -1; //No edits have been performed yet
    }

    /**Adds a new action after the current position in the stack, discarding all future edits.
     * This edit is performed using {@link MapEdit#redo(WorldMap)} upon the call of this method.*/
    public void addEdit(MapEdit nextEdit) {
        //Remove all future edits
        if (this.edits.size() > this.editIndex+1) { //If there are any edits after the last edit in the timeline, remove them
            this.edits.subList(this.editIndex+1, this.edits.size()).clear();
        }
        //Add this next edit
        this.edits.add(nextEdit);
        this.redo(); //Uses redo because this behaviour should be identical; pushing the index forward one and performing the edit
    }

    /**Undoes the last edit, thus moving another index back in the stack.*/
    public void undo() {
        if (this.editIndex < 0) return; //If there aren't any edits left to undo, stop there
        this.edits.get(this.editIndex).undo(this.map); //Undo the last edit
        this.editIndex--; //And move back to the previous edit
    }

    /**Redoes the next edit, if present, and moves another index forward in the stack.*/
    public void redo() {
        if (this.editIndex + 1 >= this.edits.size()) return; //If already at the last edit, can't go into the future
        this.editIndex++; //Move forward an edit
        this.edits.get(this.editIndex).redo(this.map); //Perform that next edit
    }

    public WorldMap getMap() {
        return map;
    }
}

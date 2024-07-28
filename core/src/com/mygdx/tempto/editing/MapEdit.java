package com.mygdx.tempto.editing;

import com.mygdx.tempto.maps.WorldMap;

/**An edit performed on a map file, which can both be undone and redone.*/
public abstract class MapEdit {
    /**Whether or not the action has been performed (i.e. you would go back in time and undo it) or false, if it 'hasn't been done yet' (you undid it, but haven't made any new changes yet so you could still redo)*/
    private boolean done;

    /**Undoes the given edit to the map, if it has been done.
     * Subclasses should prioritize implementing {@link #undoEdit(WorldMap)}
     * */
    public void undo(WorldMap map) {
        if (this.done) {
            this.undoEdit(map);
            this.done = false;
        }
    }

    /**The method by which implementing subclasses undo the edits they've made.
     * Should be a consistent reverse of {@link #redoEdit(WorldMap)}*/
    public abstract void undoEdit(WorldMap map);

    /**Redoes the given edit to the map, if it has been done.
     * Subclasses should prioritize implementing {@link #redoEdit(WorldMap)}
     * */
    public void redo(WorldMap map) {
        if (!this.done) {
            this.redoEdit(map);
            this.done = true;
        }
    }

    /**The medthod by which implementing subclasses redo the edits they've made.
     * Should be a consistent reverse of {@link #undoEdit(WorldMap)}*/
    public abstract void redoEdit(WorldMap map);
}

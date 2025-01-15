package com.mygdx.tempto.editing;

import com.mygdx.tempto.maps.WorldMap;

import java.util.ArrayList;

/**Performs a combination of separate {@link MapEdit} instances as if they were one combined edit.
 * Edits are performed in the order given, and undone in the reverse of the order given.*/
public class ComboMapEdit extends MapEdit{

    ArrayList<MapEdit> subEdits;

    public ComboMapEdit(ArrayList<MapEdit> subEdits) {
        this.subEdits = subEdits;
    }

    public ArrayList<MapEdit> getSubEdits() {
        return subEdits;
    }

    @Override
    public void undoEdit(WorldMap map) {
        for (int i = this.subEdits.size()-1; i >= 0; i--) {
            MapEdit subEdit = this.subEdits.get(i);
            subEdit.undo(map);
        }
    }

    @Override
    public void redoEdit(WorldMap map) {
        for (int i = 0; i < this.subEdits.size(); i++) {
            MapEdit subEdit = this.subEdits.get(i);
            subEdit.redo(map);
        }
    }
}

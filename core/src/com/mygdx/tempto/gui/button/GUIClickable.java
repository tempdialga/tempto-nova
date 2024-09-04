package com.mygdx.tempto.gui.button;

import com.badlogic.gdx.math.Vector2;

public interface GUIClickable {
    /**Returns whether the given item can be clicked by a mouse in the given gui coordinates (i.e. on the intervals [-1,1], [-1,1] representing the screen from bottom to top and left to right*/
    boolean canClickScreen(Vector2 guiCoords);
}

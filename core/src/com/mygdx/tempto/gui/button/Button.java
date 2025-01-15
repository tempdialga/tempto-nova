package com.mygdx.tempto.gui.button;

import com.badlogic.gdx.math.Vector2;

/**A class representing an individual GUI element*/
public interface Button {
    /**Returns whether or not the button can be pressed with the given mouse location.
     * If the button requires it, this should also be how it stores the mouse location for later use.
     * Should only be called when the mouse actively moves.*/
    boolean canBePressed(Vector2 mouseLoc);

    /**Returns the action associated with this button*/
    ButtonAction getAction();
}

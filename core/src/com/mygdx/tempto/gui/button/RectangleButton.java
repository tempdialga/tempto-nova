package com.mygdx.tempto.gui.button;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**A rectangular button, with location and size defined in GUI coordinates ([-1,1],[-1,1] bottom to top, left to right). Clickable by a mouse  */
public class RectangleButton implements GUIClickable{
    public Rectangle buttonArea;

    public RectangleButton(Rectangle buttonArea) {
        this.buttonArea = buttonArea;
    }

    @Override
    public boolean canClickScreen(Vector2 guiCoords) {
        return buttonArea.contains(guiCoords);
    }
}

package com.mygdx.tempto.gui.button;

import com.badlogic.gdx.math.Rectangle;

public class RectangleTextButton extends RectangleButton {

    public String buttonText;

    public RectangleTextButton(Rectangle buttonArea, String text) {
        super(buttonArea);
        this.buttonText = text;
    }
    public RectangleTextButton(Rectangle buttonArea) {
        this(buttonArea, "none");
    }
}

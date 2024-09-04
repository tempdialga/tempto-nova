package com.mygdx.tempto.gui.button;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

/**A button which is composed of multiple buttons which are always visible.*/
public class MultiButton implements Button {

    public int currentButtonIndex;
    public ArrayList<Button> buttons;

    public MultiButton(ArrayList<Button> buttons) {
        this.buttons = buttons;
    }

    @Override
    public boolean canBePressed(Vector2 mouseLoc) {
        return false;
    }

    @Override
    public ButtonAction getAction() {
        return this.buttons.get(this.currentButtonIndex).getAction();
    }
}

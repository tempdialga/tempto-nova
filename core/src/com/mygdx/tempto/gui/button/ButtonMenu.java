package com.mygdx.tempto.gui.button;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import org.eclipse.collections.impl.bimap.mutable.HashBiMap;

import java.util.ArrayList;

/**A menu consisting of a set of buttons which can be clicked on. Prioritizes clicking buttons starting by lower index, and only clicks one button at a time.*/
public class ButtonMenu extends InputAdapter {

//    public ArrayList<ButtonAction> actions;
//    public HashBiMap<ButtonAction, >
//    public MultiButton mainButtons
    /**The buttons of the menu.*/
    public ArrayList<GUIClickable> buttons;
    /**The button actions corresponding to each button, where defined. If a button is clicked without a corresponding action in this map, nothing happens.*/
    public HashBiMap<GUIClickable, ButtonAction> buttonActionMap;
    /**The camera to GUI coordinates ([-1,1],[-1,1] bottom to top, left to right)*/
    public OrthographicCamera screenCamera;
    /**The aspect ratio to use for the screen*/
    public float aspectratio;
    /**The button currently selected by the mouse*/
    public GUIClickable selectedButton;

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Clicked: Reassess if the selected button is under the mouse. Whatever was selected before is mostly irrelevant.

        // Obtain coordinates in GUI space
        Vector3 guiCoords3D = this.screenCamera.unproject(new Vector3(screenX, screenY, 0));
        Vector2 guiCoords = new Vector2(guiCoords3D.x, guiCoords3D.y);
        // If the last button couldn't actually be clicked, quickly try to select a new one
        if (this.selectedButton == null || !this.selectedButton.canClickScreen(guiCoords)) {
            this.selectedButton = null; //Don't save which was clicked before
            this.mouseMoved(guiCoords); //Look for something that can be clicked, and if nothing can be clicked, it'll be null
        }
        // If a button can be selected, activate its corresponding action
        if (this.selectedButton != null) {
            if (this.buttonActionMap.containsKey(this.selectedButton)) {
                ButtonAction action = this.buttonActionMap.get(this.selectedButton);
                action.act();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        // Obtain the coordinates in GUI space
        Vector3 guiCoords3D = this.screenCamera.unproject(new Vector3(screenX, screenY, 0));
        Vector2 guiCoords = new Vector2(guiCoords3D.x, guiCoords3D.y);
        return this.mouseMoved(guiCoords);
    }

    private boolean mouseMoved(Vector2 guiCoords) {
        for (GUIClickable buttonElement : this.buttons) {
            if (buttonElement.canClickScreen(guiCoords)) {
                this.selectedButton = buttonElement;
                return true;
            }
        }
        return false;
    }
}

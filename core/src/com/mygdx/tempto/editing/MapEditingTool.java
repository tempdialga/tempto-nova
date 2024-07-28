package com.mygdx.tempto.editing;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.mygdx.tempto.rendering.RendersToScreen;
import com.mygdx.tempto.rendering.RendersToWorld;

/**An interface for a tool to edit Tempto maps*/
public abstract class MapEditingTool implements RendersToWorld, RendersToScreen {

    /**The stack of edits to push to*/
    public EditStack editStack;
    public String name;

    /**Gives the tool access to the worldmap to edit*/
    public void setEditStack(EditStack edits) {
        this.editStack = edits;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract void touchDown(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam);

    public abstract void touchUp(int screenX, int screenY, int pointer, int button, OrthographicCamera worldCam);

    public abstract void buttonDown(int gameInput);

    public abstract void buttonUp(int gameInput);

    public abstract void touchDragged(int screenX, int screenY, int pointer, OrthographicCamera worldCam);

    public abstract void mouseMoved(int screenX, int screenY, OrthographicCamera worldCam);
}

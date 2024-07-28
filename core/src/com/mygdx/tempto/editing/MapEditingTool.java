package com.mygdx.tempto.editing;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.mygdx.tempto.rendering.RendersToScreen;
import com.mygdx.tempto.rendering.RendersToWorld;

import java.util.HashMap;

/**An interface for a tool to edit Tempto maps*/
public abstract class MapEditingTool implements RendersToWorld, RendersToScreen {

    /**The UI editing the map that's using these tools*/
    private MapEditor parent;
    /**The stack of edits to push to*/
    public EditStack editStack;
    public String name;
    /**A hashmap of context shared between tools, which contains context from the last tool.*/
    public HashMap<String, Object> toolContext;

    /**Gives the tool access to the worldmap to edit*/
    public void setEditStack(EditStack edits) {
        this.editStack = edits;
    }

    /**Gives the tool access to its parent editor*/
    public void setParent(MapEditor parent) {
        this.parent = parent;
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

    public void switchToTool(MapEditingTool nextTool) {
        this.toolContext.put("lastTool", this.getName());
        nextTool.toolContext = this.toolContext;
        nextTool.editStack = this.editStack;
        nextTool.setParent(this.parent);
        nextTool.activate();
        // Clear the context so that when this tool switches to a new one, only the context this tool adds will be present
        this.toolContext.clear();
        // Tell the parent to switch to using this next tool
        this.parent.setCurrentTool(nextTool);
    }

    /**Called when the tool is switched to. The previous tool should have updated the context with any information necessary.*/
    public abstract void activate();
}

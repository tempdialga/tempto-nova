package com.mygdx.tempto.editing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.rendering.RendersToScreen;
import com.mygdx.tempto.rendering.RendersToWorld;

import java.util.HashMap;

/**A class to edit {@link WorldMap} instances; Consumes all input for the world, takes {@link com.mygdx.tempto.input.InputTranslator.GameInputs}*/
public class MapEditor implements InputProcessor, RendersToScreen, RendersToWorld {

    public boolean active;
    private MapEditingTool currentTool;
    private HashMap<String, Object> toolContext;

    private WorldMap mapToEdit;

    /**The list of previous and, if applicable, future edits*/
    private EditStack editStack;

    //Display

    private BitmapFont defaultFont = new BitmapFont();

    public MapEditor(WorldMap mapToEdit) {
        // Start with the idle tool
        this.currentTool = Tools.SELECT_VERTICES.getInstance();
        // Start the context
        this.toolContext = new HashMap<>();
        this.toolContext.put("lastTool", "NONE");
        this.currentTool.toolContext = this.toolContext;
        this.currentTool.setParent(this);
        this.currentTool.activate();
        this.mapToEdit = mapToEdit;
        this.editStack = new EditStack(this.mapToEdit);
        this.currentTool.setEditStack(this.editStack);
    }

    public void update(float deltaTime) {

    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == InputTranslator.GameInputs.TOGGLE_EDITOR && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            this.active = !this.active; //If asked to toggle the editor, change whether it's active or not
        }
        if (!this.active) return false;


        if (keycode == InputTranslator.GameInputs.UNDO && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) { // If wanting to undo or redo
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) { // Redo if shift is pressed
                this.editStack.redo();
            } else { // Undo
                this.editStack.undo();
            }
        } else {
            this.currentTool.buttonDown(keycode);
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (!this.active) return false;
        this.currentTool.buttonUp(keycode);
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        if (!this.active) return false;
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!this.active) return false;
        this.currentTool.touchDown(screenX, screenY, pointer, button, this.mapToEdit.getCamera());
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!this.active) return false;
        this.currentTool.touchUp(screenX, screenY, pointer, button, this.mapToEdit.getCamera());
        return true;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        if (!this.active) return false;
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!this.active) return false;
        this.currentTool.touchDragged(screenX, screenY, pointer, this.mapToEdit.getCamera());
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (!this.active) return false;
        this.currentTool.mouseMoved(screenX, screenY, this.mapToEdit.getCamera());
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (!this.active) return false;
        return true;
    }

    @Override
    public void renderToScreen(SpriteBatch batch, OrthographicCamera screenCamera, float aspectRatio) {
        if (!this.active) return; //Only render if active
        //System.out.println("Rendering editor!");
        this.currentTool.renderToScreen(batch, screenCamera, aspectRatio);
        String display = "EDITING: " + this.currentTool.getName();

        float textHeight = 0.15f;
        this.defaultFont.setColor(Color.WHITE);
        this.defaultFont.setUseIntegerPositions(false);
        this.defaultFont.getData().setScale(textHeight * this.defaultFont.getScaleY() / this.defaultFont.getLineHeight());
        this.defaultFont.draw(batch, display,-1*aspectRatio, -1 + textHeight);
    }

    @Override
    public void renderToWorld(SpriteBatch batch, OrthographicCamera worldCamera) {
        if (!this.active) return; //Only render if active
        this.currentTool.renderToWorld(batch, worldCamera);
    }

    public MapEditingTool getCurrentTool() {
        return currentTool;
    }

    public void setCurrentTool(MapEditingTool currentTool) {
        this.currentTool = currentTool;
    }
}

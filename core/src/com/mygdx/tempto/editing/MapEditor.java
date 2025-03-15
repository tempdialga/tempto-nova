package com.mygdx.tempto.editing;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.mygdx.tempto.gui.button.ButtonAction;
import com.mygdx.tempto.gui.button.ButtonMenu;
import com.mygdx.tempto.gui.button.GUIClickable;
import com.mygdx.tempto.gui.button.RectangleButton;
import com.mygdx.tempto.gui.button.RectangleTextButton;
import com.mygdx.tempto.gui.text.TextUtils;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.rendering.RendersToScreen;
import com.mygdx.tempto.rendering.RendersToWorld;
import com.mygdx.tempto.view.GameScreen;

import org.eclipse.collections.impl.bimap.mutable.HashBiMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**A class to edit {@link WorldMap} instances; Consumes all input for the world, takes {@link com.mygdx.tempto.input.InputTranslator.GameInputs}*/
public class MapEditor implements InputProcessor, RendersToScreen, RendersToWorld {

    public boolean active;
    private MapEditingTool currentTool;
    private HashMap<String, Object> toolContext;

    private WorldMap mapToEdit;

    /**The list of previous and, if applicable, future edits*/
    private EditStack editStack;

    /**The GUI of the editor*/
    private MainEditorGUI editorGUI;

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

        this.editorGUI = new MainEditorGUI();
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
        } else if (keycode == InputTranslator.GameInputs.POSE && this.currentTool != Tools.EDIT_POSE.getInstance()) {
            this.currentTool.switchToTool(Tools.EDIT_POSE.toolInstance);
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
        //If not active, stop
        if (!this.active) return false;
        //If the gui can use this input, start with that
        if (this.editorGUI.touchDown(screenX, screenY, pointer, button)) return true;
        //Else, check for the tool's action
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
        //If not active, don't take the input
        if (!this.active) return false;
        //Check if the GUI wants this input, and if so don't give it to the tool
        if (this.editorGUI.mouseMoved(screenX, screenY)) return true;
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

        //Render the GUI
        this.editorGUI.renderToScreen(batch, screenCamera, aspectRatio);
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

    /**The overarching GUI/interface of the map editor. Determines general cases of switching to tools, or exiting editor mode.*/
    private class MainEditorGUI extends InputAdapter implements RendersToScreen{


        /**The set of buttons at the top of the screen*/
        private class EditorTaskBar extends ButtonMenu implements RendersToScreen {

            // // // Rendering Constants // // //

            /**Constants dictating how far from the sides and top the taskbar at the top should be*/
            private static final float SIDE_PADDING = 0.2f, TOP_PADDING = 0.1f;
            /**How tall the taskbar should be, i.e. how far to stretch down from the top after padding*/
            private static final float BAR_HEIGHT = 0.2f;
            /**How tall the text should be, proportionally to the height of the bar*/
            private static final float TEXT_HEIGHT_IN_BAR = 1.0f;
            /**Default color for a button*/
            private static final Color DEFAULT_BUTTON_COLOR = new Color(Color.LIGHT_GRAY).mul(Color.CORAL);
            /**Color for a button that's selected*/
            private static final Color SELECTED_BUTTON_COLOR = new Color(Color.PINK).mul(Color.CORAL);
            /**Color for the text on the buttons*/
            private static final Color BUTTON_TEXT_COLOR = new Color(Color.WHITE);



            /**Creates a new GUI with the given camera*/
            public EditorTaskBar(OrthographicCamera screenCamera){
                // Instantiate collections
                this.buttons = new ArrayList<>();
                this.buttonActionMap = new HashBiMap<>();
                // Save the camera for later
                this.screenCamera = screenCamera;

                // Start by making buttons without coordinates

                // Button to leave the editor
                RectangleTextButton stopEditing = new RectangleTextButton(new Rectangle(), "Leave Editor");
                this.buttons.add(stopEditing);
                this.buttonActionMap.put(stopEditing, new ButtonAction() {
                    @Override
                    public void act() {
                        MapEditor.this.mapToEdit.toggleEditing();
                    }
                });

                // Button to save and quit
                RectangleTextButton saveQuit = new RectangleTextButton(new Rectangle(), "Save and Quit");
                this.buttons.add(saveQuit);
                this.buttonActionMap.put(saveQuit, new ButtonAction() {
                    @Override
                    public void act() {
                        MapEditor.this.mapToEdit.writeToSaveAndCoreFile();
                        Gdx.app.exit();
                    }
                });

                // Button to switch to vertex mode
                RectangleTextButton vertexMode = new RectangleTextButton(new Rectangle(), "Vertex Mode");
                this.buttons.add(vertexMode);
                this.buttonActionMap.put(vertexMode, new ButtonAction() {
                    @Override
                    public void act() {
                        //Switch to vertex editor if not already on it
                        MapEditingTool vertexEditor = Tools.SELECT_VERTICES.getInstance();
                        if (MapEditor.this.currentTool == null) MapEditor.this.setCurrentTool(vertexEditor);
                        if (MapEditor.this.currentTool != vertexEditor) MapEditor.this.currentTool.switchToTool(vertexEditor);
                    }
                });

                // // // Other instantiation // // //

                // Arrange the buttons on the top of the screen, with evenly spaced bars
                System.out.println("Camera view dims: " + screenCamera.viewportWidth + ", " + screenCamera.viewportHeight);
                float screenWidth = screenCamera.viewportWidth;
                float paddedWidth = screenWidth - 2*SIDE_PADDING, height = BAR_HEIGHT; //width of the screen after cutting out the padding
                float buttonWidth = paddedWidth / ((float) this.buttons.size());
                for (int i = 0; i < this.buttons.size(); i++) {
                    System.out.println("Rendering button at index " + i);
                    // Coordinates of the button
                    float x = SIDE_PADDING + i*buttonWidth - (screenWidth*0.5f);
                    float y = 1.0f - BAR_HEIGHT - TOP_PADDING;
                    // Set button dimensions
                    RectangleButton button = (RectangleButton) this.buttons.get(i);
                    button.buttonArea.set(x, y, buttonWidth, height);
                    System.out.println("Button dimensions: " + button.buttonArea);
                }
            }

            @Override
            public void renderToScreen(SpriteBatch batch, OrthographicCamera screenCamera, float aspectRatio) {
                // Font to use
                BitmapFont font = MapEditor.this.defaultFont;
                // Blank texture to use
                Texture blank = MapEditor.this.mapToEdit.blankTexture;

                // Render the buttons
                for (int i = 0; i < this.buttons.size(); i++) {
                    GUIClickable button = this.buttons.get(i);

                    //System.out.println("Rendering button at index " + i);
                    if (button instanceof RectangleTextButton rectButton) {
                        //Render backing
                        Rectangle buttonArea = rectButton.buttonArea;
                        //Set color based on selection
                        Color backingColor;
                        if (rectButton == this.selectedButton) {
                            backingColor = SELECTED_BUTTON_COLOR;
                        } else {
                            backingColor = DEFAULT_BUTTON_COLOR;
                        }
                        batch.setColor(backingColor);
                        batch.draw(blank, buttonArea.x, buttonArea.y, buttonArea.width, buttonArea.height);

                        //Set the font dimensions, and test how they actually render
                        float idealTextHeight = BAR_HEIGHT*TEXT_HEIGHT_IN_BAR; //The full size of the text
//                        font.getData().setScale(idealTextHeight * font.getScaleY() / font.getLineHeight());
//                        //Prepare to render really far offscreen to check how it'll actually look dimension wise
//                        GlyphLayout testRender = font.draw(batch, rectButton.buttonText, screenCamera.position.x+999999, screenCamera.position.y+999999);
//
//                        //Using those found dimensions, actually render the text
//                        float textHeight = testRender.height;
//                        float textVertPadding = 0.5f*(BAR_HEIGHT-textHeight);
//                        //System.out.println("Vertical text padding: " + textVertPadding + ", vs button and text height: " + BAR_HEIGHT + ", " + textHeight);
//                        //System.out.println("Text final height: " + testRender.height);
//                        float textLeftPadding = (buttonArea.width - testRender.width)*0.5f; //How far from the left of the button this text should be to be centered
//                        float textX = buttonArea.x + textLeftPadding;
//                        float textY = buttonArea.y + buttonArea.height - textVertPadding;


                        //Once dimensions and locations are set, render the text over the button
                        batch.setColor(Color.WHITE); //Reset batch color
                        font.setColor(BUTTON_TEXT_COLOR);
                        font.setUseIntegerPositions(false);

                        //Use TextUtils to render
                        TextUtils.renderTextCentered(buttonArea, font, rectButton.buttonText, idealTextHeight, batch, screenCamera);

                        //font.draw(batch, rectButton.buttonText, buttonArea.x, buttonArea.y);
                        //font.draw(batch, rectButton.buttonText, textX, textY);
                    }
                }
            }
        }
        /**The task bar of the editor gui*/
        private EditorTaskBar taskBar;




        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            return (this.taskBar != null) && this.taskBar.touchDown(screenX, screenY, pointer, button);
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            return (this.taskBar != null) && this.taskBar.mouseMoved(screenX, screenY);
        }

        @Override
        public void renderToScreen(SpriteBatch batch, OrthographicCamera screenCamera, float aspectRatio) {
            //Instantiate the taskbar here with the camera
            if (this.taskBar == null) this.taskBar = new EditorTaskBar(screenCamera);
            //Render the taskbar
            this.taskBar.renderToScreen(batch, screenCamera, aspectRatio);
        }
    }
}

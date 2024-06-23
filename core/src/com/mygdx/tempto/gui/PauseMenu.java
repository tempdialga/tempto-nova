package com.mygdx.tempto.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.mygdx.tempto.input.InputTranslator;
import static com.mygdx.tempto.input.InputTranslator.GameInputs;
import com.mygdx.tempto.input.KeyboardMapper;
import com.mygdx.tempto.rendering.RendersToScreen;

public class PauseMenu implements InputProcessor, RendersToScreen {

    //////// Data //////////////////////////////
    private static Texture temporaryButtonImg = new Texture("badlogic.jpg");
    private static Sprite tempButtonSprite = new Sprite(temporaryButtonImg);
    private int selectedButton;
    public static final int RETURN = 0;
    public static final int QUIT_GAME = 1;

    //////// Control Flow //////////////////////
    /**Whether the pause menu is currently being shown to the screen. If true, blocks all input to anything behind it. (i.e. doesn't let the player get input to move)*/
    private boolean visible;

    /**Shows the menu; until {@link #hide()} is called to hide it, the menu will then block input from going past it, and render to the screen. */
    private void show() {
            // When the menu is shown, the default option should be returning //
        this.selectedButton = RETURN;
            // When updating, rendering, and handling input, the menu now knows it is active //
        this.visible = true;
    }

    /**Hides the menu; until {@link #show()} is called, the menu will only handle input for the pause button, and it won't render to the screen.*/
    private void hide() {this.visible = false;}
    private boolean isVisible() {return this.visible;}

    //////// Constructors //////////////////////
    /**Creates a new pause menu, which starts hidden and with the return button selected.*/
    public PauseMenu() {
        this.selectedButton = RETURN;
        this.hide();
    }

    //////// Interface Methods /////////////////


    @Override
    public void renderToScreen(SpriteBatch batch, OrthographicCamera screenCamera, float aspectRatio) {
        // Handle menu visibility //

            // If not visible, don't render anything //
        if (!this.visible) return;

        // Partially hide world/background //

            // Uses a screen clear with a transparent color to still show the game, just with less emphasis. Note: Do we want to blur? //
        Color overlayColor = new Color(0,0,0,0.02f);
        ScreenUtils.clear(overlayColor);

        // Debug: test screen coordinate usage //

            // Set a sprite to have transparency and fill up what *should* be the screen coordinates
        tempButtonSprite.setBounds(-1,-1, 2, 2);
        tempButtonSprite.setAlpha(0.2f);
        tempButtonSprite.draw(batch);

        // Render menu buttons //

            // Positions of each button: These are some temporary, absolute coordinates to make sure the actual logic works TODO: Design, and make an efficient method to design, menu layouts // .
            // These are screen coordinates on the intervals ([-a,a], [-1,1]) where (-a,-1) is the bottom left corner
        float leftX = -0.5f*aspectRatio;
        float returnX = leftX, returnY = 0.5f, returnWidth = 0.75f, returnHeight = 0.25f;
        float quitX = leftX, quitY = 0f, quitWidth = 0.75f, quitHeight = 0.25f;
            //Adds a little indent to whichever of the two is selected
        float indent = 0.04f;
        if (this.selectedButton == RETURN) returnX += indent; else quitX += indent;

            //Draw the huttons at their respective positions //
        batch.draw(temporaryButtonImg, returnX, returnY, returnWidth, returnHeight); //Return button
        batch.draw(temporaryButtonImg, quitX, quitY, quitWidth, quitHeight); //Quit button

    }

    @Override
    public boolean keyDown(int keycode) {
        if (this.visible) { // If the pause menu is currently active:

            switch (keycode) { // Currently hardcoded TODO: integrate with TemptoInput once that class is complete
                case GameInputs.PAUSE -> this.hide(); // Toggle the menu back to being hidden
                case GameInputs.UP -> this.selectedButton = RETURN; // Move to the top button
                case GameInputs.DOWN -> this.selectedButton = QUIT_GAME; // Move to the bottom button

                case GameInputs.CONFIRM -> { // Input to select a button

                    if (this.selectedButton == RETURN) {
                        this.hide(); // Selected return button -> return to game, close menu
                    } else if (this.selectedButton == QUIT_GAME) {
                        Gdx.app.exit(); // Selected quit button -> quit the game, :)))) this is a terrible way to quit the game I think
                    }

                }
            }

        } else { // If the Pause Menu is currently inactive, the only input that it needs to look for is input to activate it
            if (keycode == GameInputs.PAUSE) {
                this.show();
            }
        }
        return this.visible;
    }

    @Override
    public boolean keyUp(int keycode) {
        return this.visible;
    }

    @Override
    public boolean keyTyped(char character) {
        return this.visible;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return this.visible;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return this.visible;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return this.visible;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return this.visible;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return this.visible;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return this.visible;
    }
}

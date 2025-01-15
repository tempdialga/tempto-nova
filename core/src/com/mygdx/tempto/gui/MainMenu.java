package com.mygdx.tempto.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.mygdx.tempto.TemptoNova;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.rendering.RendersToScreen;

public class MainMenu implements InputProcessor, RendersToScreen {

    //////// Data //////////////////////////////
    private static Texture temporaryButtonImg = new Texture("badlogic.jpg");
    private static Sprite tempButtonSprite = new Sprite(temporaryButtonImg);
    private int selectedButton;
    public static final int PLAY = 0;
    public static final int EDIT = 1;
    public static final int QUIT = 2;

    //////// Control flow //////////////////////
    private TemptoNova overallGame;

    public MainMenu(TemptoNova overallGame) {
        this.overallGame = overallGame;
        this.selectedButton = PLAY;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == InputTranslator.GameInputs.DOWN) {
            this.selectedButton += 1;
            if (this.selectedButton > 2) this.selectedButton = 2;
        } else if (keycode == InputTranslator.GameInputs.UP) {
            this.selectedButton -= 1;
            if (this.selectedButton < 0) this.selectedButton = 0;
        } else if (keycode == InputTranslator.GameInputs.CONFIRM) {
            switch (this.selectedButton) {
                case PLAY -> overallGame.switchScreen(TemptoNova.GAME_SCREEN);
                case EDIT -> overallGame.switchScreen(TemptoNova.EDIT_SCREEN);
                case QUIT -> Gdx.app.exit();
            }
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public void renderToScreen(SpriteBatch batch, OrthographicCamera screenCamera, float aspectRatio) {
        aspectRatio = 1;
        // Debug: test screen coordinate usage //

        ScreenUtils.clear(0.1f,0.1f,0.1f,1);

        // Set a sprite to have transparency and fill up what *should* be the screen coordinates
        tempButtonSprite.setBounds(-1,-1, 2, 2);
        tempButtonSprite.setAlpha(0.2f);
        tempButtonSprite.draw(batch);

        // Render menu buttons //

        // Positions of each button: These are some temporary, absolute coordinates to make sure the actual logic works TODO: Design, and make an efficient method to design, menu layouts // .
        // These are screen coordinates on the intervals ([-a,a], [-1,1]) where (-a,-1) is the bottom left corner
        float leftX = -0.5f*aspectRatio;
        float playX = leftX, playY = 0.5f, playWidth = 0.75f, playHeight = 0.25f;
        float editX = leftX, editY = 0f, editWidth = 0.75f, editHeight = 0.25f;
        float quitX = leftX, quitY = -0.5f, quitWidth = 0.75f, quitHeight = 0.25f;
        //Adds a little indent to whichever of the two is selected
        float indent = 0.04f;
        switch (this.selectedButton) {
            case PLAY -> playX += indent;
            case EDIT -> editX += indent;
            case QUIT -> quitX += indent;
        }
        //Draw the huttons at their respective positions //
        batch.draw(temporaryButtonImg, playX, playY, playWidth, playHeight); //Play button
        batch.draw(temporaryButtonImg, editX, editY, editWidth, editHeight); //Edit button
        batch.draw(temporaryButtonImg, quitX, quitY, quitWidth, quitHeight); //Quit button

    }
}

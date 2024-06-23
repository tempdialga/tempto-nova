package com.mygdx.tempto.input;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;

/**A class to translate various inputs from keyboard, controller, and touchscreen into abstract, specific game input.
 * For example, this class takes whatever input, W, up, controller stick up.
 *
 * The class operates analogously to {@link InputMultiplexer}, however, give inputs in terms of hard-coded, abstract game inputs like "up", "down", "confirm" "cancel" instead of the actual keycode.
 * With one major exception: {@link #keyTyped(char)} does not translate input, as this is a method specifically intended, and only really used for, text input
 * */
public class InputTranslator extends InputMultiplexer implements ControllerListener {

    //////// Abstracted input codes; These will be the ones submitted to things in the game ////////
    /**The codes that represent generic game inputs, to be used by core, platform and binding independent, game code.*/
    public static final class GameInputs {
        /**Integer codes representing abstract game inputs, rather than specific keyboard or button bindings. Occupy negative values to minimize accidental misinterpretation*/
        public static final int
                //The game input given when some kind of input happens but it couldn't be mapped to a normal game input
                OTHER = -2,

                PAUSE = -3,

                CANCEL = -4, CONFIRM = -5,

                UP = -6, DOWN = -7, LEFT = -8, RIGHT = -9,

                JUMP = -10,

                DEBUG_SAVE = -11;
    }
    @Override
    public boolean keyDown(int keycode) {
        // Translate keyboard input to game input
        int gameInput = KeyboardMapper.gameInputOf(keycode);
        return super.keyDown(gameInput);
    }

    @Override
    public boolean keyUp(int keycode) {
        // Translate keyboard input to game input
        int gameInput = KeyboardMapper.gameInputOf(keycode);
        return super.keyUp(gameInput);
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
    public void connected(Controller controller) {

    }

    @Override
    public void disconnected(Controller controller) {

    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        return false;
    }
}

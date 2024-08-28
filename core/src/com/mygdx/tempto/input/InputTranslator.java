package com.mygdx.tempto.input;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntSet;

/**A class to translate various inputs from keyboard, controller, and touchscreen into abstract, specific game input.
 * For example, this class takes whatever input, W, up, controller stick up.
 *
 * The class operates analogously to {@link InputMultiplexer}, however, give inputs in terms of hard-coded, abstract game inputs like "up", "down", "confirm" "cancel" instead of the actual keycode.
 * With one major exception: {@link #keyTyped(char)} does not translate input, as this is a method specifically intended, and only really used for, text input
 * */
public class InputTranslator extends InputMultiplexer implements ControllerListener {

    //////// Abstracted input codes; These will be the ones submitted to things in the game ////////
    /**A class representing generic game inputs, to be used by core, platform and binding independent, game code.
     * Contains integer codes representing discrete inputs, as well as a few abstract but commonly used data to poll, such as input direction.*/
    public static final class GameInputs {
        /**Integer codes representing abstract game inputs, rather than specific keyboard or button bindings. Occupy negative values to minimize accidental misinterpretation*/
        public static final int
                //The game input given when some kind of input happens but it couldn't be mapped to a normal game input
                OTHER = -2,

                PAUSE = -3,

                CANCEL = -4, CONFIRM = -5,

                UP = -6, DOWN = -7, LEFT = -8, RIGHT = -9,

                JUMP = -10,

                DEBUG_SAVE = -11,

                NEW_ITEM = -12,

                TOGGLE_EDITOR = -13,

                UNDO = -14,

                DELETE = -15,

                BRUSH = -16,

                VERTEX = -17;

        /**A set of integers containing all inputs known to be active at the moment.
         * An input is added to this set when given through {@link #keyDown(int)}, and removed through {@link #keyUp(int)}*/
        public static final IntSet activeInputs = new IntSet();

        /**Returns whether the given game input is currently active, i.e. has been found using {@link #keyDown(int)} and not yet released using {@link #keyUp(int)}
         * @param gameInput The game input, defined by {@link GameInputs}, to poll for*/
        public static boolean inputActive(int gameInput) {
            return activeInputs.contains(gameInput);
        }

        /**A global {@link Vector2} representing the current direction of input, which can be updated using {@link #updateProcessedInput()}*/
        public static Vector2 inputDirection = new Vector2(0,0);

        /**Updates secondary/processed input data like {@link #inputDirection} using the information submitted to {@link #activeInputs}*/
        public static void updateProcessedInput() {
            // Update input direction //

            inputDirection.y = 0; //Start without assuming any vertical direction,
            if (inputActive(GameInputs.UP)) inputDirection.y = 1; //And prioritize upward input
            else if (inputActive(GameInputs.DOWN)) inputDirection.y = -1;

            inputDirection.x = 0; //Start without assuming horizontal direction,
            if (inputActive(GameInputs.RIGHT)) inputDirection.x = 1; //And prioritize rightward movement? Do we want to make left and right cancel out
            else if (inputActive(GameInputs.LEFT)) inputDirection.x = -1;


        }
    }
    @Override
    public boolean keyDown(int keycode) {
        // Translate keyboard input to game input
        int gameInput = KeyboardMapper.gameInputOf(keycode);
        GameInputs.activeInputs.add(gameInput);
        return super.keyDown(gameInput);
    }

    @Override
    public boolean keyUp(int keycode) {
        // Translate keyboard input to game input
        int gameInput = KeyboardMapper.gameInputOf(keycode);
        GameInputs.activeInputs.remove(gameInput);
        return super.keyUp(gameInput);
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return super.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return super.touchUp(screenX, screenY, pointer, button);
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
        System.out.println("New controller connected named " + controller.getName());
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

    //TODO: Fix axis moving to be more generalized, after figuring out what controller mapping should look like
    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        //System.out.println("Axis " + axisCode + " moved to " + value);
        float threshold = 0.2f;
        if (axisCode == 0) { //Left stick l-r
            if (value < -threshold) { //go left
                GameInputs.activeInputs.add(GameInputs.LEFT);
                GameInputs.activeInputs.remove(GameInputs.RIGHT);
            } else if (value > threshold) { //go right
                GameInputs.activeInputs.remove(GameInputs.LEFT);
                GameInputs.activeInputs.add(GameInputs.RIGHT);
            } else { //none
                GameInputs.activeInputs.remove(GameInputs.LEFT);
                GameInputs.activeInputs.remove(GameInputs.RIGHT);
            }
        } else if (axisCode == 1) { //Left stick l-r
            if (value < -threshold) { //go up
                GameInputs.activeInputs.add(GameInputs.UP);
                GameInputs.activeInputs.remove(GameInputs.DOWN);
            } else if (value > threshold) { //go down
                GameInputs.activeInputs.remove(GameInputs.UP);
                GameInputs.activeInputs.add(GameInputs.DOWN);
            } else { //none
                GameInputs.activeInputs.remove(GameInputs.UP);
                GameInputs.activeInputs.remove(GameInputs.DOWN);
            }
        }
        //Findings: Axis 0 is left stick l-r (-1 left to 1 right), Axis 1 is left stick u-d (-1 up to 1 down)
                    //And then Axis 2 is right stick l-r (same) and Axis 3 is right stuck u-d (same)
        return false;
    }
}

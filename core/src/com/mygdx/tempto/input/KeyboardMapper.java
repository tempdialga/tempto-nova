package com.mygdx.tempto.input;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.IntIntMap;

import static com.badlogic.gdx.Input.Keys;
import static com.mygdx.tempto.input.InputTranslator.GameInputs;


import java.util.HashMap;

/**A static class for mapping keyboard bindings to abstract Tempto input, and changing these values at runtime.
 * TODO: Add in-game support for changing key bindings
 */
public class KeyboardMapper {

    /////// Mappings ////////
    /**A hashmap where keys of key codes (as given by LibGDX's {@link Input.Keys}) map to corresponding abstract inputs (as given by {@link InputTranslator}).
     * Thus multiple keys can map to the same input, but the same key cannot have multiple effects. (mostly for things like having WASD and arrow keys both input direction).
     *
     * The map itself is private and should be accessed using utility methods to ensure intended outcome*/
    private static final IntIntMap keyboardToAbstract = new IntIntMap();

        //// The default mappings TODO: Flesh out "weak bindings", i.e. the bindings like arrow keys or enter that serve as secondary bindings you don't need to set; These bindings should be overridden when the key is actively set to something else, but return to their weak bindings if unoccupied
    static {
        //// Main bindings, the ones you'd change in the menu
        addIntArray(keyboardToAbstract, new int[]{
                Keys.ESCAPE, GameInputs.PAUSE,

                Keys.J, GameInputs.CONFIRM,
                Keys.I, GameInputs.CANCEL,

                Keys.W, GameInputs.UP,
                Keys.A, GameInputs.LEFT,
                Keys.S, GameInputs.DOWN,
                Keys.D, GameInputs.RIGHT,

                Keys.SPACE, GameInputs.JUMP,

                Keys.BACKSLASH, GameInputs.DEBUG_SAVE,

        });
        //// Secondary/"weak" bindings, the ones that duplicate their intuitive input unless you actively tell them to do something else
        addIntArray(keyboardToAbstract, new int[]{
                Keys.ENTER, GameInputs.CONFIRM,

                Keys.UP, GameInputs.UP,
                Keys.LEFT, GameInputs.LEFT,
                Keys.DOWN, GameInputs.DOWN,
                Keys.RIGHT, GameInputs.RIGHT,
        });
    }

    /**Returns the game input corresponding to a keyboard key code, or {@link GameInputs#OTHER} if the keycode doesn't map to any of them
     * @param keyboardCode The keyboard keycode, as defined by {@link Keys}, to translate*/
    public static int gameInputOf(int keyboardCode) {
        // Return the input that key maps to, or OTHER if it doesn't map to any
        return keyboardToAbstract.get(keyboardCode, GameInputs.OTHER);
    }


    /**Quick utility method to dump a 2d int array into an {@link IntIntMap}, to make declaring the initial input values a little cleaner.
     * @param map The Int-Int map to add the entries to
     * @param entries A n*2 length int array, sets of 2 elements where the first element is the desired key and the second the value
     */
    private static void addIntArray(IntIntMap map, int[] entries) {
        // Check that the array is an even number of entries long
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Array of entry pairs must be of even length");
        }
        for (int i = 0; i < entries.length; i+=2) { //Iterate through every two entries, treating each as a pair
            int j = i+1;
            map.put(entries[i], entries[j]);
        }
    }
}

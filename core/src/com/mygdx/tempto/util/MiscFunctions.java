package com.mygdx.tempto.util;

/**A class with miscellaneous utility functions*/
public class MiscFunctions {
    /**Returns either the value, or, if the value does not fit in the given boundaries, the boundary closest to the value.
     * Assumption: max > min*/
    public static int clamp(int value, int min, int max) {
        return Math.max(Math.min(value, max), min);
    }

    /**Returns if the given string represents an integer value*/
    public static boolean isInteger(String str) {
        if (str == null) return false; //Can't be an integer if there's nothing

        int length = str.length();
        if (length == 0) return false; //Can't be an integer if there's nothing

        int i = 0;
        if (str.charAt(0) == '-') { //If it starts with a minus sign it still might be an integer
            if (length == 1) return false; //As long as there's stuff after it
            i = 1;
        }
        for (; i < length; i++) { //For each remaining character:
            char c = str.charAt(i);
            if (c < '0' || c > '9') return false; //If it's not a character between 0 and 9 it isn't an integer (an analogous method to check hex would just use different chars)
        }
        return true; //If every character is an integer from 0-9 permitting a minus sign in front, it should be an integer :)
    }
}

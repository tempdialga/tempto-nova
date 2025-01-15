package com.mygdx.tempto.data;

import com.badlogic.gdx.utils.JsonValue;

public interface SavesToFile {
    /**Any data the object needs to save for future use, serialized into a {@link com.badlogic.gdx.utils.JsonValue} object
     * @param host The JsonValue that the object should write a new value into*/
    void writeSerializedValue(JsonValue host);
}

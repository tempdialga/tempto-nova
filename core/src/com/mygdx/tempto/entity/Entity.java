package com.mygdx.tempto.entity;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.mygdx.tempto.maps.WorldMap;

import java.util.ArrayList;

/**A general class/interface for anything that interacts with the world map. This could be anything from pieces of terrain to the player to cutscene triggers*/
public interface Entity{

    /**Updates the entity in the world, by the given step in time.
     * @param deltaTime The time to progress by, typically the time since the last frame
     * @param world The {@link WorldMap} the entity exists in*/
    void update(float deltaTime, WorldMap world);

}

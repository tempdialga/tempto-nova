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
    
    /**Assigns the entity to a world. This will allow it to check for map information, add itself to input processing chains, etc.
     * @param parent The world the entity is being assigned to; should be the same as the one given in {@link #update(float, WorldMap)}*/
    void setParentWorld(WorldMap parent);

}

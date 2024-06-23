package com.mygdx.tempto.maps;

import com.badlogic.gdx.maps.tiled.TiledMap;

public class WorldMap {
    /**Filepath for the Tiled map file (.tmx). If not otherwise specified, the loader will look for an identically named JSON (.json) file in the local file directory*/
    String tiledMapFilePath;
    TiledMap tiledMap;
    /**Path to a JSON (.json) file in the local file directory. By default, construed to be a file with the same name as {@link #tiledMapFilePath} (apart from file extension), located in local/data/map*/
    String mapDataFilePath;
}

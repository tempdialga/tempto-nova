package com.mygdx.tempto.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;

import java.util.HashMap;

public class CentralTextureData {
    public static TextureAtlas coreTextures;

    public static HashMap<String, AtlasRegion> regions;
    public static final String DEFAULT_DEPTH_NAME = "default_depth";

    public static void loadCoreTextures(FileHandle atlasFile) {
        coreTextures = new TextureAtlas(atlasFile);
        regions = new HashMap<>();
        regions.put(DEFAULT_DEPTH_NAME, coreTextures.findRegion(DEFAULT_DEPTH_NAME));
    }

    public static AtlasRegion getRegion(String name){
        if (!regions.containsKey(name)) {
            regions.put(name, coreTextures.findRegion(name));
        }
        return regions.get(name);
    }

    public static AtlasRegion getDepthRegion(String name, boolean addSuffix) {
        if (addSuffix) {
            name = name.replaceAll("\\.png$","_depth.png");
        }
        //Scenario 1: already cached
        if (regions.containsKey(name)) {
            return regions.get(name);
        }
        AtlasRegion depthRegion = coreTextures.findRegion(name);
        //Scenario 2: not already cached, but is specified in the atlas
        if (depthRegion != null) {
            return depthRegion;
        }
        //Scenario 3: not specified in the atlas, return the default depth texture (plain grey 7F7F7F7F)
        return regions.get(DEFAULT_DEPTH_NAME);
    }

    public static AtlasRegion getDepthRegion(String name) {return getDepthRegion(name, true);}

    public static TextureAtlas getCoreTextures() {
        return coreTextures;
    }
}

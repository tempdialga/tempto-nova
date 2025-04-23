package com.mygdx.tempto.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;

public class CentralTextureData {
    public static TextureAtlas coreTextures;

    public static HashMap<String, AtlasRegion> regions;
    /**Cached pairs of TextureRegions and their corresponding regions for depth mapping, for more complex situations like how tile regions are another subregion down*/
    public static HashMap<TextureRegion, TextureRegion> baseToDepthPairs;
    public static final String DEFAULT_DEPTH_NAME = "maps/default_depth";
    public static AtlasRegion defaultDepthRegion;

    public static void loadCoreTextures(FileHandle atlasFile) {
        coreTextures = new TextureAtlas(atlasFile);
        regions = new HashMap<>();
        defaultDepthRegion = coreTextures.findRegion(DEFAULT_DEPTH_NAME);
        regions.put(DEFAULT_DEPTH_NAME, defaultDepthRegion);
        baseToDepthPairs = new HashMap<>();
    }

    public static AtlasRegion getRegion(String name){
        if (!regions.containsKey(name)) {
            regions.put(name, coreTextures.findRegion(name));
        }
        return regions.get(name);
    }

    public static AtlasRegion getDepthRegion(String name, boolean addSuffix) {
        if (addSuffix) {
            name = name + "_depth";
        }
        //Scenario 1: already cached
        if (regions.containsKey(name) && regions.get(name) != null) {
            System.out.println("Returning cached depth region");
            return regions.get(name);
        }
        AtlasRegion depthRegion = coreTextures.findRegion(name);
        //Scenario 2: not already cached, but is specified in the atlas
        if (depthRegion != null) {
            System.out.println("Returning region found new from atlas");
            regions.put(name, depthRegion);
            return depthRegion;
        }
        //Scenario 3: not specified in the atlas, return the default depth texture (plain grey 7F7F7F7F)
        System.out.println("Returning default depth region");
        depthRegion = regions.get(DEFAULT_DEPTH_NAME);
        regions.put(name, depthRegion);
        return depthRegion;
    }

    public static AtlasRegion getDepthRegion(String name) {return getDepthRegion(name, true);}

    public static TextureAtlas getCoreTextures() {
        return coreTextures;
    }
}

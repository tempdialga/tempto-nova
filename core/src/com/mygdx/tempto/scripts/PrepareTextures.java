package com.mygdx.tempto.scripts;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

import java.io.File;

public class PrepareTextures {

    public static final String LOCAL_ASSETS_DIR = "./data/"; //local filepath, not internal. In the future, this may be replaced with the internal one
    public static final String PACKED_OUTPUT_DIR = "./packeddata/"; //Also local filepath, for now
    static TexturePacker packer;

    public static void main(String[] args) {
        TexturePacker.process(LOCAL_ASSETS_DIR, PACKED_OUTPUT_DIR, "temptoNovaTextures");
    }
}

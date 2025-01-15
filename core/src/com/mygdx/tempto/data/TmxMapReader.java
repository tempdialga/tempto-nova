package com.mygdx.tempto.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

public class TmxMapReader {
    public static TmxMapData read(FileHandle tmxFile){
        Element root = new XmlReader().parse(tmxFile);//Obtain root element
        //Parse necessary data, return TmxMapData?
        return null;
    }
}

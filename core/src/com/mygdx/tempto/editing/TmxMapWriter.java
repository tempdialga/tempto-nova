package com.mygdx.tempto.editing;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.XmlWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;

public class TmxMapWriter {
    public XmlWriter writer;

    public TmxMapWriter(){
        StringWriter writer = new StringWriter();
        this.writer = new XmlWriter(writer);
//        try {
//            this.writer.write("<?xml version=\"1.0\"?>\n");
//            this.writer.element("meow")
//                    .attribute("moo", "cow")
//                    .element("child")
//                        .attribute("moo", "cow")
//                        .element("child")
//                            .attribute("moo", "cow")
//                            .text("XML is like violence. If it doesn't solve your problem, you're not using enough of it.")
//                        .pop()
//                    .pop()
//                .pop();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println(writer);

    }
    /**Writes relevant portions of a map for Tempto back into a usable xml string. Intended for use by an internal map editor*/
    public String writeTiledMapToString(TiledMap map){

        StringWriter writer = new StringWriter();
        this.writer = new XmlWriter(writer);

        try {
            this.writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            this.writer.element("map");
            MapProperties mapProperties = map.getProperties();//Properties of the whole map

            float mapHeight = (int)mapProperties.get("height") * (int)mapProperties.get("tileheight");//Height of the map, for encoding positions in vertically reversed form


            //Encode some weird hard coded properties
            this.writer.attribute("version", "1.9");
            this.writer.attribute("tiledversion", "1.9.2");
            this.writer.attribute("renderorder", "right-down");
            this.writer.attribute("infinite","0");//Pretty sure this is a safe assumption
            //Encode properties
            for (Iterator<String> it = mapProperties.getKeys(); it.hasNext(); ) { //For each property, encode into the xml object
                String propName = it.next();
                Object propValue = mapProperties.get(propName);
                this.writer.attribute(propName, propValue);
            }
            //Write each layer to the map
            for (MapLayer layer : map.getLayers()){
                if (layer instanceof TiledMapTileLayer || layer instanceof TiledMapImageLayer){
                    continue; //We'll figure out how to deal with tile layers later if we need to
                }
                this.writer.element("objectgroup");
                int id = map.getLayers().getIndex(layer) + 1;//This does not 1:1 correspond with the original id, but it does give each layer a unique id. Unless it's found that LibGDX Tiled interface stores that original id and it's accessible, this should work fine
                this.writer.attribute("id", id);
                this.writer.attribute("name", layer.getName());
                MapProperties layerProps = layer.getProperties();
                for (Iterator<String> it = layerProps.getKeys(); it.hasNext(); ) { //For each additional property, encode into the xml object.
                    String propName = it.next();
                    Object propValue = layerProps.get(propName);
                    this.writer.attribute(propName, propValue);
                }


                //Write each object in that layer
                MapObjects layerObjects = layer.getObjects();
                for (MapObject object : layerObjects){
                    this.writer.element("object");

                    //Encode basic properties of the object for Tiled
                    int objID = layerObjects.getIndex(object);//Like before, doesn't necessarily correspond but ensures they maintain unique ids
                    this.writer.attribute("id", objID);
                    MapProperties objProps = object.getProperties();//For some reason x and y are loaded as if they were custom properties but saved as original properties
                    this.writer.attribute("x", objProps.get("x"));
                    this.writer.attribute("y", mapHeight - (float)objProps.get("y"));

                    //Encode custom properties of the object
                    this.writer.element("properties");
                    for (Iterator<String> it = objProps.getKeys(); it.hasNext(); ) { //For each additional property, encode into the xml object.
                        String propName = it.next();
                        if (propName.equals("x") || propName.equals("y")){//Don't re-encode x and y
                            continue;
                        }
                        Object propValue = objProps.get(propName);
                        this.writer.element("property");
                        this.writer.attribute("name", propName);
                        this.writer.attribute("value", propValue);
                        this.writer.pop();
                    }
                    this.writer.pop();

                    if (object instanceof PolygonMapObject polObj) {
                        this.writer.element("polygon");

                        //Write the contents of the polygon to a string in the same format as a tiled map
                        float[] verts = polObj.getPolygon().getVertices();
                        StringBuilder vertexString = new StringBuilder();
                        vertexString.append(String.valueOf(verts[0])).append(",").append(String.valueOf(-verts[1]));
                        for (int i = 2; i < verts.length; i += 2) {
                            float x = verts[i];
                            float y = -verts[i+1];
                            vertexString.append(" ").append(x).append(",").append(y);
                        }
                        this.writer.attribute("points", vertexString.toString());
                        this.writer.pop();
                    }

                    //Encode other data about what the object is like, based on what kind of map object it is

                    //Can't get the ide to suppress errors about preview features >:( so back to the if statement chain we go
                    if (object instanceof PolygonMapObject polObj) {
                        this.writer.element("polygon");
                        //Write the contents of the polygon to a string in the same format as a tiled map
                        float[] verts = polObj.getPolygon().getVertices();
                        StringBuilder vertexString = new StringBuilder();
                        vertexString.append(String.valueOf(verts[0])).append(",").append(String.valueOf(-verts[1]));
                        for (int i = 2; i < verts.length; i += 2) {
                            float x = verts[i];
                            float y = -verts[i+1];
                            vertexString.append(" ").append(x).append(",").append(y);
                        }
                        this.writer.attribute("points", vertexString.toString());
                        this.writer.pop();
                    }

//                    String objectClass = object.getClass().getSimpleName();
//                    System.out.println(objectClass);
//
//                    switch (objectClass) {
//                        case "PolygonMapObject" : //If it's a polygon map object
//                            System.out.println("Saving polygon");
//                            PolygonMapObject polObj = (PolygonMapObject) object;
//
//                            break;
//                        default: break;
//                    }

                    this.writer.pop();//End object
                }

                this.writer.pop();//End layer
            }
            this.writer.pop(); //End map
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return writer.toString();
    }


}

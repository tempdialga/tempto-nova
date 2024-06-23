package com.mygdx.tempto.maps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.XmlReader;
import com.mygdx.tempto.data.SavesToFile;
import com.mygdx.tempto.editing.TmxMapWriter;
import com.mygdx.tempto.entity.Entity;
import com.mygdx.tempto.entity.StaticTerrainElement;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class WorldMap {


    //File loading and unloading

    /**Base map data, from internal map file*/
    TiledMap tiledMap;
    /**Writer to save TiledMap data back to a file*/
    TmxMapWriter mapWriter;
    /**Path to a JSON (extension .dat) file in the local file directory, which is the data file describing the map*/
    String mapDataFilePath;
//    /**An object containing serializable data about the map that should be stored in a data file when the map is unloaded. At bare minimum, stores the map file path redundantly*/
//    PersistentMapData mapData;

    //"In-world" stuff like entities
    /**ArrayList of all entities loaded in the map*/
    ArrayList<Entity> entities;

    //Rendering utilities:
    OrthographicCamera camera;

    //Debugging utilities:
    ShapeRenderer debugRenderer;


    /**Loads a world map from the following:
     * @param mapID The ID of the map, such that the constructor looks for a data file in local/data/mapID.dat and a base map file in assets/map/mapID.tmx (unless mapID.dat specifies otherwise)*/
    public WorldMap(String mapID){

        this.entities = new ArrayList<>();
        this.debugRenderer = new ShapeRenderer();
        this.camera = new OrthographicCamera();
        this.mapDataFilePath = "data/maps/" + mapID + ".dat";//Where it would store persistent data
        FileHandle existingDataFile = Gdx.files.local(this.mapDataFilePath);//Thus, if it has previously stored persistent data, grab it
        boolean hasExistingData = existingDataFile.exists();

        JsonValue existingData;
        if (hasExistingData) {
            existingData = new JsonReader().parse(existingDataFile);//If there is existing data, load from the file
        } else {
            existingData = new JsonValue(JsonValue.ValueType.object);//If not, prepare as if the file was a blank json
        }
        //This map has never been loaded before; load using defaults of the internal map file
        String probableMap = "maps/" + mapID + ".tmx";//Convention for internal map files
        probableMap = "maps/testmap.tmx";//Quick debug thing to test validity of handmade files
        TmxMapLoader loader = new TmxMapLoader();
        this.tiledMap = loader.load(probableMap);//Attempt to load from what would be assumed to be the map file name
        XmlReader.Element mapRoot = new XmlReader().parse(Gdx.files.internal(probableMap));
        System.out.println(mapRoot.toString());
        for (int i = 0, j = mapRoot.getChildCount(); i < j; i++) {
            XmlReader.Element element = mapRoot.getChild(i);
            System.out.println(element.toString());
        }
        String mapStr = this.tiledMap.toString();
        System.out.println(mapStr);

        //TODO: Generalize and clean up this process
        MapProperties mapProps = this.tiledMap.getProperties();
        for (Iterator<String> it = mapProps.getKeys(); it.hasNext();) {
            String propName = it.next();
            System.out.println(propName);
        }
        for (MapLayer layer : this.tiledMap.getLayers()){
            System.out.println("Found a layer!");
            if (layer.getName().equalsIgnoreCase("terrain")){
                for (MapObject obj : layer.getObjects()){
                    if (obj instanceof PolygonMapObject){
                        StaticTerrainElement terrainPiece = new StaticTerrainElement((PolygonMapObject) obj, existingData);
                        this.entities.add(terrainPiece);

                    }
                }
            }
        }

        MapProperties props = this.tiledMap.getProperties();
        int width = props.get("width", int.class)*props.get("tilewidth", int.class);
        int height = props.get("height", int.class)*props.get("tileheight", int.class);
        this.camera.setToOrtho(false, width, height);
        this.camera.position.x = ((float) width) / 2;
        this.camera.position.y = ((float) height) / 2;

        this.mapWriter = new TmxMapWriter();

    }

//    /**Reads data from an instance of {@link PersistentMapData} to finish instantiating this map*/
//    private void loadFromPersistentData(PersistentMapData persistentData){
//        //TODO: Actually make it load stuff
//
//    }
    /**Updates the world by the given time increment.
     * @param deltaTime The time to step forward in the world (typically using the time since the last frame)*/
    public void update(float deltaTime) {
        for (Entity entity : this.entities) {
            entity.update(deltaTime, this);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {//Quick debug to save to file
            this.writeToFile();
        }
    }

    /**Renders contents of the map to the screen*/
    public void render(){
        this.debugRenderer.setProjectionMatrix(camera.combined);
        this.debugRenderer.setColor(Color.BLACK);
        this.debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (Entity entity : this.entities){
            if (entity instanceof StaticTerrainElement) {
                Polygon polygon = ((StaticTerrainElement) entity).polygon;
                Color color = ((StaticTerrainElement) entity).color;
                this.debugRenderer.setColor(color);
                this.debugRenderer.polygon(polygon.getTransformedVertices());
            }
        }
        this.debugRenderer.end();
    }

    public void writeToFile(){
        //Create central json object for the whole map
        JsonValue mapData = new JsonValue(JsonValue.ValueType.object);
        for (Entity entity : this.entities) {
            if (entity instanceof SavesToFile) {//If it wants to save to the file
                ((SavesToFile) entity).writeSerializedValue(mapData); //Have it save to the json object
            }
        }
        //Then write the json data to a file
        FileHandle saveFile = Gdx.files.local(this.mapDataFilePath);//Grab filepath
        saveFile.writeString(new Json().prettyPrint(mapData.prettyPrint(JsonWriter.OutputType.json, 50)), false);//Write a pretty print of the json value to that file path

        String mapXML = this.mapWriter.writeTiledMapToString(this.tiledMap);

        String absoluteAssetPath = "C:\\Users\\Owen\\Desktop\\TemptoDev\\CurrentDev\\TemptoNova\\assets\\maps\\";
        FileHandle file = Gdx.files.absolute(absoluteAssetPath + "testmap.tmx");
        file.writeString(mapXML, false);
        //System.out.println(mapXML);
    }
//
//    /**Internal class to serialize persistent data about a {@link WorldMap}, to be saved in JSON format using GDX {@link Json} utilities
//     * @param tiledMapFilePath Filepath for the Tiled map file (.tmx). If not otherwise specified, the loader will look for an identically named JSON (extension .dat) file in the local file directory
//     * @param fileInternal Specifies whether the base tmx file is located internally. True by default, should rarely if ever need to be local or other.
//     * @param serializedEntities An array list of entities in the map, stored in serialized form. Entity classes determine how their data is serialized; Classes which do not specify will be serialized by gdx library {@link Json}
//     * */
//    public record PersistentMapData(
//            String tiledMapFilePath,
//            boolean fileInternal,
//            ArrayList<String> serializedEntities) {
//
//
//    }
//    public static ArrayList<String> serializeEntities(ArrayList<Entity> entities, ArrayList<String> listToUse){
//        if (listToUse == null){listToUse = new ArrayList<>();}
//
//        Json serializer = new Json();//For now, we probably just need one set of settings for all entities
//
//        for (Entity entity : entities) {
//
//            String serialEntity = serializer.toJson(entity); //Serialized form of the entity
//            if (serialEntity != null)
//                listToUse.add(serialEntity);
//        }
//        return listToUse;
//    }

}

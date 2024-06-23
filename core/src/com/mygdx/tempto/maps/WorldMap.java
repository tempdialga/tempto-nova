package com.mygdx.tempto.maps;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.tempto.TemptoNova;
import com.mygdx.tempto.data.SavesToFile;
import com.mygdx.tempto.editing.TmxMapWriter;
import com.mygdx.tempto.entity.Entity;
import com.mygdx.tempto.entity.StaticTerrainElement;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.view.GameScreen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

public class WorldMap {

    //Game structure and input

    /**The {@link GameScreen} which is operating this map.*/
    GameScreen parent;
    /**The interface for giving input to the world*/
    InputMultiplexer worldInput;

    //File loading and unloading

    /**Base map data, from internal map file*/
    TiledMap tiledMap;
    /**Writer to save TiledMap data back to a file*/
    TmxMapWriter mapWriter;
    /**Path to a JSON (extension .dat) file in the local file directory, which is the data file describing the map*/
    String mapDataFilePath;


    //"In-world" stuff like entities

    /**ArrayList of all entities loaded in the map*/
    ArrayList<Entity> entities;

    //Rendering utilities:

    FitViewport worldViewport;
    OrthographicCamera camera;
    SpriteBatch worldBatch;

    //Debugging utilities:

    ShapeRenderer debugRenderer;
    Texture debugTexture;
    Sprite debugSprite;


    /**Loads a world map from the following:
     * @param parent The GameScreen this map is being initialized in. (This is usually run from within a GameScreen)
     * @param mapID The ID of the map, such that the constructor looks for a data file in local/data/mapID.dat and a base map file in assets/map/mapID.tmx (unless mapID.dat specifies otherwise)*/
    public WorldMap(String mapID, GameScreen parent){
        this.parent = parent;

        // Initialize world input, and add a testing input device to save to file
        this.worldInput = new InputMultiplexer();
        this.addWorldInput(0, new InputAdapter(){
            @Override
            public boolean keyDown(int keycode) { // When an input is given to save to file, do so
                if (keycode == InputTranslator.GameInputs.DEBUG_SAVE) {
                    WorldMap.this.writeToFile();
                }
                return false;
            }
        });

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

        // Assign each entity to this world
        for (Entity entity : this.entities) {
            entity.setParentWorld(this);
        }

        MapProperties props = this.tiledMap.getProperties();
        int width = props.get("width", int.class)*props.get("tilewidth", int.class);
        int height = props.get("height", int.class)*props.get("tileheight", int.class);
        this.camera.setToOrtho(false, width, height);
        this.camera.position.x = ((float) width) / 2;
        this.camera.position.y = ((float) height) / 2;

        // Create a viewport to go with the world
        this.worldViewport = new FitViewport(TemptoNova.PIXEL_GAME_WIDTH, TemptoNova.PIXEL_GAME_HEIGHT, this.camera);

        // Create a spritebatch (also mostly for testing things)
        this.worldBatch = new SpriteBatch();

        //Create a debug texture for testing things
        this.debugTexture = new Texture("badlogic.jpg");
        this.debugSprite = new Sprite(this.debugTexture);

        this.mapWriter = new TmxMapWriter();

    }

    /////// Game logic ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**Updates the world by the given time increment.
     * @param deltaTime The time to step forward in the world (typically using the time since the last frame)*/
    public void update(float deltaTime) {
        for (Entity entity : this.entities) {
            entity.update(deltaTime, this);
        }

        Vector2 cameraMovement = new Vector2(15,15).scl(deltaTime).scl(InputTranslator.GameInputs.inputDirection);
        this.camera.position.add(cameraMovement.x, cameraMovement.y, 0); //Quick debug to move camera with user input
    }

    //////// Input //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**Returns the InputMultiplexer that gives input to the world.*/
    public InputMultiplexer getWorldInput() {
        return worldInput;
    }
    /**Adds an input listener to the {@link InputMultiplexer} that takes world inputs.
     * @param index The index to add the input at, where inputs are handled starting from 0 and working up.
     * @param newInput The input to add. Should take {@link com.mygdx.tempto.input.InputTranslator.GameInputs} codes, as that is what it will be given*/
    public void addWorldInput(int index, InputProcessor newInput) {
        this.worldInput.addProcessor(index, newInput);
    }

    /**Adds an input listener to the {@link InputMultiplexer} that takes world inputs, after the rest of the inputs.
     * @param newInput The input to add. Should take {@link com.mygdx.tempto.input.InputTranslator.GameInputs} codes, as that is what it will be given*/
    public void addWorldInput(InputProcessor newInput) {
        this.worldInput.addProcessor(newInput);
    }

    /**Removes an input listener from the {@link InputMultiplexer} that handles world inputs.
     * @param toRemove The input processor to remove from the world inputs.*/
    public void removeWorldInput(InputProcessor toRemove) {
        this.worldInput.removeProcessor(toRemove);
    }


    //////// Rendering ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**Renders contents of the map to the screen*/
    public void render(){
        ScreenUtils.clear(0.2f,0,0.2f,1);
        // Apply the viewport to the camera

        this.worldViewport.apply();


//        Camera worldViewportCamera = this.worldViewport.getCamera();
//        worldViewportCamera.position.set(this.worldViewport.getWorldWidth()/2, this.worldViewport.getWorldHeight()/2,1f);

        this.debugRenderer.setProjectionMatrix(this.camera.combined);
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
        this.debugRenderer.setColor(Color.BLACK);
        this.debugRenderer.circle(this.camera.position.x, this.camera.position.y, 1);
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

        //String absoluteAssetPath = "C:\\Users\\Owen\\Desktop\\TemptoDev\\CurrentDev\\TemptoNova\\assets\\maps\\";
        String toMapData = "data/maps/";
        FileHandle file = Gdx.files.external(toMapData + "testmap.tmx");
        file.writeString(mapXML, false);
        file.writeString(mapXML, false);
        //System.out.println(mapXML);
    }



    public void dispose() {
        this.worldBatch.dispose();
        this.debugRenderer.dispose();
        this.debugTexture.dispose();
    }

    /**Called to update the world's rendering utilities, so it can properly render to the screen.
     * If we */
    public void resizeWindow(int newWidth, int newHeight) {
        this.worldViewport.update(newWidth, newHeight);
    }

}

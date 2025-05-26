package com.mygdx.tempto.maps;

import static com.mygdx.tempto.view.GameScreen.elapsedTime;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.tempto.TemptoNova;
import com.mygdx.tempto.data.CentralTextureData;
import com.mygdx.tempto.data.SavesToFile;
import com.mygdx.tempto.editing.MapEditor;
import com.mygdx.tempto.editing.TmxMapWriter;
import com.mygdx.tempto.entity.Entity;
import com.mygdx.tempto.entity.StaticTerrainElement;
import com.mygdx.tempto.entity.decoration.TileLayer;
import com.mygdx.tempto.entity.player.Player;
import com.mygdx.tempto.entity.testpoint.TestPoint;
import com.mygdx.tempto.entity.physics.Collidable;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.rendering.AltDepthBatch;
import com.mygdx.tempto.rendering.AltFinalBatch;
import com.mygdx.tempto.rendering.AltLightBatch;
import com.mygdx.tempto.rendering.AltShadeBatch;
import com.mygdx.tempto.rendering.LightSource;
import com.mygdx.tempto.rendering.ShadowCaster;
import com.mygdx.tempto.rendering.TileLayerDepthRenderer;
import com.mygdx.tempto.rendering.TileLayerFinalRenderer;
import com.mygdx.tempto.rendering.RendersToScreen;
import com.mygdx.tempto.rendering.RendersToWorld;
import com.mygdx.tempto.util.MiscFunctions;
import com.mygdx.tempto.view.GameScreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import space.earlygrey.shapedrawer.ShapeDrawer;

public class WorldMap implements RendersToScreen {

    public static final float TILE_SIZE = 16;

    //Game structure and input

    /**The {@link GameScreen} which is operating this map.*/
    GameScreen parent;
    /**The interface for giving input to the world*/
    InputMultiplexer worldInput;

    //Editing
    /**The GUI that edits the world*/
    MapEditor editor;

    //File loading and unloading

    /**Default location of map files within the 'data' local directory*/
    public static final String DEFAULT_MAP_DATA_RELPATH = "maps/";
    /**ID of the map currently loaded*/
    String mapID;
    /**Relative path of the current map file, from the 'data' local directory. Currently always {@link #DEFAULT_MAP_DATA_RELPATH}, but this could likely change*/
    public String pathDirFromData = DEFAULT_MAP_DATA_RELPATH;
    /**Base map data, from internal map file*/
    TiledMap tiledMap;
    /**The layer chosen as the central layer to store entities, currently just the first layer found that already has an entity in it TODO: choose the map layer in a more sophisticated manner*/
    MapLayer entityLayer;
    /**Writer to save TiledMap data back to a file*/
    TmxMapWriter mapWriter;
    /**Path to a JSON (extension .dat) file in the local file directory, which is the data file describing the map*/
    String mapDataFilePath;
    /**The map loader, which also stores some relevant context about the original map loading*/
    NewAtlasTmxMapLoader mapLoader;


    //"In-world" stuff like entities

    /**ArrayList of all entities loaded in the map*/
    ArrayList<Entity> entities;
    /**ArrayList of all terrain and other implementors of {@link Collidable} which entities can collide with*/
    ArrayList<Collidable> collidables;

    /**Gravitational acceleration, in in-game units (IGU) per second*/
    static final float DEFAULT_GRAVITY = 45;

    /**The maximum amount of time that a world can progress in a single timestep, in seconds*/
    public static final float MAX_FRAME_TIME = 0.2f;

    /**The maximum number of lights that the game can store information on (4 color channels * n columns * n rows)*/
    public static final int SHADMAP_COLS = 4, SHADMAP_ROWS = 4, MAX_N_SHADOWS = 4*SHADMAP_COLS*SHADMAP_ROWS;


    //Rendering utilities:

    FitViewport worldViewport;
    OrthographicCamera camera;
    SpriteBatch miscWorldBatch;
    AltDepthBatch depthMapBatch;
    ArrayList<LightSource> lightSources;
    final int debugLightCount = 10;
    AltLightBatch lightBatch;
    AltShadeBatch shadeBatch;
    AltFinalBatch finalPassBatch;
    public ShapeDrawer editorShapeDrawer;
    public ShapeDrawer tempFinalPassShapeDrawer;
    public Texture blankTexture = new Texture("blank.png");
    public TextureRegion blank = CentralTextureData.getRegion("misc/blank");

    FrameBuffer depthBuffer;
    Texture depthMap;
    FrameBuffer shadowBuffer;
    Texture shadowMap;
    FrameBuffer lightBuffer;
    Texture lightMap;
    public TileLayerFinalRenderer tileFinalRenderer;
    public TileLayerDepthRenderer tileDepthRenderer;

    //Debugging utilities:

    ShapeRenderer debugRenderer;
    Texture debugTexture;
    Sprite debugSprite;


    /**Loads a world map from the following:
     * @param parent The GameScreen this map is being initialized in. (This is usually run from within a GameScreen)
     * @param mapID The ID of the map, such that the constructor looks for a data file in local/data/mapID.dat and a base map file in assets/map/mapID.tmx (unless mapID.dat specifies otherwise)*/
    public WorldMap(String mapID, GameScreen parent){
        this.parent = parent;
        this.mapID = mapID;

        // Initialize world input, and add a testing input device to save to file
        this.worldInput = new InputMultiplexer();
        this.addWorldInput(0, new InputAdapter(){
            @Override
            public boolean keyDown(int keycode) { // When an input is given to save to file, do so
                if (keycode == InputTranslator.GameInputs.DEBUG_SAVE) {
                    WorldMap.this.writeToSaveFile();
                    if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                        WorldMap.this.writeToCoreFile(); // Also save anything that needs to be to the core file
                    }
                    return true;
                }
                return false;
            }
        });

        this.entities = new ArrayList<>();
        this.collidables = new ArrayList<>();
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
        String probableMap = "data/"+this.pathDirFromData + mapID + ".tmx";//Convention for internal map files
        FileHandle localMapFile = Gdx.files.local(probableMap); //Find where it would be locally
//        NewAtlasTmxMapLoader loader;
        TmxMapLoader oldLoader;
        if (localMapFile.exists()) {
            this.mapLoader = new NewAtlasTmxMapLoader(new LocalFileHandleResolver()); //If there's a local file with the right name, create a local file to load from that
            oldLoader = new TmxMapLoader(new LocalFileHandleResolver());
        } else {
            this.mapLoader = new NewAtlasTmxMapLoader(); //And if not, load from assets
            oldLoader = new TmxMapLoader();
        }
        this.mapLoader.setParent(this);

        this.tiledMap = this.mapLoader.load(probableMap);//Attempt to load from what would be assumed to be the map file name
//        XmlReader.Element mapRoot = new XmlReader().parse(Gdx.files.internal(probableMap));
//        System.out.println(mapRoot.toString());
//        for (int i = 0, j = mapRoot.getChildCount(); i < j; i++) {
//            XmlReader.Element element = mapRoot.getChild(i);
//            System.out.println(element.toString());
//        }
//        String mapStr = this.tiledMap.toString();
//        System.out.println(mapStr);

        System.out.println(SpriteBatch.createDefaultShader().getVertexShaderSource());
        System.out.println(SpriteBatch.createDefaultShader().getFragmentShaderSource());
        //TODO: Generalize and clean up this process
        MapProperties mapProps = this.tiledMap.getProperties();
        for (Iterator<String> it = mapProps.getKeys(); it.hasNext();) {
            String propName = it.next();
            System.out.println(propName);
        }
        for (MapLayer layer : this.tiledMap.getLayers()){
            System.out.println("Found a layer!");
            if (layer.getName().equalsIgnoreCase("terrain")){
                this.entityLayer = layer;
                for (MapObject obj : layer.getObjects()){
                    if (obj instanceof PolygonMapObject){
                        StaticTerrainElement terrainPiece = new StaticTerrainElement((PolygonMapObject) obj, existingData);
                        this.entities.add(terrainPiece);

                    }
                }
            } else if (layer instanceof TiledMapTileLayer tileLayer) {
                if (!layer.isVisible()) continue;
                //Tile layers, only for rendering I think
                String name = layer.getName();
                TileLayer temptoLayer;
                if (name.endsWith("px")) { //Confirms that it's a rendering layer, named [#]px from the screen
                    float depth = Float.parseFloat(name.substring(0,name.length()-2));
                    temptoLayer = (new TileLayer(this, tileLayer, this.mapLoader.layerElements.get(layer), depth));
                } else if (name.endsWith("px_rot")) { //Same but it's one testing rotated elements
                    float depth = Float.parseFloat(name.substring(0,name.length()-6));
                    temptoLayer = (new TileLayer(this, tileLayer, this.mapLoader.layerElements.get(layer), depth, true));
                } else if (name.endsWith("px_wav")) {
                    float depth = Float.parseFloat(name.substring(0,name.length()-6));
                    temptoLayer = (new TileLayer(this, tileLayer, this.mapLoader.layerElements.get(layer), depth));
                    temptoLayer.setWaver(true);
                } else {
                    if (name.equals("flats")) {
                        TileLayer.setFlatTiles(tileLayer);
                    }
                    continue;
                }
                entities.add(temptoLayer);
            }
        }

        this.entities.add(new TestPoint(new Vector2(0,0), this));
        this.entities.add(new Player(new Vector2(), this));

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
        this.miscWorldBatch = new SpriteBatch();
        this.depthMapBatch = new AltDepthBatch();
        this.lightBatch = new AltLightBatch();
        this.shadeBatch = new AltShadeBatch();
        this.finalPassBatch = new AltFinalBatch();

        // Create a centralized shape renderer to use for stuff
        this.editorShapeDrawer = new ShapeDrawer(this.miscWorldBatch);
        this.editorShapeDrawer.setTextureRegion(this.blank);
        // TODO: Figure out our own shapedrawer situation, since this wont work with the custom shaders and whatnot
        this.tempFinalPassShapeDrawer = new ShapeDrawer(this.finalPassBatch);
        this.tempFinalPassShapeDrawer.setTextureRegion(this.blank);

        //Create a debug texture for testing things
        this.debugTexture = new Texture("badlogic.jpg");
        this.debugSprite = new Sprite(this.debugTexture);

        //Initialize the depth map
        this.depthBuffer = new FrameBuffer(Pixmap.Format.RGBA8888,TemptoNova.PIXEL_GAME_WIDTH, TemptoNova.PIXEL_GAME_HEIGHT, true);

        //Initialize the shadow map TODO: Work out the camera with resolutions of the buffers and whatnot
        this.shadowBuffer = new FrameBuffer(Pixmap.Format.RGBA4444,TemptoNova.PIXEL_GAME_WIDTH*SHADMAP_ROWS, TemptoNova.PIXEL_GAME_HEIGHT*SHADMAP_COLS, false);

        //Initialize the light map
//        this.lightBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, TemptoNova.PIXEL_GAME_WIDTH, TemptoNova.PIXEL_GAME_HEIGHT, false);
        this.lightBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

        this.lightSources = new ArrayList<>();
        this.lightSources.add(new LightSource(new Vector3(), new Color(1,0.9f, 0.5f, 1), 250, LightSource.SPHERE_APPROX, 1));
        this.lightSources.add(new LightSource(new Vector3(), Color.CYAN, 250, LightSource.SPHERE_APPROX, 1));
        this.lightSources.add(new LightSource(new Vector3(), Color.CHARTREUSE, 250, LightSource.SPHERE_APPROX, 1));
        for (int i = 3; i < this.debugLightCount; i++) {
            this.lightSources.add(new LightSource(new Vector3(), new Color(Color.CORAL).mul(0.5f), 550, LightSource.SPHERE_APPROX, 0));
        }

        //Initialize tilemap renderer
        this.tileFinalRenderer = new TileLayerFinalRenderer(this.tiledMap, this.finalPassBatch);
        this.tileDepthRenderer = new TileLayerDepthRenderer(this.tiledMap, this.depthMapBatch);


        this.mapWriter = new TmxMapWriter();

        //Initialize editing software
        this.editor = new MapEditor(this);
        this.worldInput.addProcessor(1, this.editor);

    }

    /////// Game logic ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**Updates the world by the given time increment.
     * @param deltaTime The time to step forward in the world (typically using the time since the last frame)*/
    public void update(float deltaTime) {
        // Only allow time to progress a certain amount per frame at most
        if (deltaTime > MAX_FRAME_TIME) deltaTime = MAX_FRAME_TIME;

        // If in editing mode, freezes time
        if (this.isEditing()) deltaTime = 0;

        // Prompt any mouse movement, since apparently LibGDX isn't doing that properly for some reason
        this.worldInput.mouseMoved(Gdx.input.getX(), Gdx.input.getY());

        // Update terrain
        this.updateCollisionList();

        // Update every entity with the amount of time that has passed
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

        //Define light at the mouse coordinates for simplicity
        Vector3 mouseCoords = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        mouseCoords.z=-9 + 10*(float) Math.sin(3* elapsedTime);
//        System.out.println("Mouse depth: " +mouseCoords.z);
        this.lightSources.get(0).pos().set(mouseCoords);
        this.lightSources.get(1).pos().set(mouseCoords).add(-40, -40, -10);
        this.lightSources.get(2).pos().set(mouseCoords).add(40, -20, -1);
        for (int i = 3; i < debugLightCount; i++) {
            this.lightSources.get(i).pos().set(mouseCoords).add(190-10*i, 50+10*(float)Math.sin(0.5f*i+2*elapsedTime), -20);
        }
//        LightSource mouseLight = new LightSource(mouseCoords, Color.YELLOW, 250, LightSource.SPHERE_APPROX, 0.9f);

        // Render depth buffer
        this.depthBuffer.begin();
        float bgDepth = 150;
        Gdx.gl.glClearDepthf(1/bgDepth);
        ScreenUtils.clear(1/bgDepth,0.5f,0.5f,0.5f, true);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_GREATER);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);

        this.depthMapBatch.setProjectionMatrix(this.camera.combined);
        this.depthMapBatch.disableBlending();
        this.depthMapBatch.begin();
        for (Entity entity : this.entities) {
            if (entity instanceof TileLayer renderable) { //Currently only tile layer bc this is the only one the batch supports atm
//                if (renderable.isWaver()) {
//                    renderable.setBaseDepth(mouseCoords.z + 20 + 10);
//                }
                renderable.renderToDepthMap(this.depthMapBatch, this.camera);
            }
        }


        this.depthMapBatch.end();
        this.depthBuffer.end();
        this.depthMap = depthBuffer.getColorBufferTexture();
        this.depthMap.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        //Render lights ! :D
        this.shadeBatch.switchShadowShader((int)(elapsedTime*0.5f) % (AltShadeBatch.NINE_SAMPLE+1)); //Debug: switch shadow shaders every 2 seconds
        this.shadowBuffer.begin();


        this.shadeBatch.setProjectionMatrix(this.camera.combined);

        ArrayList<ShadowCaster> casters = new ArrayList<>();
        for (Entity entity : this.entities) {
            if (entity instanceof RendersToWorld renders) {
                renders.addShadowCastersToList(casters);
            }
        }

        float width = camera.viewportWidth * camera.zoom;
        float height = camera.viewportHeight * camera.zoom;
        float w = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x);
        float h = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x);
        Rectangle viewBounds = new Rectangle(camera.position.x - w / 2, camera.position.y - h / 2, w, h);
        Polygon viewPoly = new Polygon(new float[]{
                viewBounds.x, viewBounds.y,
                viewBounds.x, viewBounds.y+h,
                viewBounds.x+w, viewBounds.y+h,
                viewBounds.x+w, viewBounds.y
        });

//        ShadowCaster screenTestCaster = new ShadowCaster(new TextureRegion(depthMap), new Vector3(viewBounds.x, viewBounds.y, 0), new Vector3(viewBounds.width, 0, 0), new Vector3(0, viewBounds.height, 0));
//        casters.add(screenTestCaster);
//        System.out.println(casters.size() + " casters found");

//        this.shadeBatch.dispose();
//        this.shadeBatch = new AltShadeBatch();
//        this.shadeBatch.setProjectionMatrix(this.camera.combined);
//        this.shadeBatch.enableBlending();

        final int RED = 0, GREEN = 1, BLUE = 2, ALPHA = 3;
        int color_channel = RED;
        int shadMapCol = 0;
        int shadMapHTotal = this.shadowBuffer.getWidth() / TemptoNova.PIXEL_GAME_WIDTH;
        int shadMapRow = 0;
        int shadMapVTotal = this.shadowBuffer.getHeight() / TemptoNova.PIXEL_GAME_HEIGHT;
        ScreenUtils.clear(1, 1, 1, 1);
        this.shadeBatch.setBlendFunctionSeparate(GL20.GL_DST_COLOR, GL20.GL_ZERO, GL20.GL_ONE, GL20.GL_ZERO);//Mult
//        this.shadeBatch.setBlendFunctionSeparate(GL20.GL_ONE, GL20.GL_ONE, GL20.GL_SRC_ALPHA, GL20.GL_DST_ALPHA);//Add
        this.shadeBatch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
        this.shadeBatch.adjustChannelDims(shadMapHTotal, shadMapVTotal);
        Gdx.gl.glBlendEquation(GL20.GL_FUNC_REVERSE_SUBTRACT); //naturally switch to subtracting actually
        this.shadeBatch.begin();
        this.shadeBatch.setViewport(this.worldViewport);



        Collections.sort(casters);

        for (int shadowShader = 0; shadowShader < AltShadeBatch.NUM_SHADOW_SHADERS; shadowShader++) {
            this.shadeBatch.switchShadowShader(shadowShader);
            color_channel = RED;
            shadMapCol = 0;
            shadMapRow = 0;
            Gdx.gl.glColorMask(color_channel == RED, color_channel == GREEN, color_channel == BLUE, color_channel == ALPHA);
            for (int i = 0; i < this.lightSources.size(); i++) {
                Gdx.gl.glColorMask(color_channel == RED, color_channel == GREEN, color_channel == BLUE, color_channel == ALPHA);

                LightSource source = this.lightSources.get(i);

                ShadowCaster.numRangesVisible = 0;

                for (ShadowCaster caster : casters) {
                    if (caster.shadowShader(source) == shadowShader) this.shadeBatch.drawShadow(caster, source, this.depthMap, this.camera, viewBounds, viewPoly, shadMapCol, shadMapHTotal, shadMapRow, shadMapVTotal);
                }


                shadMapCol++; //Iterate position
                if (shadMapCol >= shadMapHTotal) {
                    shadMapCol = 0;
                    shadMapRow++;
                }

                if (shadMapRow >= shadMapVTotal) { //Then, when positions run out, switch to the next color channel
                    color_channel++;

                    shadMapRow = 0;
                    shadMapCol = 0;
                    this.shadeBatch.flush();
                }
            }
        }
        this.shadeBatch.end();

        this.shadowBuffer.end();
        this.shadowMap = this.shadowBuffer.getColorBufferTexture();
        this.shadowMap.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);


        this.lightBuffer.begin();

        this.lightBatch.setProjectionMatrix(this.camera.combined);
        Gdx.gl.glColorMask(true, true, true, true);
        Color amb = new Color(Color.NAVY).mul(0.3f);

        ScreenUtils.clear(amb.r,amb.g,amb.b,1f);
        Gdx.gl.glColorMask(true, true, true, false);
//        this.lightBatch.setBlendFunctionSeparate(GL20.GL_DST_COLOR, GL20.GL_ZERO, GL20.GL_ONE, GL20.GL_ZERO);
        Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD);
        this.lightBatch.adjustChannelDims(shadMapHTotal, shadMapVTotal);
        this.lightBatch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
        this.lightBatch.enableBlending();
        this.lightBatch.begin();
        this.lightBatch.setViewport(this.worldViewport);
        shadMapRow = 0;
        shadMapCol = 0;
        color_channel = RED;
        for (int i = 0; i < this.lightSources.size(); i++) {
            float color_channel_float = color_channel + 0.01f;
            this.lightBatch.drawLight(this.lightSources.get(i), this.depthMap, this.shadowMap, this.camera, viewBounds, color_channel, shadMapCol, shadMapRow);

            shadMapCol++;//Iterate position
            if (shadMapCol >= shadMapHTotal) {
                shadMapCol = 0;
                shadMapRow++;
            }
            if (shadMapRow >= shadMapVTotal) {//Loop to first position and switch color channel
                shadMapRow = 0;
                shadMapCol = 0;
                color_channel++;
            }
        }
        this.lightBatch.end();

        this.lightBuffer.end();
        this.lightMap = this.lightBuffer.getColorBufferTexture();
        this.lightMap.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        Gdx.gl.glColorMask(true, true, true, true);




        this.debugRenderer.setProjectionMatrix(this.camera.combined);
        this.debugRenderer.setColor(Color.BLACK);
        this.debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (Entity entity : this.entities){
            if (entity instanceof StaticTerrainElement) {
                Polygon polygon = ((StaticTerrainElement) entity).polygon;
                Color color = ((StaticTerrainElement) entity).color;
                this.debugRenderer.setColor(color);
                int numPoints = polygon.getVertexCount();
                if (numPoints >= 3)  this.debugRenderer.polygon(polygon.getTransformedVertices());
            }
        }
        this.debugRenderer.setColor(Color.BLACK);
        this.debugRenderer.circle(this.camera.position.x, this.camera.position.y, 1);
        this.debugRenderer.end();

        Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        this.finalPassBatch.switchLightTexture(this.lightMap);
        this.finalPassBatch.setProjectionMatrix(this.camera.combined);
        this.finalPassBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
//        this.finalPassBatch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
//        ScreenUtils.clear(0,0,0,1);
//        Gdx.gl.glColorMask(true, false, false, false);
//        this.finalPassBatch.disableBlending();
        this.finalPassBatch.begin();
        float hw = TemptoNova.PIXEL_GAME_WIDTH / 2f, hh = TemptoNova.PIXEL_GAME_HEIGHT / 2f;
        float x = this.camera.position.x - hw, y = this.camera.position.y;
        this.finalPassBatch.flush();
        this.finalPassBatch.draw(blankTexture, x, y+hh, hw*2, -hh*2);

        for (Entity entity : this.entities) {
            if (entity instanceof RendersToWorld renderable) {
                renderable.renderToWorld(this.finalPassBatch, this.camera);
            }
        }


        this.finalPassBatch.end();

        boolean debugRender = false;
        if (debugRender) {
            this.miscWorldBatch.setProjectionMatrix(this.camera.combined);
            this.miscWorldBatch.begin();
            this.editor.renderToWorld(this.miscWorldBatch, camera);

            this.miscWorldBatch.draw(this.depthMap, x, y, hw, -hh);
            this.miscWorldBatch.draw(this.shadowMap, x, y + hh, hw, -hh);
            this.miscWorldBatch.draw(this.lightMap, x + hw, y, hw, -hh);
            this.miscWorldBatch.end();
        }
//        Gdx.gl.glColorMask(true, true, true,  true);
    }

    /**Renders anything of the world that should be rendered using the screen camera*/
    @Override
    public void renderToScreen(SpriteBatch batch, OrthographicCamera screenCamera, float aspectRatio) {
        this.editor.renderToScreen(batch, screenCamera, aspectRatio); //Render the editor; If the editor isn't active then it won't have to render anything
    }


    //////// File Handling /////////////////////////////
    public void writeToSaveFile(){
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

    }

    /**Cycles through entities and, if they have changes to be reflected to the base map file, compiles those changes*/
    public void writeToCoreFile() {
        // Check if the map data needs to be changed
//        for (Entity entity : this.entities) {
//            if (entity.needToEditBaseFile()) {
//                entity.updateBaseFile(this.tiledMap, this.entityLayer);
//            }
//        }

        // Write the map data to file TODO: Could we do this and guarantee non-destruction of the existing map data, in case it wasn't read properly?
        String mapXML = this.mapWriter.writeTiledMapToString(this.tiledMap, this.mapLoader);

        String toMapData = "data/maps/";
        FileHandle file = Gdx.files.local(toMapData+ this.mapID + ".tmx");
        System.out.println("Writing to file: " + file.path());
        file.writeString(mapXML, false);
    }

    public void writeToSaveAndCoreFile() {
        this.writeToSaveFile();
        this.writeToCoreFile();
    }

    //// Other Utility ////

    public void dispose() {
        this.miscWorldBatch.dispose();
        this.depthMapBatch.dispose();
        this.lightBatch.dispose();
        this.shadeBatch.dispose();
        this.finalPassBatch.dispose();

        this.depthBuffer.dispose();
        this.depthMap.dispose();
        this.shadowBuffer.dispose();
        this.shadowMap.dispose();


        this.debugRenderer.dispose();
        this.debugTexture.dispose();
        this.blankTexture.dispose();
    }

    /**Called to update the world's rendering utilities, so it can properly render to the screen.
     * If we */
    public void resizeWindow(int newWidth, int newHeight) {
        this.worldViewport.update(newWidth, newHeight);
    }

    public boolean isEditing() {
        return this.editor.active;
    }
    public void toggleEditing() {
        this.editor.active = !this.editor.active; //Toggle whether the editor is active
//        if (this.editing) { // Switch back to normal gameplay
//            this.worldInput.removeProcessor(this.editor);
//            this.editing = false;
//        } else { // Switch to editing
//            this.worldInput.addProcessor(0, this.editor);
//            this.editing = true;
//        }
    }

    public Vector2 mouseWorldCoords() {
        return screenToWorldCoords(Gdx.input.getX(), Gdx.input.getY());
    }

    public Vector2 screenToWorldCoords(float screenX, float screenY) {
        Vector3 mousePos = this.camera.unproject(new Vector3(screenX, screenY, 0));//Find the world position they're looking at
        return new Vector2(mousePos.x, mousePos.y);
    }

    //////// Getters n whatnot ///////////////////////////////////


    public OrthographicCamera getCamera() {
        return camera;
    }

    public String getMapID() {
        return mapID;
    }

    public ArrayList<Entity> getEntities() {
        return entities;
    }

    public TiledMap getTiledMap() {
        return tiledMap;
    }

    public MapLayer getEntityLayer() {
        return entityLayer;
    }

    /**Returns the first {@link MapObject} in the core map file, specifically in {@link #entityLayer}, that corresponds to the given {@link Entity} by ID.
     * Returns null if none are found in the entity layer.
     * @param entity The entity to search for
     */
    public MapObject getMapObjectForEntity(Entity entity) {
        for (MapObject object : this.entityLayer.getObjects()) {
            MapProperties props = object.getProperties();
            if (props.containsKey("id")) {
                if (props.get("id", "", String.class).equals(entity.getID())) {
                    return object;
                }
            }
        }
        return null;
    }

    public void addEntity(Entity toAdd) {
        this.entities.add(toAdd);
        toAdd.setParentWorld(this);
    }

    /**Finds the next available procedural ID around the given base, in the format '[mapID]_[base]_[number]'
     * @param base The base of the ID, e.g. a terrain might try to assign ID's with the base 'terrain' */
    public String nextAvailableProceduralID(String base) {
        int IDNumber = 0; //Each id consists of some identifier + a number; to make an id first we need to see if any objects currently have ids using the same pre-number portion (and then go one number higher)

        String baseID = this.getMapID() + "_" + base + "_"; // Identified as map + terrain + number

        // TODO: do we want to check the entity layer, or the entire base file, instead?
        for (Entity entity : this.getEntities()) {
            String entityID = entity.getID(); //Check each entity's id:
            if (entityID.startsWith(baseID) && entityID.length() > baseID.length()) { //If the first part of string matches (and there is a modifier)
                String modifier = entityID.substring(baseID.length()); // Find the modifier (most likely an id number)
                if (MiscFunctions.isInteger(modifier)) {
                    int entityIDNum = Integer.parseInt(modifier);
                    if (entityIDNum >= IDNumber) {
                        IDNumber = entityIDNum + 1;
                    }
                }
            }
        }
        String newID = baseID + IDNumber;
        return newID;
    }

    public void removeEntity(Entity toRemove) {
        this.entities.remove(toRemove);
    }

    public ArrayList<Collidable> getCollidables() {
        return collidables;
    }

    /**Clears the current collidables list, and refills it from the list of entities.*/
    public void updateCollisionList() {
        this.collidables.clear();
        for (Entity entity : this.entities) {
            if (entity instanceof Collidable coll) {
                this.collidables.add(coll);
            }
        }
    }

    public float getGravity() {
        return DEFAULT_GRAVITY;
    }
}

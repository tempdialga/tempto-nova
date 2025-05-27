package com.mygdx.tempto.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.tempto.TemptoNova;
import com.mygdx.tempto.gui.PauseMenu;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.input.UserInterface;
import com.mygdx.tempto.maps.WorldMap;
import com.mygdx.tempto.rendering.RendersToScreen;

import java.util.ArrayList;

public class GameScreen extends TemptoScreen {

    //////// Data about the game at hand //////////////////////////////////////////////////////////////////
    /**Everything that takes place inside the world itself. E.g., this would include terrain, the player, a popup right above a sign, but not a screen-static gui like the health bar or pause menu*/
    private WorldMap world;

    /**The things that might collect input, organized by priority. Only the highest will be told to collect input.
     * E.g., there's an input representing input to move the player, and an input representing the pause menu; the pause menu would normally be higher, and so the player would not be told to accept input
     *
     * (Note: UserInterface/UI and GUI sound kinda similar. The difference is that a UI represents anything the user can "interface" with to affect the game, which is a lot of things. Maybe that could be the player listening for input to move around, or it could be a menu that opened up and is listening for input to select a button. The menu would be a GUI, which is by definition also a UI, and so should also be included here)
     * */
    @Deprecated
    private ArrayList<UserInterface> inputStack;

    /**The central input processor of the game, to process input from GUI's, the player, what have you*/
    private InputTranslator gameInput;

    /**The input listener of the pause menu; should always take priority*/
    private PauseMenu pauseMenu;

    /////// Data/Utilities for rendering to the screen //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**Viewport representing screen coordinates, alongside {@link #overlayCamera}*/
    private Viewport overlayViewport;

    /**Camera to use to render to screen coordinates, alongside {@link #overlayViewport}. Screen coordinates are on the interval ([-1,1], [-1,1]), where (-1,-1) represents the bottom left corner.*/
    private OrthographicCamera overlayCamera;

    /**Batch that will be used to render overlays in screen coordinates*/
    private SpriteBatch overlayBatch;

    /**A list of things to be rendered on top of the world using {@link #overlayViewport} and {@link #overlayCamera}, such as the pause menu or a health bar*/
    private ArrayList<RendersToScreen> overlays;

    /**If true, avoids writing to file on next call of {@link #hide()}*/
    private boolean notSaving = false;

    /**Time elapsed since the most recent game screen was created*/
    public static float elapsedTime = 0;

    //////// Creating and showing the main game /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public GameScreen(TemptoNova parent) {
        super(parent);


        // Load Global save data //
            // Note: The WorldMap will handle stored data about the specific map being loaded; What we load here is the global stuff, like the general save file that says stuff like "we were last at this map"

            // What map to load (typically, this will be what the save file said the player was last at) //
        String testMap = "testmap";
        String demo = "hallway";

        // Load the world, with context of global save data //
        this.world = new WorldMap(demo, this);
        elapsedTime = 0;
    }
    @Override
    public void show() {
        // Prepare to process input //

            // Initiate input processing //
        this.gameInput = new InputTranslator();
        Gdx.input.setInputProcessor(this.gameInput); //Add main input processor
        Controllers.addListener(this.gameInput); //Add to controller input
            // Start the pause menu.
        this.pauseMenu = new PauseMenu(this.parent, this);
            // The pause menu will always be first in input, but will pass or block input from going past it based on whether it should be active //
        this.addPrimaryInput(this.pauseMenu);
            // After the pause menu comes the input given to the world
        this.addSecondaryInput(this.world.getWorldInput());

        // Prepare to render //

            //Initiate viewport, batch and camera that will be used for screen coordinates
        this.overlayBatch = new SpriteBatch();
        this.overlayCamera = new OrthographicCamera();
        this.overlayViewport = new FitViewport(TemptoNova.PIXEL_GAME_WIDTH,TemptoNova.PIXEL_GAME_HEIGHT); //2 units tall, 2*aspect ratio units wide (so units are equal in scale)
        System.out.println("Width: " + this.overlayViewport.getWorldWidth());
        System.out.println("Height: " + this.overlayViewport.getWorldHeight());
        this.overlayCamera.position.set(0,0,0); //Center at 0, thus making screen coordinates of ([-a,a], [-1,1]) where a is aspect ratio
        this.overlayCamera.setToOrtho(false, 2*TemptoNova.ASPECT_RATIO, 2);
        //this.overlayCamera.zoom = 1;
            //Initiate list of things to render on top of the world
        this.overlays = new ArrayList<>();
        this.overlays.add(this.pauseMenu); // Include tha pause menu in this list
    }

    //////// Input Processing ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Adds an input to the front of the input stack so that it gets "dibs" on input. E.g. opening the pause menu.
     * Not recommended for anything except the pause menu, might be changed to public if there turns out to be a use case for that.
     */
    private void addPrimaryInput(InputProcessor newInput) {
            // InputMultiplexer starts at index 0 and goes up until stopped; therefore, the primary input is index 0 //
        this.gameInput.addProcessor(0, newInput);
    }

    /**
     * Adds an input as the next input after the pause menu listener. Should be the case of most GUIs
     * */
    public void addSecondaryInput(InputProcessor newInput) {
            // Where the pause menu is (-1 if it's not in there for some reason) //
        int pauseMenuIndex = this.gameInput.getProcessors().indexOf(this.pauseMenu, true);
            // Put the new input right after the pause menu (if the pause menu wasn't there, this will put it at the front (index 0) //
        this.gameInput.addProcessor(pauseMenuIndex + 1, newInput);
    }

    /**
     * Removes the given input from the input stack. In most cases, this will be called by a menu that closes, to remove itself.
     * */
    public void removeInput(InputProcessor toRemove) {
        this.gameInput.removeProcessor(toRemove);
    }


    //////// Updating the game on a frame by frame basis ////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void render(float delta) {
        elapsedTime += delta;
        System.out.println("Frame time: " + delta + " seconds.");

        // Update processed input data like direction //
        InputTranslator.GameInputs.updateProcessedInput();

        // Update the 'physical' world with the change in time; If menu is open, freeze time //
        float worldTimeStep;
        if (this.pauseMenu.isVisible()) worldTimeStep = 0; else worldTimeStep = delta;
        this.world.update(worldTimeStep);

        // Render the 'physical' world to the screen
        this.world.render();

        // Render things like GUIs that are overlaid over the world
        this.overlayViewport.apply(); // Apply the viewport
        this.overlayCamera.position.set(0,0,0); // Center the camera
        this.overlayCamera.update();

        this.overlayBatch.setProjectionMatrix(this.overlayCamera.combined); // Apply the camera to the sprite batch

        this.overlayBatch.begin();
        this.world.renderToScreen(this.overlayBatch, this.overlayCamera, TemptoNova.ASPECT_RATIO); //Render anything the world needs to do using the screen camera
        for (RendersToScreen overlay : this.overlays) { // Render each overlay to the screen in one batch call
            overlay.renderToScreen(this.overlayBatch, this.overlayCamera, TemptoNova.ASPECT_RATIO);
        }
        this.overlayBatch.end();

    }

    //////// Persistent Data Handling ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**Saves applicable persistent game data. Should be called reasonably and regularly*/
    public void save(){
    }





    //////// Other Game Loop Logic ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void resize(int width, int height) {
        if (this.world != null) this.world.resizeWindow(width, height);
        this.overlayViewport.update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    public void doNotSave() {
        this.notSaving = true;
    }

    @Override
    public void hide() {
        if (this.notSaving) {
            System.out.println("Skipping next save!");
        } else {
            System.out.println("Writing to save file lmao");
            this.world.writeToSaveFile(); // Write to save file (Final game behavior)
            this.world.writeToCoreFile(); // Write to core map file (ONLY FOR EDITING)
        }
    }

    @Override
    public void dispose() {
        this.world.dispose();
        this.overlayBatch.dispose();
        this.pauseMenu.dispose();
    }

    //////// Getters and setters ///////////////////

    public WorldMap getWorld() {
        return world;
    }


}

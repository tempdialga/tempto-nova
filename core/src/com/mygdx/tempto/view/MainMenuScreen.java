package com.mygdx.tempto.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.tempto.TemptoNova;
import com.mygdx.tempto.gui.MainMenu;
import com.mygdx.tempto.gui.PauseMenu;
import com.mygdx.tempto.input.InputTranslator;
import com.mygdx.tempto.rendering.RendersToScreen;
import com.sun.tools.javac.Main;

public class MainMenuScreen extends TemptoScreen {

    //////// Input //////////////////

    InputTranslator menuTranslator;
    MainMenu menu;

    //////// Rendering /////////////

    FitViewport menuViewport; // Similar to those of GameScreen and Edit screen TODO: decide which viewport form is best for each screen
    OrthographicCamera menuCam;
    SpriteBatch menuBatch;


    public MainMenuScreen(TemptoNova parent) {
        super(parent);
        this.menu = new MainMenu(parent);
    }

    @Override
    public void show() {
        // Prepare to process input //

            // Initiate input processing //
        this.menuTranslator = new InputTranslator();
        Gdx.input.setInputProcessor(this.menuTranslator); //Add main input processor
        Controllers.addListener(this.menuTranslator); //Add to controller input
            // Start the pause menu.
        this.menu = new MainMenu(this.parent);
        this.menuTranslator.addProcessor(0, this.menu);

        // Prepare to render //

            //Initiate viewport, batch and camera that will be used for screen coordinates
        this.menuBatch = new SpriteBatch();
        this.menuCam = new OrthographicCamera();
        this.menuViewport = new FitViewport(2*TemptoNova.ASPECT_RATIO,2); //2 units tall, 2*aspect ratio units wide (so units are equal in scale)
        this.menuCam.position.set(0,0,0); //Center at 0, thus making screen coordinates of ([-a,a], [-1,1]) where a is aspect ratio
        this.menuCam.zoom = 1;
    }

    @Override
    public void render(float delta) {
        // Render things like GUIs that are overlaid over the world
        this.menuViewport.apply(); // Apply the viewport
        this.menuCam.position.set(0,0,0); // Center the camera

        this.menuBatch.setProjectionMatrix(this.menuCam.combined); // Apply the camera to the sprite batch

        this.menuBatch.begin();
        this.menu.renderToScreen(this.menuBatch, this.menuCam, TemptoNova.ASPECT_RATIO);
        this.menuBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        this.menuViewport.update(width, height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}

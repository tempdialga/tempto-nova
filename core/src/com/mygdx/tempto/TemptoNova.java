package com.mygdx.tempto;

import com.badlogic.gdx.Game;
import com.mygdx.tempto.view.EditScreen;
import com.mygdx.tempto.view.GameScreen;
import com.mygdx.tempto.view.LoadingScreen;
import com.mygdx.tempto.view.MainMenuScreen;
import com.mygdx.tempto.view.TemptoScreen;

public class TemptoNova extends Game {

	//////// General Game Constants /////////

	/**Width and height of the game shown on screen. Not necessarily the dimensions of the window; they will be expanded and letterboxed to fit the window it is displayed in.*/
	public static final int PIXEL_GAME_WIDTH = 520, PIXEL_GAME_HEIGHT = 200;
	/**The intended ratio between width and height of the primary game window. Used for things like rendering GUIs*/
	public static final float ASPECT_RATIO = PIXEL_GAME_WIDTH/((float) PIXEL_GAME_HEIGHT);

	//////// Screens ////////////////

	public static final int LOADING_SCREEN = 0, MAIN_MENU_SCREEN = 1, GAME_SCREEN = 2, EDIT_SCREEN = 3;
	private TemptoScreen currentScreen;
	private int screenType;


	@Override
	public void create () {
		this.switchScreen(MAIN_MENU_SCREEN);
	}
	
	@Override
	public void dispose () {
		this.currentScreen.dispose();
	}

	public void switchScreen(int screenType) { // When told to switch to a given screen:
		boolean screenValid = true;

		// Keep track of what the screen just was, in case it actually changes
		TemptoScreen lastScreen = this.currentScreen;

		// Try to switch to a corresponding screen //
		switch (screenType) {
			case LOADING_SCREEN -> this.currentScreen = new LoadingScreen(this);
			case MAIN_MENU_SCREEN -> this.currentScreen = new MainMenuScreen(this);
			case GAME_SCREEN -> this.currentScreen = new GameScreen(this);
			case EDIT_SCREEN -> this.currentScreen = new EditScreen(this);
			default -> screenValid = false; // If haven't changed screen, don't change the screen type
		}

		if (screenValid) this.screenType = screenType; // If a new screen was made, change the screen type
		this.setScreen(this.currentScreen); // Update the application on what screen is currently being used

		// In the current model, screens will not be saved and reused. In the future this may change, but for now, if the screen is changed, the old screen is disposed
		if (lastScreen != this.currentScreen && lastScreen != null) {// If the screen changed:
			lastScreen.hide(); //Hide the screen
			lastScreen.dispose(); //Dispose of the screen, since it will no longer be accessible. (this might change if we decide to hold onto screens after switching)
		}
	}


}

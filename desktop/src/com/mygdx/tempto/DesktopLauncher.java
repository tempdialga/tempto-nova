package com.mygdx.tempto;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mygdx.tempto.TemptoNova;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		config.setWindowedMode(TemptoNova.PIXEL_GAME_WIDTH*2, TemptoNova.PIXEL_GAME_HEIGHT*2);
		config.setTitle("TemptoNova");
		new Lwjgl3Application(new TemptoNova(), config);
	}
}

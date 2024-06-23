package com.mygdx.tempto;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.mygdx.tempto.editing.TmxMapWriter;
import com.mygdx.tempto.maps.WorldMap;

public class TemptoNova extends ApplicationAdapter {
	SpriteBatch batch;
	Texture img;
	WorldMap testMap;
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		img = new Texture("badlogic.jpg");
		FileHandle file = Gdx.files.local("myfile.txt");
		file.writeString("My god, it's full of stars", false);

		testMap = new WorldMap("loading_test");
		TmxMapWriter testWriter = new TmxMapWriter();


	}

	@Override
	public void render () {
		ScreenUtils.clear(1, 0, 0, 1);
//		batch.begin();
//		batch.draw(img, 0, 0);
//		batch.end();
		float deltaTime = Gdx.graphics.getDeltaTime();
		testMap.update(deltaTime);

		testMap.render();
	}

	/**Saves applicable persistent game data. Should be called reasonably and regularly*/
	public void save(){
		//Save preferences
		testMap.writeToFile();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();
	}


}

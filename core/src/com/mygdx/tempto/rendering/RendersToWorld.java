package com.mygdx.tempto.rendering;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**An interface entities or other items should implement in order to be rendered using world coordinates.*/
public interface RendersToWorld {

    void renderToWorld(SpriteBatch batch, OrthographicCamera worldCamera);
}

package com.mygdx.tempto.rendering;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**An interface for drawing directly to the screen, using screen coordinates. This method will be called outside of any context of the world or its coordinates.*/
public interface RendersToScreen {
    /**Called to render something to screen coordinates, with the batch already started.
     * Rendering should take place in coordinates ([-aspectRatio:aspectRatio], [-1:1]) with the center at (0,0), but the camera is provided for reference.
     * @param batch A {@link SpriteBatch} which is currently in a drawing state (i.e. between calling {@link SpriteBatch#begin()} and {@link SpriteBatch#end()})
     * @param screenCamera An {@link OrthographicCamera}
     * @param aspectRatio The ratio between width and height of the window. This way, the coordinates of the screen are centered, but stay equal scales.
     * */
    void renderToScreen(SpriteBatch batch, OrthographicCamera screenCamera, float aspectRatio);
}

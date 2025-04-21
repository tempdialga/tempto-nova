package com.mygdx.tempto.rendering;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**An interface entities or other items should implement in order to be rendered using world coordinates.*/
public interface RendersToWorld {

    void renderToWorld(SpriteBatch batch, OrthographicCamera worldCamera);

    /**Renders this item to an active FrameBuffer representing the depth map of the visible world, with the following channels:
     * r - Depth (reciprocal, 1/pixel) The distance from the camera*/
    default void renderToDepthMap(SpriteBatch depthBatch, OrthographicCamera worldCamera) {

    }
}

package com.mygdx.tempto.gui.text;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class TextUtils {
    public static void renderTextCentered(Rectangle area, BitmapFont font, String text, float idealTextHeight, SpriteBatch batch, OrthographicCamera camera) {
        //First, set the scale
        font.getData().setScale(idealTextHeight * font.getScaleY() / font.getLineHeight());
        //Prepare to render really far offscreen to check how it'll actually look dimension wise
        GlyphLayout testRender = font.draw(batch, text, camera.position.x+999999, camera.position.y+999999);

        //Using those found dimensions, calculate where to put the text
        float actualHeight = testRender.height;
        float textVertPadding = 0.5f*(area.height-actualHeight);
        //System.out.println("Vertical text padding: " + textVertPadding + ", vs button and text height: " + BAR_HEIGHT + ", " + textHeight);
        //System.out.println("Text final height: " + testRender.height);
        float textLeftPadding = (area.width - testRender.width)*0.5f; //How far from the left of the button this text should be to be centered
        float textX = area.x + textLeftPadding;
        float textY = area.y + area.height - textVertPadding;

        //Render
        font.draw(batch, text, textX, textY);
    }
}

package com.mygdx.tempto.view;

import com.badlogic.gdx.Screen;
import com.mygdx.tempto.TemptoNova;

/**An implementation of {@link Screen} that Tempto's screens use*/
public abstract class TemptoScreen implements Screen {
    /**The parent application*/
    protected TemptoNova parent;

    public TemptoScreen(TemptoNova parent) {
        this.parent = parent;
    }

    public TemptoNova getParent() {
        return parent;
    }

    public void setParent(TemptoNova parent) {
        this.parent = parent;
    }
}

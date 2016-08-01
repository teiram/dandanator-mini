package com.grelobites.romgenerator.util.gamerenderer;

import com.grelobites.romgenerator.util.gamerenderer.renderers.ScreenshotGameRenderer;

public enum GameRendererType {
    SCREENSHOT(ScreenshotGameRenderer.class);

    private Class<? extends GameRenderer> renderer;

    GameRendererType(Class<? extends GameRenderer> renderer) {
        this.renderer = renderer;
    }

    public static GameRendererType fromString(String type) {
        return GameRendererType.valueOf(type.toUpperCase());
    }

    public Class<? extends GameRenderer> renderer() {
        return renderer;
    }
}

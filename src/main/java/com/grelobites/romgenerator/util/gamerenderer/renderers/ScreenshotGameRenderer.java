package com.grelobites.romgenerator.util.gamerenderer.renderers;


import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.gamerenderer.GameRenderer;
import com.grelobites.romgenerator.util.gamerenderer.PassiveGameRendererBase;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import java.io.IOException;

public class ScreenshotGameRenderer extends PassiveGameRendererBase implements GameRenderer  {

    private WritableImage spectrum48kImage;
    private ImageView targetView;

    private void initializeImages() throws IOException {
        spectrum48kImage = ImageUtil.scrLoader(
                ImageUtil.newScreenshot(),
                ScreenshotGameRenderer.class.getClassLoader()
                        .getResourceAsStream("sinclair-1982.scr"));
    }

    public ScreenshotGameRenderer() throws IOException {
        initializeImages();
    }

    @Override
    public void setTarget(ImageView imageView) {
        this.targetView = imageView;
        previewGame(null);
    }

    @Override
    public void previewGame(Game game) {
        if (game != null && game instanceof RamGame) {
            targetView.setImage(((RamGame) game).getScreenshot());
        } else {
            targetView.setImage(spectrum48kImage);
        }
    }
}

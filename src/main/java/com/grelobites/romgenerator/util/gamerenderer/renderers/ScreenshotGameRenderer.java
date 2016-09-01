package com.grelobites.romgenerator.util.gamerenderer.renderers;


import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.gamerenderer.GameRenderer;
import com.grelobites.romgenerator.util.gamerenderer.PassiveGameRendererBase;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import java.io.IOException;

public class ScreenshotGameRenderer extends PassiveGameRendererBase implements GameRenderer  {

    private WritableImage spectrum48kImage;
    private WritableImage cartridgeImage;
    private ImageView targetView;

    private void initializeImages() throws IOException {
        spectrum48kImage = ImageUtil.scrLoader(
                ImageUtil.newScreenshot(),
                ScreenshotGameRenderer.class.getClassLoader()
                        .getResourceAsStream("sinclair-1982.scr"));
        cartridgeImage = ImageUtil.scrLoader(
                ImageUtil.newScreenshot(),
                ScreenshotGameRenderer.class.getClassLoader()
                        .getResourceAsStream("3carts.scr"));
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
        if (game != null) {
            if (game instanceof RamGame) {
                targetView.setImage(((RamGame) game).getScreenshot());
            } else if (game.getType() == GameType.ROM) {
                targetView.setImage(cartridgeImage);
            } else {
                targetView.setImage(spectrum48kImage);
            }
        } else {
            targetView.setImage(spectrum48kImage);
        }
    }
}

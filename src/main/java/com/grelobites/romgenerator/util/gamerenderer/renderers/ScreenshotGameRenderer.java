package com.grelobites.romgenerator.util.gamerenderer.renderers;


import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.model.*;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.gamerenderer.GameRenderer;
import com.grelobites.romgenerator.util.gamerenderer.PassiveGameRendererBase;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class ScreenshotGameRenderer extends PassiveGameRendererBase implements GameRenderer  {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotGameRenderer.class);
    private WritableImage spectrum48kImage;
    private WritableImage cartridgeImage;
    private ImageView targetView;

    private void initializeImages() throws IOException {
        setDefaultImage(Configuration.getInstance().getTapLoaderTarget(), false);
        cartridgeImage = ImageUtil.scrLoader(
                ImageUtil.newScreenshot(),
                ScreenshotGameRenderer.class.getClassLoader()
                        .getResourceAsStream("3carts.scr"));
    }

    private void loadDefaultImage(String imageResource) {
        LOGGER.debug("Loading default image " + imageResource);
        try {
            spectrum48kImage = ImageUtil.scrLoader(
                    ImageUtil.newScreenshot(),
                    ScreenshotGameRenderer.class.getClassLoader()
                            .getResourceAsStream(imageResource));
        } catch (IOException ioe) {
            LOGGER.error("Loading screenshot image resource", ioe);
        }
    }

    private void setDefaultImage(String tapLoaderTarget, boolean update) {
        switch (HardwareMode.valueOf(tapLoaderTarget)) {
            case HW_128K:
                loadDefaultImage("128k.scr");
                break;
            case HW_PLUS2A:
                loadDefaultImage("plus2a.scr");
                break;
            default:
                loadDefaultImage("sinclair-1982.scr");
        }
        if (update) {
            targetView.setImage(spectrum48kImage);
        }
    }

    public ScreenshotGameRenderer() throws IOException {
        initializeImages();
        Configuration.getInstance().tapLoaderTargetProperty()
                .addListener((observable, oldValue, newValue) -> {
            setDefaultImage(newValue, targetView.getImage() == spectrum48kImage);
        });
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

    @Override
    public void loadImage(InputStream resource) throws IOException {
        targetView.setImage(ImageUtil.scrLoader(
                ImageUtil.newScreenshot(), resource));
    }
}

package com.grelobites.romgenerator.util.gamerenderer;


import com.grelobites.romgenerator.model.Game;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.io.InputStream;

public interface GameRenderer {
    void setTarget(ImageView imageView);
    void start();
    void stop();
    void pause();
    void resume();
    void previewGame(Game game);
    void loadImage(InputStream resource) throws IOException;
}

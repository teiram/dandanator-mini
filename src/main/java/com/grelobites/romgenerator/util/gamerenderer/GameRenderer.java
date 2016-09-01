package com.grelobites.romgenerator.util.gamerenderer;


import com.grelobites.romgenerator.model.Game;
import javafx.scene.image.ImageView;

public interface GameRenderer {
    void setTarget(ImageView imageView);
    void start();
    void stop();
    void pause();
    void resume();
    void previewGame(Game game);
}

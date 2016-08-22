package com.grelobites.romgenerator.handlers.dandanatormini.model;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.PositionAwareInputStream;

import java.io.IOException;

public interface GameMapper {

    Game createGame();
    void populateGameSlots(PositionAwareInputStream is) throws IOException;
}

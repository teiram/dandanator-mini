package com.grelobites.dandanator;

import com.grelobites.dandanator.Configuration;
import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.util.romset.RomSetHandler;
import com.grelobites.dandanator.util.romset.RomSetHandlerFactory;
import javafx.collections.ObservableList;

import java.util.Collection;

public class Context {
    private Configuration configuration;
    private ObservableList<Game> gameList;
    private RomSetHandler romSetHandler;

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public ObservableList<Game> getGameList() {
        return gameList;
    }

    public void setGameList(ObservableList<Game> gameList) {
        this.gameList = gameList;
    }

    public RomSetHandler getRomSetHandler() {
        if (romSetHandler == null) {
            romSetHandler = RomSetHandlerFactory.getHandler(configuration.getMode());
        }
        return romSetHandler;
    }
}

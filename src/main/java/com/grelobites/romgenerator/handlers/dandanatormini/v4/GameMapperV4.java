package com.grelobites.romgenerator.handlers.dandanatormini.v4;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.model.GameMapper;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.model.TrainerList;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GameMapperV4 implements GameMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameMapperV4.class);

    private boolean holdScreen;
    private boolean activeRom;
    private String name;
    private GameHeader gameHeader;
    private List<byte[]> gameSlots;
    private TrainerList trainerList = new TrainerList(null);
    private int trainerCount = 0;

    void setHoldScreen(int holdScreenByte) {
        holdScreen = holdScreenByte != 0;
    }

    void setActiveRom(int activeRomByte) {
        activeRom = activeRomByte != 0;
    }

    void readHeader(InputStream is) throws IOException {
        gameHeader = GameHeaderV4Serializer.deserialize(is);
    }

    @Override
    public void populateGameSlots(PositionAwareInputStream is) throws IOException {
        gameSlots = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            gameSlots.add(Util.fromInputStream(is, Constants.SLOT_SIZE));
        }
    }

    void readName(InputStream is) throws IOException {
        name = Util.getNullTerminatedString(is, 3, 33);
        LOGGER.debug("Name read as " + name);
    }

    TrainerList getTrainerList() {
        return trainerList;
    }

    public int getTrainerCount() {
        return trainerCount;
    }

    public void setTrainerCount(int trainerCount) {
        this.trainerCount = trainerCount;
    }

    public List<byte[]> getGameSlots() {
        if (gameSlots != null) {
            return gameSlots;
        } else {
            throw new IllegalStateException("Game slots not set");
        }
    }

    public void exportTrainers(RamGame game) {
        trainerList.setOwner(game);
        game.setTrainerList(trainerList);
    }

    @Override
    public Game createGame() {
        final RamGame game = new RamGame(GameType.RAM48, getGameSlots());
        game.setName(name);
        game.setHoldScreen(holdScreen);
        game.setRom(activeRom);
        game.setGameHeader(gameHeader);
        exportTrainers(game);
        return game;
    }
}

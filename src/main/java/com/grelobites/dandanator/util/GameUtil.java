package com.grelobites.dandanator.util;

import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.util.gameloader.GameImageLoader;
import com.grelobites.dandanator.util.gameloader.GameImageLoaderFactory;
import com.grelobites.dandanator.util.pokeimporter.PokeImporter;
import com.grelobites.dandanator.util.pokeimporter.PokeImporterFactory;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.IntStream;

public class GameUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameUtil.class);

    private static Optional<String> getFileExtension(String fileName) {
        int index;
        if ((index = fileName.lastIndexOf('.')) > -1) {
            return Optional.of(fileName.substring(index + 1));
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Game> createGameFromFile(File file) {
        LOGGER.debug("createGameFromFile " + file);
        try {
            if (file.canRead() && file.isFile()) {
                InputStream is = new FileInputStream(file);

                GameImageLoader loader = getFileExtension(file.getName())
                        .map(GameImageLoaderFactory::getLoader)
                        .orElseGet(GameImageLoaderFactory::getDefaultLoader);

                byte[] snaStream = loader.load(is);
                Game game = new Game();
                game.setData(snaStream);
                game.setName(getGameName(file));
                game.setRom(false);
                game.setScreen(false);
                return Optional.of(game);
            }
        } catch (Exception e) {
            LOGGER.error("Creating game from file " + file, e);
        }
        return Optional.empty();
    }

    public static String getGameName(File file) {
        String fileName = file.getName();
        int extensionIndex;
        if ((extensionIndex = fileName.lastIndexOf('.')) > 0) {
            fileName = fileName.substring(0, extensionIndex);
        }
        if (fileName.length() > Constants.GAMENAME_SIZE) {
            fileName = fileName.substring(0, Constants.GAMENAME_SIZE);
        }
        return fileName;
    }

    public static int getGamePokeSizeUsage(Game game) {
        return game.getTrainerList().getChildren().stream()
                .flatMapToInt(g -> IntStream.builder()
                        .add(Constants.GAMENAME_SIZE)
                        .add(g.getChildren().size() * Constants.POKE_ENTRY_SIZE)
                        .build()).sum();
    }

    public static double getOverallPokeUsage(ObservableList<Game> gameList) {
        int usedSize = Constants.POKE_HEADER_SIZE
                + gameList.stream().mapToInt(GameUtil::getGamePokeSizeUsage).sum();
        LOGGER.debug("Used size is " + usedSize + " from " + Constants.POKE_ZONE_SIZE);
        return ((double) usedSize) / Constants.POKE_ZONE_SIZE;
    }

    public static void importPokesFromFile(Game game, File pokeFile) throws IOException {
        PokeImporter importer = getFileExtension(pokeFile.getName())
                .map(PokeImporterFactory::getImporter)
                .orElseGet(PokeImporterFactory::getDefaultImporter);

        importer.importPokes(game.getTrainerList(), new FileInputStream(pokeFile));
    }
}

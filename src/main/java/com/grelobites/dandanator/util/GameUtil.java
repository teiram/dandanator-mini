package com.grelobites.dandanator.util;

import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.model.PokeNode;
import com.grelobites.dandanator.model.poke.pok.PokPoke;
import com.grelobites.dandanator.model.poke.pok.PokTrainer;
import com.grelobites.dandanator.model.poke.pok.PokValue;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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

    public static void createRomSet(File romSetFile, ObservableList<Game> gameList) throws IOException {
        LOGGER.debug("Creating ROM from " + gameList + " on " + romSetFile);
        byte[] romSetBytes = RomSetBuilder.newInstance()
                .withGames(gameList)
                .build();
        Files.write(romSetFile.toPath(), romSetBytes);
    }

    public static int getGamePokeSizeUsage(Game game) {
        return game.getPokes().getChildren().stream()
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
        PokPoke poke = PokPoke.fromInputStream(new FileInputStream(pokeFile));
        for (PokTrainer trainer: poke.getTrainers()) {
            PokeNode node = game.getPokes().addPokeNode(trainer.getName());
            trainer.getPokeValues().stream()
                    .filter(PokValue::isCompatibleSpectrum48K)
                    .filter(pokeValue -> !pokeValue.isInteractive())
                    .forEach(pokeValue -> node.addAddressValue(pokeValue.getAddress(),
                            pokeValue.getValue()));
        }

    }
}

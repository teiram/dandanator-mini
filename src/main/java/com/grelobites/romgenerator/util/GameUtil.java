package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.GameImageLoaderFactory;
import com.grelobites.romgenerator.util.gameloader.GameImageType;
import com.grelobites.romgenerator.util.pokeimporter.ImportContext;
import com.grelobites.romgenerator.util.pokeimporter.PokeImporter;
import com.grelobites.romgenerator.util.pokeimporter.PokeImporterFactory;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.IntStream;

public class GameUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameUtil.class);

    public static Optional<Game> createGameFromFile(File file) {
        LOGGER.debug("createGameFromFile " + file);
        try {
            if (file.canRead() && file.isFile()) {
                InputStream is = new FileInputStream(file);

                GameImageLoader loader = Util.getFileExtension(file.getName())
                        .map(GameImageLoaderFactory::getLoader)
                        .orElseGet(GameImageLoaderFactory::getDefaultLoader);

                Game game = loader.load(is);
                game.setName(getGameName(file));
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
        return filterGameName(fileName);
    }

    private static boolean allowedGameNameChar(Integer character) {
        return character > 31 && character < 127;
    }

    public static String filterGameName(String name) {
        return name.codePoints().map(c -> allowedGameNameChar(c) ? c : '_')
                .limit(DandanatorMiniConstants.GAMENAME_EFFECTIVE_SIZE)
                .collect(StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString();
    }

    public static int getGamePokeSizeUsage(Game game) {
        if (game.getType() != GameType.ROM) {
            RamGame ramGame = (RamGame) game;
            return ramGame.getTrainerList().getChildren().stream()
                    .flatMapToInt(g -> IntStream.builder()
                            .add(DandanatorMiniConstants.POKE_NAME_SIZE)
                            .add(1) //Number of pokes byte
                            .add(g.getChildren().size() * DandanatorMiniConstants.POKE_ENTRY_SIZE)
                            .build()).sum();
        } else {
            return 0;
        }
    }

    public static double getOverallPokeUsage(ObservableList<Game> gameList) {
        int usedSize = gameList.stream().mapToInt(GameUtil::getGamePokeSizeUsage).sum();
        int totalSize = DandanatorMiniConstants.POKE_ZONE_SIZE - DandanatorMiniConstants.POKE_HEADER_SIZE;
        LOGGER.debug("Used size is " + usedSize + " from " + totalSize);
        return ((double) usedSize) / totalSize;
    }

    public static void importPokesFromFile(RamGame game, ImportContext ctx) throws IOException {
        PokeImporter importer = Util.getFileExtension(ctx.getPokesFile().getName())
                .map(PokeImporterFactory::getImporter)
                .orElseGet(PokeImporterFactory::getDefaultImporter);

        importer.importPokes(game.getTrainerList(), ctx);
    }

    public static void exportPokesToFile(RamGame game, File pokeFile) throws IOException {
        PokeImporter importer = Util.getFileExtension(pokeFile.getName())
                .map(PokeImporterFactory::getImporter)
                .orElseGet(PokeImporterFactory::getDefaultImporter);
        try (FileOutputStream fos = new FileOutputStream(pokeFile)) {
            importer.exportPokes(game.getTrainerList(), fos);
        }
    }

    public static void exportGameAsSNA(Game selectedGame, File saveFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            GameImageLoaderFactory.getLoader(GameImageType.SNA)
                    .save(selectedGame, fos);
        }
    }

    public static boolean gameHasPokes(Game game) {
        return game instanceof RamGame && ((RamGame) game).hasPokes();
    }

    public static int getGameAddressValue(Game game, int address) {
        int slot = address / Constants.SLOT_SIZE;
        int offset = address % Constants.SLOT_SIZE;
        if (slot < game.getSlotCount()) {
            return Byte.toUnsignedInt(game.getSlot(slot)[offset]);
        } else {
            throw new IllegalArgumentException("Requesting past end address for game " + game.getName());
        }
    }


}

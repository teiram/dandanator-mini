package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.GameImageLoaderFactory;
import com.grelobites.romgenerator.util.pokeimporter.ImportContext;
import com.grelobites.romgenerator.util.pokeimporter.PokeImporter;
import com.grelobites.romgenerator.util.pokeimporter.PokeImporterFactory;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
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
        return filterGameName(fileName);
    }

    private static boolean allowedGameNameChar(Integer character) {
        return character > 31 && character < 127;
    }

    public static String filterGameName(String name) {
        return name.codePoints().map(c -> allowedGameNameChar(c) ? c : '_')
                .limit(DandanatorMiniConstants.GAMENAME_SIZE)
                .collect(StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString();
    }

    public static int getGamePokeSizeUsage(Game game) {
        return game.getTrainerList().getChildren().stream()
                .flatMapToInt(g -> IntStream.builder()
                        .add(DandanatorMiniConstants.POKE_NAME_SIZE)
                        .add(1) //Number of pokes byte
                        .add(g.getChildren().size() * DandanatorMiniConstants.POKE_ENTRY_SIZE)
                        .build()).sum();
    }

    public static double getOverallPokeUsage(ObservableList<Game> gameList) {
        int usedSize = DandanatorMiniConstants.POKE_HEADER_SIZE
                + gameList.stream().mapToInt(GameUtil::getGamePokeSizeUsage).sum();
        LOGGER.debug("Used size is " + usedSize + " from " + DandanatorMiniConstants.POKE_ZONE_SIZE);
        return ((double) usedSize) / DandanatorMiniConstants.POKE_ZONE_SIZE;
    }

    public static void importPokesFromFile(Game game, ImportContext ctx) throws IOException {
        PokeImporter importer = Util.getFileExtension(ctx.getPokesFile().getName())
                .map(PokeImporterFactory::getImporter)
                .orElseGet(PokeImporterFactory::getDefaultImporter);

        importer.importPokes(game.getTrainerList(), ctx);
    }

    public static void exportPokesToFile(Game game, File pokeFile) throws IOException {
        PokeImporter importer = Util.getFileExtension(pokeFile.getName())
                .map(PokeImporterFactory::getImporter)
                .orElseGet(PokeImporterFactory::getDefaultImporter);
        importer.exportPokes(game.getTrainerList(), new FileOutputStream(pokeFile));
    }

    public static void exportGameAsSNA(Game selectedGame, File saveFile) throws IOException {
        Files.write(saveFile.toPath(), selectedGame.getData());
    }
}

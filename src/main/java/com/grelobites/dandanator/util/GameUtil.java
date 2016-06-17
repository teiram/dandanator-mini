package com.grelobites.dandanator.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.model.Game;

public class GameUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameUtil.class);
	
	public static Optional<Game> createGameFromFile(File file) {
		LOGGER.debug("createGameFromFile " + file);
		try {
			if (file.canRead() && file.isFile()) {
				byte[] sna = Files.readAllBytes(file.toPath());
				Game game = new Game();
				game.setData(sna);
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
}

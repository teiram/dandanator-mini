package com.grelobites.dandanator.util;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

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
		fileName = fileName.substring(0, fileName.lastIndexOf('.'));
		if (fileName.length() > Constants.GAMENAME_SIZE) {
			fileName = fileName.substring(0, Constants.GAMENAME_SIZE);
		}
		return fileName;
	}
}

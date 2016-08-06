package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.ImageUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class BaseGame {
	private static final Logger LOGGER = LoggerFactory.getLogger(BaseGame.class);

	protected StringProperty name;
	protected Image screenshot;
	protected final List<byte[]> data;
    private final GameType gameType;

	public BaseGame(GameType gameType, List<byte[]> data) {
	    this.gameType = gameType;
        this.data = data;
		name = new SimpleStringProperty();
	}

	public String getName() {
		return name.get();
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public StringProperty nameProperty() {
		return this.name;
	}

	public GameType getType() {
	    return gameType;
    }

    public int getSlotCount() {
        return data.size();
    }

    public byte[] getSlot(int slot) {
        return data.get(slot);
    }
}

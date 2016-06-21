package com.grelobites.dandanator.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.util.ImageUtil;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

public class Game {
	private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);
	
	private StringProperty name;
	private BooleanProperty rom;
	private BooleanProperty screen;
	private Image screenshot;
	private byte[] data;
	private PokeEntity pokes;
	
	public Game() {
		name = new SimpleStringProperty();
		rom = new SimpleBooleanProperty();
		screen = new SimpleBooleanProperty();
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

	public boolean getRom() {
		return rom.get();
	}
	
	public void setRom(boolean rom) {
		this.rom.set(rom);
	}
	
	public BooleanProperty romProperty() {
		return this.rom;
	}
	
	public boolean getScreen() {
		return screen.get();
	}
	
	public void setScreen(boolean screen) {
		this.screen.set(screen);
	}
	
	public BooleanProperty screenProperty() {
		return this.screen;
	}
	
	public Image getScreenshot() {
		if (screenshot == null) {
			try {
				screenshot = ImageUtil
						.scrLoader(ImageUtil.newScreenshot(), 
								new ByteArrayInputStream(data, 
										Constants.SNA_HEADER_SIZE,
										Constants.SPECTRUM_FULLSCREEN_SIZE));
			} catch (Exception e) {
				LOGGER.error("Loading screenshot", e);
			}
		}
		return screenshot;
	}
	
	public void setScreenshot(Image screenshot) {
		this.screenshot = screenshot;
	}
	
	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	public InputStream getDataStream() {
		return new ByteArrayInputStream(data);
	}

	public PokeEntity getPokes() {
        if (pokes == null) {
            pokes = new PokeEntity(this);
        }
        return pokes;
    }

	public void setPokes(PokeEntity pokes) {
		this.pokes = pokes;
	}

	public void addPoke(String pokeName) {
		getPokes().addPokeNode(pokeName);
	}

    public boolean hasPokes() {
        return pokes.getChildren().size() > 0;
    }
}

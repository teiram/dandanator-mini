package com.grelobites.romgenerator.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javafx.beans.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.ImageUtil;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

public interface Game {

    GameType getType();

	String getName();
	
    void setName(String name);
	
	StringProperty nameProperty();

    boolean isCompressible();

    byte[] getSlot(int slot);

    int getSlotCount();

    Observable[] getObservable();

    boolean isSlotZeroed(int slot);

    int getSize();

}

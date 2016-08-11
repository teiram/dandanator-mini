package com.grelobites.romgenerator.util.romsethandler;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.view.ApplicationContext;
import javafx.beans.property.BooleanProperty;

import java.io.InputStream;
import java.io.OutputStream;

public interface RomSetHandler {

    void bind(ApplicationContext applicationContext);
    void unbind();

    void exportRomSet(OutputStream os);

    void importRomSet(InputStream is);

    void updateMenuPreview();

    BooleanProperty generationAllowedProperty();
    boolean addGame(Game game);
}

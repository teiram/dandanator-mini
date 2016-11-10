package com.grelobites.romgenerator.util.romsethandler;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.ApplicationContext;
import javafx.beans.property.BooleanProperty;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;

public interface RomSetHandler {

    RomSetHandlerType type();
    void bind(ApplicationContext applicationContext);
    void unbind();

    void exportRomSet(OutputStream os);

    void importRomSet(InputStream is);

    void mergeRomSet(InputStream is);

    void updateMenuPreview();

    BooleanProperty generationAllowedProperty();
    Future<OperationResult> addGame(Game game);
    void removeGame(Game game);
}

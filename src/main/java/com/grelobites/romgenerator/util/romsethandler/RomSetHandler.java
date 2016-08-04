package com.grelobites.romgenerator.util.romsethandler;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.view.MainAppController;

import java.io.InputStream;
import java.io.OutputStream;

public interface RomSetHandler {

    void bind(MainAppController controller);
    void unbind();

    void exportRomSet(OutputStream os);

    void importRomSet(InputStream is);

    void updateMenuPreview();

    boolean addGame(Game game);
}

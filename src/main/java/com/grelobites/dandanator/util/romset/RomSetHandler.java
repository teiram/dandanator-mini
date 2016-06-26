package com.grelobites.dandanator.util.romset;

import com.grelobites.dandanator.Context;
import com.grelobites.dandanator.util.ZxScreen;

import java.io.InputStream;
import java.io.OutputStream;

public interface RomSetHandler {

    void createRomSet(Context context, OutputStream os);

    void importRomSet(Context context, InputStream is);

    void updateScreen(Context context, ZxScreen screen);
}

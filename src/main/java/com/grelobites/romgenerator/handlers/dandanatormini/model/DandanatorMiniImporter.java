package com.grelobites.romgenerator.handlers.dandanatormini.model;

import com.grelobites.romgenerator.handlers.dandanatormini.model.SlotZero;
import com.grelobites.romgenerator.view.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;

public interface DandanatorMiniImporter {
    void importRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext)
        throws IOException;
}

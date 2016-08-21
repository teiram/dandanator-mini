package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.view.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;

public interface DandanatorMiniImporter {
    void importRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext)
        throws IOException;
}

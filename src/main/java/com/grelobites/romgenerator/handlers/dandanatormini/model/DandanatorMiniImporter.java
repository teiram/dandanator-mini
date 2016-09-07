package com.grelobites.romgenerator.handlers.dandanatormini.model;

import com.grelobites.romgenerator.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;

public interface DandanatorMiniImporter {
    void importRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext,
                      DandanatorConfigurationSetter configurationSetter)
        throws IOException;

    void mergeRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext)
        throws IOException;
}

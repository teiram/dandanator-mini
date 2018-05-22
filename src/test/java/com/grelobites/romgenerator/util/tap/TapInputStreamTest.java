package com.grelobites.romgenerator.util.tap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static junit.framework.TestCase.assertNotNull;

public class TapInputStreamTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TapInputStreamTest.class);

    @Test
    public void traverseDaadTap() throws IOException {
        InputStream tap = TapInputStreamTest.class.getResourceAsStream("/daad.tap");
        assertNotNull(tap);
        TapInputStream tis = new TapInputStream(tap);
        Optional<TapBlock> tapBlockOpt;
        while ((tapBlockOpt = tis.next()).isPresent()) {
            LOGGER.debug("Read TAP Block {}", tapBlockOpt.get());
        }
    }

}

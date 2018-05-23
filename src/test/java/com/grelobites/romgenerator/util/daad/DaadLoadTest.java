package com.grelobites.romgenerator.util.daad;

import com.grelobites.romgenerator.util.Util;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static junit.framework.TestCase.assertNotNull;

public class DaadLoadTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DaadLoadTest.class);

    @Test
    public void zipInspection() throws IOException {
        InputStream daad = DaadLoadTest.class.getResourceAsStream("/daad.zip");
        assertNotNull(daad);
        try (ZipInputStream zip = new ZipInputStream(daad)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                LOGGER.debug("Got zip entry {} with size {}/{}",
                        entry.getName(), entry.getCompressedSize(), entry.getSize());
                byte[] data = Util.fromInputStream(zip);
                zip.closeEntry();
                LOGGER.debug("Got a byte array of {} bytes from entry", data.length);
            }
        }
    }

    @Test
    public void daadLoader() throws IOException {
        InputStream daad = DaadLoadTest.class.getResourceAsStream("/daad.zip");
        assertNotNull(daad);
        try (ZipInputStream zip = new ZipInputStream(daad)) {
            DAADData data = DAADData.fromZipStream(zip);
            for (int i = 0; i < 3; i++) {
                DAADBinary part = data.getBinaryPart(i);
                LOGGER.debug("Binary part at {} with size {}", part.getLoadAddress(),
                        part.getData().length);
            }
        }
    }


}

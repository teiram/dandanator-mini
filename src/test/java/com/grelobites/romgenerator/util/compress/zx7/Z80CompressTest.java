package com.grelobites.romgenerator.util.compress.zx7;


import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.Z80CompressedInputStream;
import com.grelobites.romgenerator.util.compress.z80.Z80OutputStream;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public class Z80CompressTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Z80CompressTest.class);
    @Test
    public void testCompression() throws IOException {
        byte[] screen = Util.fromInputStream(Z80CompressTest.class.getResourceAsStream("/image/pingpong.scr"));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        Z80OutputStream zos = new Z80OutputStream(compressed);
        zos.write(screen);
        zos.close();
        compressed.flush();
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed.toByteArray());
        LOGGER.debug("Compressed: " + Util.dumpAsHexString(compressed.toByteArray()));

        Z80CompressedInputStream zis = new Z80CompressedInputStream(bis);
        byte[] uncompressed = Util.fromInputStream(zis);
        LOGGER.debug("Orig: " + Util.dumpAsHexString(screen));
        LOGGER.debug("Unco: " + Util.dumpAsHexString(uncompressed));
        //assertArrayEquals(screen, uncompressed);
    }

}

package com.grelobites.romgenerator.util.compress.zx7;


import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.z80.Z80InputStream;
import com.grelobites.romgenerator.util.compress.z80.Z80OutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public class Z80CompressTest {

    @Test
    public void testCompression() throws IOException {
        byte[] screen = Util.fromInputStream(Z80CompressTest.class.getResourceAsStream("/image/pingpong.scr"));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        Z80OutputStream zos = new Z80OutputStream(compressed);
        zos.write(screen);
        zos.close();
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed.toByteArray());

        Z80InputStream zis = new Z80InputStream(bis);
        byte[] uncompressed = Util.fromInputStream(zis);
        assertArrayEquals(screen, uncompressed);
    }

}

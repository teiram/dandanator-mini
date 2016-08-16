package com.grelobites.romgenerator.util.compress.zx7;


import com.grelobites.romgenerator.util.Util;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class Zx7CompressTest {

    @Test
    public void testForwardCompression() throws IOException {
        byte[] screen = Util.fromInputStream(Zx7CompressTest.class.getResourceAsStream("/image/pingpong.scr"));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        Zx7OutputStream zos = new Zx7OutputStream(compressed);
        zos.write(screen);
        zos.close();
        compressed.flush();
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed.toByteArray());
        Zx7InputStream zis = new Zx7InputStream(bis);
        byte[] uncompressed = Util.fromInputStream(zis);
        assertArrayEquals(screen, uncompressed);
    }

    @Test
    public void testBackwardsCompression() throws IOException {
        byte[] screen = Util.fromInputStream(Zx7CompressTest.class.getResourceAsStream("/image/pingpong.scr"));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        Zx7OutputStream zos = new Zx7OutputStream(compressed, true);
        zos.write(screen);
        zos.close();
        compressed.flush();
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed.toByteArray());
        Zx7InputStream zis = new Zx7InputStream(bis, true);
        byte[] uncompressed = Util.fromInputStream(zis);
        assertArrayEquals(screen, uncompressed);
    }
}

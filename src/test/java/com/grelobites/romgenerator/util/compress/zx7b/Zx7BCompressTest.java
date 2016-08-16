package com.grelobites.romgenerator.util.compress.zx7b;


import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.zx7b.mad.MadCompressor;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class Zx7BCompressTest {

    @Test
    public void testForwardCompression() throws IOException {
        byte[] screen = Util.fromInputStream(Zx7BCompressTest.class.getResourceAsStream("/image/pingpong.scr"));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        Zx7bOutputStream zos = new Zx7bOutputStream(compressed);
        zos.write(screen);
        zos.close();
        compressed.flush();
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed.toByteArray());
        Zx7bInputStream zis = new Zx7bInputStream(bis);
        byte[] uncompressed = Util.fromInputStream(zis);
        assertArrayEquals(screen, uncompressed);
    }

    @Test
    public void testBackwardsCompression() throws IOException {
        byte[] screen = Util.fromInputStream(Zx7BCompressTest.class.getResourceAsStream("/image/pingpong.scr"));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        Zx7bOutputStream zos = new Zx7bOutputStream(compressed, true);
        zos.write(screen);
        zos.close();
        compressed.flush();
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed.toByteArray());
        Zx7bInputStream zis = new Zx7bInputStream(bis, true);
        byte[] uncompressed = Util.fromInputStream(zis);
        assertArrayEquals(screen, uncompressed);
    }
}

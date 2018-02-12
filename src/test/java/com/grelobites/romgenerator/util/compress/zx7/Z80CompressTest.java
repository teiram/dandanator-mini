package com.grelobites.romgenerator.util.compress.zx7;


import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.z80.Z80InputStream;
import com.grelobites.romgenerator.util.compress.z80.Z80OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public class Z80CompressTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Z80CompressTest.class);
    public void testCompression() throws IOException {
        byte[] screen = Util.fromInputStream(Z80CompressTest.class.getResourceAsStream("/image/pingpong.scr"));
        LOGGER.debug("Compressing byte array of length " + screen.length);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        Z80OutputStream zos = new Z80OutputStream(compressed);
        zos.write(screen);
        zos.close();
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed.toByteArray());

        Z80InputStream zis = new Z80InputStream(bis);
        byte[] uncompressed = Util.fromInputStream(zis);
        assertArrayEquals(screen, uncompressed);
    }


    public void testFailingCompression() throws IOException {
        Game game = GameUtil.createGameFromFile(new File("/Users/mteira/Desktop/Bould4.z80")).get();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        Z80OutputStream zos = new Z80OutputStream(compressed);
        LOGGER.debug("Compressing byte array of size " + game.getSlot(2).length);
        zos.write(game.getSlot(2));
        zos.close();
        FileOutputStream fos = new FileOutputStream("/Users/mteira/Desktop/" + compressed.size() + ".bon");
        fos.write(compressed.toByteArray());
        fos.close();
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed.toByteArray());
        Z80InputStream zis = new Z80InputStream(bis);
        byte[] uncompressed = Util.fromInputStream(zis);
        assertArrayEquals(game.getSlot(2), uncompressed);

    }

    public void testCompressionMarkCompression() throws IOException {
        byte[] data = new byte[] {
                (byte)0x0F, (byte)0xE5, (byte)0xDD, (byte)0xE5, (byte)0xE1,
                (byte)0xCD, (byte)0xED, (byte)0xED, (byte)0xE1, (byte)0xC9,
                (byte)0xFE};
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Z80OutputStream zos = new Z80OutputStream(bos);
        zos.write(data);
        zos.close();
        LOGGER.debug("Compressed is " + Util.dumpAsHexString(bos.toByteArray()));
    }

}

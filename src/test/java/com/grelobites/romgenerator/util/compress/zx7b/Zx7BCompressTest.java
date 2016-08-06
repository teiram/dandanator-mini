package com.grelobites.romgenerator.util.compress.zx7b;


import com.grelobites.romgenerator.Constants;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Zx7BCompressTest {

    @Test
    public void testCompression() throws IOException {
        byte[] screen = Constants.getDefaultMenuScreen();
        OutputStream compressed = new FileOutputStream("/Users/mteira/Desktop/screen.2");
        Zx7bOutputStream zos = new Zx7bOutputStream(compressed);
        zos.write(screen);
        zos.close();
    }
}

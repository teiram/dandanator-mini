package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.util.player.TapOutputStream;
import com.grelobites.romgenerator.util.player.TapUtil;
import com.grelobites.romgenerator.util.player.WavOutputFormat;
import org.junit.Test;

import java.io.*;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNotEquals;

public class TapUtilTest {

    @Test
    public void generateWavFromTap() throws IOException {
        InputStream tap = TapUtilTest.class.getResourceAsStream("/EE_full.tap");
        assertNotNull(tap);
        ByteArrayOutputStream wavStream = new ByteArrayOutputStream();
        TapUtil.tap2wav(WavOutputFormat.defaultDataFormat(), tap, wavStream);
        assertNotEquals(0, wavStream.size());
    }

    @Test
    public void generateWavFromBinary() throws IOException {
        InputStream binary = TapUtilTest.class.getResourceAsStream("/EE_full.bin");
        assertNotNull(binary);
        ByteArrayOutputStream tapStream = new ByteArrayOutputStream();
        TapOutputStream tos = new TapOutputStream(tapStream);
        tos.addProgramStream("EE", 10, binary);

        FileOutputStream fos = new FileOutputStream("/home/mteira/Escritorio/ee.wav");
        TapUtil.tap2wav(WavOutputFormat.defaultDataFormat(),
                new ByteArrayInputStream(tapStream.toByteArray()), fos);
    }

    @Test
    public void generateTapFromBinary() throws IOException {
        InputStream binary = TapUtilTest.class.getResourceAsStream("/EE_full.bin");
        assertNotNull(binary);
        FileOutputStream fos = new FileOutputStream("/home/mteira/Escritorio/ee.tap");
        TapOutputStream tos = new TapOutputStream(fos);
        tos.addProgramStream("EE", 10, binary);

        fos.close();
    }
}

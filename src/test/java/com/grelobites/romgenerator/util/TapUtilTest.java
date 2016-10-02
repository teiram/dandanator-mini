package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.util.player.TapUtil;
import com.grelobites.romgenerator.util.player.WavOutputFormat;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static junit.framework.TestCase.assertNotNull;

public class TapUtilTest {

    @Test
    public void generateWavFromTap() throws IOException {
        InputStream tap = TapUtilTest.class.getResourceAsStream("/EE_full.tap");
        assertNotNull(tap);
        FileOutputStream wavStream = new FileOutputStream("/home/mteira/Escritorio/standard.wav");
        TapUtil.tap2wav(WavOutputFormat.defaultDataFormat(), tap, wavStream);
    }
}

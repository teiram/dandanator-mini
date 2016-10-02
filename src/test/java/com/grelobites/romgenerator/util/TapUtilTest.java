package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.util.player.TapUtil;
import com.grelobites.romgenerator.util.player.WavOutputFormat;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
}

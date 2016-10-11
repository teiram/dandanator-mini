package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.util.player.ChannelType;
import com.grelobites.romgenerator.util.player.StandardWavOutputFormat;
import com.grelobites.romgenerator.util.player.TapOutputStream;
import com.grelobites.romgenerator.util.player.TapUtil;
import com.grelobites.romgenerator.util.player.CompressedWavOutputFormat;
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
        TapUtil.tap2wav(StandardWavOutputFormat.builder()
                .withChannelType(ChannelType.STEREOINV)
                .withSampleRate(StandardWavOutputFormat.SRATE_44100)
                .withPilotDurationMillis(5000).build(),
                tap, wavStream);
        assertNotEquals(0, wavStream.size());
    }

    @Test
    public void generateTapFromBinary() throws IOException {
        InputStream binary = TapUtilTest.class.getResourceAsStream("/EE_full.bin");
        assertNotNull(binary);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TapOutputStream tos = new TapOutputStream(bos);
        tos.addProgramStream("EE", 10, binary);
        assertNotEquals(0, bos.size());
    }
}

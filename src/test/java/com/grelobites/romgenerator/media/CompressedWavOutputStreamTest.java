package com.grelobites.romgenerator.media;

import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertNotEquals;

public class CompressedWavOutputStreamTest {


    @Test
    public void testWavGeneration() throws IOException {
        byte[] data = DandanatorMiniConstants.getDandanatorRom();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CompressedWavOutputStream wos = new CompressedWavOutputStream(result, WavOutputFormat.defaultDataFormat());
        wos.write(data);
        wos.close();
        assertNotEquals(0, result.toByteArray().length);
    }
}

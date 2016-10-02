package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TapUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapUtil.class);
    public static void tap2wav(WavOutputFormat format, InputStream tapStream, OutputStream wavStream)
    throws IOException {
        try (StandardWavOutputStream encodingStream = new StandardWavOutputStream(wavStream, format)) {
            while (true) {
                int length = Util.readAsLittleEndian(tapStream);
                if (length > 0) {
                    LOGGER.debug("Encoding block from TAP of " + length + " bytes");
                    byte[] data = new byte[length];
                    tapStream.read(data);
                    encodingStream.write(data);
                    encodingStream.nextBlock();
                } else {
                    break;
                }
            }
        }
    }
}

package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.zx7.Zx7OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class TapUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapUtil.class);

    private static final String LOADER_NAME = "EEP Writer";
    private static final String BOOOTER_RESOURCE = "/player/boot.bin";
    private static final String SCREEN_RESOURCE = "/player/screen.scr";

    public static void tap2wav(StandardWavOutputFormat format, InputStream tapStream, OutputStream wavStream)
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

    private static byte[] compressedByteArrayOf(InputStream is) throws IOException {
        ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
        try (Zx7OutputStream zos = new Zx7OutputStream(compressedStream)) {
            zos.write(Util.fromInputStream(is));
        }
        return compressedStream.toByteArray();
    }

    public static byte[] generateLoaderTap(InputStream loader, boolean beepFeedback) throws IOException {
        InputStream uncompressor = TapUtil.class.getResourceAsStream(BOOOTER_RESOURCE);
        if (uncompressor != null) {
            byte[] compressedScreen = compressedByteArrayOf(TapUtil.class.getResourceAsStream(SCREEN_RESOURCE));
            byte[] uncompressorByteArray = Util.fromInputStream(uncompressor);
            uncompressorByteArray[uncompressorByteArray.length - 3] = Integer.valueOf(beepFeedback ? 1 : 0)
                .byteValue();
            Util.writeAsLittleEndian(uncompressorByteArray, uncompressorByteArray.length - 2, compressedScreen.length);

            byte[] loaderByteArray = compressedByteArrayOf(loader);

            ByteArrayOutputStream tapStream = new ByteArrayOutputStream();
            TapOutputStream tos = new TapOutputStream(tapStream);
            tos.addProgramStream(LOADER_NAME, 10, new ByteArrayInputStream(ByteBuffer
                    .allocate(uncompressorByteArray.length + compressedScreen.length + loaderByteArray.length)
                    .put(uncompressorByteArray)
                    .put(compressedScreen)
                    .put(loaderByteArray).array()));
            return tapStream.toByteArray();
        } else {
            throw new IllegalStateException("No uncompressor resource found");
        }
    }
}

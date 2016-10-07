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
    private static final String UNCOMPRESSOR_RESOURCE = "/player/uncompressor.bin";

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

    public static byte[] generateLoaderTap(InputStream loader) throws IOException {
        InputStream uncompressor = TapUtil.class.getResourceAsStream(UNCOMPRESSOR_RESOURCE);
        if (uncompressor != null) {
            byte[] uncompressorByteArray = Util.fromInputStream(uncompressor);
            ByteArrayOutputStream compressedLoader = new ByteArrayOutputStream();
            try (Zx7OutputStream zos = new Zx7OutputStream(compressedLoader)) {
                zos.write(Util.fromInputStream(loader));
            }
            byte[] loaderByteArray = compressedLoader.toByteArray();

            ByteArrayOutputStream tapStream = new ByteArrayOutputStream();
            TapOutputStream tos = new TapOutputStream(tapStream);
            tos.addProgramStream(LOADER_NAME, 10, new ByteArrayInputStream(ByteBuffer
                    .allocate(uncompressorByteArray.length + loaderByteArray.length)
                    .put(uncompressorByteArray)
                    .put(loaderByteArray).array()));
            return tapStream.toByteArray();
        } else {
            throw new IllegalStateException("No uncompressor resource found");
        }
    }
}

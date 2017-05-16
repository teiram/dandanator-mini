package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
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
import java.nio.ByteOrder;

public class TapUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapUtil.class);

    private static final String LOADER_NAME = "EEP Writer";
    public static final String BOOTER_RESOURCE = "/player/boot.bin";
    public static final String SCREEN_RESOURCE = "/player/screen.scr";

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

    public static byte[] getLoaderTapByteArray(InputStream loader, int flagValue) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TapOutputStream loaderTap = getLoaderTap(loader, out, flagValue);
        return out.toByteArray();
    }

    public static TapOutputStream getLoaderTap(InputStream loader, OutputStream out, int flagValue) throws IOException {
        InputStream uncompressor = TapUtil.class.getResourceAsStream(BOOTER_RESOURCE);
        if (uncompressor != null) {
            byte[] compressedScreen = compressedByteArrayOf(TapUtil.class.getResourceAsStream(SCREEN_RESOURCE));
            byte[] uncompressorByteArray = Util.fromInputStream(uncompressor);

            uncompressorByteArray[uncompressorByteArray.length - 3] =
                    Integer.valueOf(flagValue).byteValue();

            Util.writeAsLittleEndian(uncompressorByteArray, uncompressorByteArray.length - 2, compressedScreen.length);

            byte[] loaderByteArray = compressedByteArrayOf(loader);

            TapOutputStream tos = new TapOutputStream(out);
            tos.addProgramStream(LOADER_NAME, 10, new ByteArrayInputStream(ByteBuffer
                    .allocate(uncompressorByteArray.length + compressedScreen.length + loaderByteArray.length)
                    .put(uncompressorByteArray)
                    .put(compressedScreen)
                    .put(loaderByteArray).array()));
            return tos;
        } else {
            throw new IllegalStateException("No uncompressor resource found");
        }
    }

    public static void upgradeTapLoader(InputStream oldTap, OutputStream newTap) throws IOException {
        //Read the first block header and validate
        ByteBuffer header = ByteBuffer.wrap(Util.fromInputStream(oldTap, 21));
        header.order(ByteOrder.LITTLE_ENDIAN);
        int headerLength = Short.valueOf(header.getShort()).intValue();
        int headerFlag = Byte.valueOf(header.get()).intValue();
        int headerType = Byte.valueOf(header.get()).intValue();
        byte[] nameByteArray = new byte[10];
        header.get(nameByteArray);
        String headerName = new String(nameByteArray);
        int length = Short.valueOf(header.getShort()).intValue();
        //Add length bytes, flag and checksum
        length += 4;
        LOGGER.debug("Found header with length: " + headerLength + ", flag: " + headerFlag
            + ", type: " + headerType + ", name: " + headerName + ", dataLength: " + length);
        if (!LOADER_NAME.equals(headerName)) {
            throw new IllegalArgumentException("The provided file is not a TAP DivIDE Loader");
        }
        LOGGER.debug("Skipping " + length + " bytes");
        oldTap.skip(length);

        TapUtil.getLoaderTap(new ByteArrayInputStream(DandanatorMiniConstants
                .getDivIdeLoader()), newTap, 0);

        byte[] buffer = new byte[1024];
        int len;
        while ((len = oldTap.read(buffer)) != -1) {
            newTap.write(buffer, 0, len);
        }
        newTap.flush();
    }
}

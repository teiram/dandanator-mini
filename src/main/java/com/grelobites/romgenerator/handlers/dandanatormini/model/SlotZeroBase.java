package com.grelobites.romgenerator.handlers.dandanatormini.model;

import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.Compressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class SlotZeroBase implements SlotZero {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlotZeroBase.class);

    private static final int VERSION_OFFSET = 16352;
    protected byte[] data;

    public SlotZeroBase(byte[] data) {
        this.data = data;
    }

    @Override
    public InputStream data() {
        return new ByteArrayInputStream(data);
    }

    protected int getMajorVersion() throws IOException {

        String majorVersion = getVersion();
        int index;
        if ((index = majorVersion.indexOf('.')) > -1) {
            majorVersion = majorVersion.substring(0, index);
        }
        LOGGER.debug("Version of romset detected as " + majorVersion);
        return Integer.parseInt(majorVersion);
    }

    protected int getMinorVersion() throws IOException {
        String version = getVersion();
        String minorVersion;
        int index;
        if ((index = version.indexOf('.')) > -1) {
            minorVersion = version.substring(index + 1);
        } else {
            minorVersion = "0";
        }
        LOGGER.debug("Minor version of romset detected as " + minorVersion);
        return Integer.parseInt(minorVersion);
    }

    protected String getVersion() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(data, VERSION_OFFSET,
                DandanatorMiniConstants.VERSION_SIZE);

        //Skip the starting 'v' of the version string
        return Util.getNullTerminatedString(stream, 1, DandanatorMiniConstants.VERSION_SIZE);
    }

    @Override
    public boolean getDisableBorderEffect() {
        return false;
    }

    private static Compressor getCompressor() {
        return DandanatorMiniConfiguration.getInstance()
                .getCompressor();
    }

    protected static byte[] uncompressByteArray(byte[] compressedData) throws IOException {
        InputStream uncompressedStream = getCompressor().getUncompressingInputStream(
                new ByteArrayInputStream(compressedData));
        return Util.fromInputStream(uncompressedStream);
    }

    protected static byte[] uncompress(PositionAwareInputStream is, int offset, int size) throws IOException {
        LOGGER.debug("Uncompress with offset " + offset + " and size " + size);
        LOGGER.debug("Skipping " + (offset - is.position()) + " to start of compressed data");
        is.safeSkip(offset - is.position());
        byte[] compressedData = Util.fromInputStream(is, size);
        return uncompressByteArray(compressedData);
    }

    protected static byte[] copy(PositionAwareInputStream is, int offset, int size) throws IOException {
        LOGGER.debug("Copying data with offset " + offset + " and size " + size);
        LOGGER.debug("Skipping " + (offset - is.position()) + " to start of uncompressed data");
        is.safeSkip(offset - is.position());
        return Util.fromInputStream(is, size);
    }

}

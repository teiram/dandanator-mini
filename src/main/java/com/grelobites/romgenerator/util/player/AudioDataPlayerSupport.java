package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.tap.TapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioDataPlayerSupport extends DataPlayerSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioDataPlayerSupport.class);

    private File temporaryFile;

    private File getTemporaryFile() throws IOException {
        if (temporaryFile == null) {
            temporaryFile = File.createTempFile("romgenerator", ".wav");
        }
        return temporaryFile;
    }

    protected void cleanup() {
        if (temporaryFile != null) {
            if (!temporaryFile.delete()) {
                LOGGER.warn("Unable to delete temporary file " + temporaryFile);
            }
            temporaryFile = null;
        }
    }

    private int getLoaderFlagValue() {
        int flagValue = (configuration.isUseTargetFeedback() ? 1 : 0) |
                (configuration.isUseSerialPort() ? 2 : 0);

        switch (configuration.getEncodingSpeed()) {
            case SPEED_STANDARD:
                flagValue |= 0x04;
                break;
            case SPEED_TURBO_1:
                flagValue |= 0x08;
                break;
            case SPEED_TURBO_2:
                flagValue |= 0x10;
                break;
            default:
                flagValue |= 0x20;
        }
        return flagValue;
    }

    public File getBootstrapAudioFile() throws IOException {
        File targetFile = getTemporaryFile();
        FileOutputStream fos = new FileOutputStream(targetFile);

        byte[] loaderTap = TapUtil.getLoaderTapByteArray(configuration.getLoaderStream(), getLoaderFlagValue());

        TapUtil.tap2wav(StandardWavOutputFormat.builder()
                        .withSampleRate(configuration.getPreferredAudioSampleRate())
                        .withChannelType(ChannelType.valueOf(configuration.getAudioMode()))
                        .withPilotDurationMillis(2500)
                        .withHighValue(configuration.isBoostLevel() ? BOOST_HIGH_LEVEL : HIGH_LEVEL)
                        .withLowValue(configuration.isBoostLevel() ? BOOST_LOW_LEVEL : LOW_LEVEL)
                        .withReversePhase(configuration.isReversePhase())
                        .build(),
                new ByteArrayInputStream(loaderTap),
                fos);
        fos.close();
        return targetFile;
    }


    public File getBlockAudioFile(int block, byte[] data) throws IOException {
        int blockSize = configuration.getBlockSize();
        byte[] buffer = new byte[blockSize + 3];
        System.arraycopy(data, 0, buffer, 0, blockSize);

        buffer[blockSize] = Integer.valueOf(block + 1).byteValue();

        Util.writeAsLittleEndian(buffer, blockSize + 1, Util.getBlockCrc16(buffer, blockSize + 1));

        File tempFile = getTemporaryFile();
        LOGGER.debug("Creating new wav file for block " + block + " on file " + tempFile);
        FileOutputStream fos = new FileOutputStream(tempFile);
        encodeBuffer(buffer, fos);
        fos.close();
        return tempFile;
    }


}

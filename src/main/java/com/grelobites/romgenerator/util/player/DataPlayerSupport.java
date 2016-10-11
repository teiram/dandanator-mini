package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.PlayerConfiguration;

import java.io.IOException;
import java.io.OutputStream;

public class DataPlayerSupport {

    protected PlayerConfiguration configuration = PlayerConfiguration.getInstance();
    protected static final int LOADER_BLOCK = -1;

    protected static int getBlockCrc(byte[] data, int blockSize) {
        int sum = 0;
        for (int i = 0; i <  blockSize; i++) {
            sum += Byte.toUnsignedInt(data[i]);
        }
        return sum & 0xffff;
    }

    protected void encodeBuffer(byte[] buffer, EncodingSpeedPolicy encodingSpeedPolicy,
                              OutputStream out) throws IOException {
        OutputStream wos = encodingSpeedPolicy.useStandardEncoding() ?
                new StandardWavOutputStream(out, StandardWavOutputFormat.builder()
                        .withSampleRate(StandardWavOutputFormat.SRATE_44100)
                        .withPilotDurationMillis(configuration.getPilotLength())
                        .withChannelType(ChannelType.valueOf(configuration.getAudioMode()))
                        .build()) :
                new CompressedWavOutputStream(out,
                        CompressedWavOutputFormat.builder()
                                .withSampleRate(CompressedWavOutputFormat.SRATE_44100)
                                .withChannelType(ChannelType.valueOf(configuration.getAudioMode()))
                                .withSpeed(encodingSpeedPolicy.getEncodingSpeed())
                                .withFlagByte(CompressedWavOutputFormat.DATA_FLAG_BYTE)
                                .withOffset(CompressedWavOutputFormat.DEFAULT_OFFSET)
                                .withPilotDurationMillis(configuration.getPilotLength())
                                .withFinalPauseDurationMillis(configuration.getTrailLength())
                                .build());

        wos.write(buffer);
        wos.flush();
    }

}

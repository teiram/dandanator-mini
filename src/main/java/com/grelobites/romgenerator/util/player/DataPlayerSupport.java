package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.PlayerConfiguration;

import java.io.IOException;
import java.io.OutputStream;

public class DataPlayerSupport {

    public static final int SPEED_STANDARD = 0;
    public static final int SPEED_TURBO_1 = 1;
    public static final int SPEED_TURBO_2 = 2;
    public static final int SPEED_LECHES_1 = 3;
    public static final int SPEED_LECHES_2 = 4;
    public static final int SPEED_LECHES_3 = 5;

    protected PlayerConfiguration configuration = PlayerConfiguration.getInstance();

    protected static int getBlockCrc(byte[] data, int blockSize) {
        int sum = 0;
        for (int i = 0; i <  blockSize; i++) {
            sum += Byte.toUnsignedInt(data[i]);
        }
        return sum & 0xffff;
    }

    private StandardWavOutputFormat getStandardWavOutputFormat() {
        return StandardWavOutputFormat.builder()
                .withChannelType(ChannelType.valueOf(configuration.getAudioMode()))
                .withPilotDurationMillis(configuration.getPilotLength())
                .withSampleRate(StandardWavOutputFormat.SRATE_44100)
                .withZeroDurationTStates(StandardWavOutputFormat.ZERO_DURATIONS[configuration.getEncodingSpeed()])
                .withOneDurationTStates(StandardWavOutputFormat.ONE_DURATIONS[configuration.getEncodingSpeed()])
                .build();
    }

    private CompressedWavOutputFormat getCompressedWavOutputFormat() {
        return CompressedWavOutputFormat.builder()
                .withChannelType(ChannelType.valueOf(configuration.getAudioMode()))
                .withPilotDurationMillis(configuration.getPilotLength())
                .withSampleRate(StandardWavOutputFormat.SRATE_44100)
                .withSpeed(5 - configuration.getEncodingSpeed())
                .withFlagByte(CompressedWavOutputFormat.DATA_FLAG_BYTE)
                .withOffset(CompressedWavOutputFormat.DEFAULT_OFFSET)
                .withPilotDurationMillis(configuration.getPilotLength())
                .withFinalPauseDurationMillis(configuration.getTrailLength())
                .build();
    }


    protected void encodeBuffer(byte[] buffer, OutputStream out) throws IOException {
        OutputStream wos = configuration.getEncodingSpeed() <= SPEED_TURBO_2 ?
                new StandardWavOutputStream(out, getStandardWavOutputFormat()) :
                new CompressedWavOutputStream(out, getCompressedWavOutputFormat());

        wos.write(buffer);
        wos.flush();
    }

}

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

    protected static final int BOOST_HIGH_LEVEL = 0xff;
    protected static final int HIGH_LEVEL = 0xc0;
    protected static final int BOOST_LOW_LEVEL = 0;
    protected static final int LOW_LEVEL = 0x40;

    private static final int STANDARD_PILOT_DURATION = 2500;

    private static final Integer DATA_HEADER = 0xFF;

    private static final int[] LECHES_MAPPED = new int[] {7, 5, 2};

    protected PlayerConfiguration configuration = PlayerConfiguration.getInstance();

    protected static int getBlockXorCrc(byte[] data, int blockSize) {
        int crc = 0;
        for (int i = 0; i < blockSize; i++) {
            crc ^= Byte.toUnsignedInt(data[i]);
        }
        return crc;
    }

    private StandardWavOutputFormat getStandardWavOutputFormat() {
        return StandardWavOutputFormat.builder()
                .withChannelType(ChannelType.valueOf(configuration.getAudioMode()))
                .withPilotDurationMillis(STANDARD_PILOT_DURATION)
                .withSampleRate(configuration.getPreferredAudioSampleRate())
                .withZeroDurationTStates(StandardWavOutputFormat.ZERO_DURATIONS[configuration.getEncodingSpeed()])
                .withOneDurationTStates(StandardWavOutputFormat.ONE_DURATIONS[configuration.getEncodingSpeed()])
                .withHighValue(configuration.isBoostLevel() ? BOOST_HIGH_LEVEL : HIGH_LEVEL)
                .withLowValue(configuration.isBoostLevel() ? BOOST_LOW_LEVEL : LOW_LEVEL)
                .withReversePhase(configuration.isReversePhase())
                .withLeadOutDurationMillis(configuration.getTrailLength())
                .build();
    }

    private CompressedWavOutputFormat getCompressedWavOutputFormat() {
        return CompressedWavOutputFormat.builder()
                .withChannelType(ChannelType.valueOf(configuration.getAudioMode()))
                .withPilotDurationMillis(configuration.getPilotLength())
                .withSampleRate(configuration.getPreferredAudioSampleRate())
                .withSpeed(LECHES_MAPPED[configuration.getEncodingSpeed() - SPEED_LECHES_1])
                .withFlagByte(CompressedWavOutputFormat.DATA_FLAG_BYTE)
                .withOffset(CompressedWavOutputFormat.DEFAULT_OFFSET)
                .withPilotDurationMillis(configuration.getPilotLength())
                .withFinalPauseDurationMillis(configuration.getTrailLength())
                .withHighLevel(configuration.isBoostLevel() ? BOOST_HIGH_LEVEL : HIGH_LEVEL)
                .withLowLevel(configuration.isBoostLevel() ? BOOST_LOW_LEVEL : LOW_LEVEL)
                .withReversePhase(configuration.isReversePhase())
                .build();
    }

    private boolean useStandardEncoding() {
        return configuration.getEncodingSpeed() <= SPEED_TURBO_2;
    }

    protected void encodeBuffer(byte[] buffer, OutputStream out) throws IOException {
        OutputStream wos = useStandardEncoding() ?
                new StandardWavOutputStream(out, getStandardWavOutputFormat()) :
                new CompressedWavOutputStream(out, getCompressedWavOutputFormat());

        if (useStandardEncoding()) {
            wos.write(DATA_HEADER);
        }

        wos.write(buffer);

        if (useStandardEncoding()) {
            int crc = getBlockXorCrc(buffer, buffer.length);
            crc ^= Byte.toUnsignedInt(DATA_HEADER.byteValue());
            wos.write(Integer.valueOf(crc).byteValue());
        }

        wos.flush();
    }

}

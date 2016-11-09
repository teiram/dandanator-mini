package com.grelobites.romgenerator.zxspectrum.spectrum;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.player.ChannelType;
import com.grelobites.romgenerator.util.player.StandardWavOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class TapBitInputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapBitInputStream.class);

    private static final int SPECTRUM_CLOCK = 3500000;
    private static final int HEADER_PULSE_LENGTH = 2168;
    private static final int SYNC_P0_TSTATES = 667;
    private static final int SYNC_P1_TSTATES = 735;
    private static final int ZERO_TSTATES = 748;
    private static final int ONE_TSTATES = 1496;

    private InputStream tapData;

    private byte[] currentBlock;
    private int currentBlockPosition = -1;

    public TapBitInputStream(InputStream tapData) {
        this.tapData = tapData;
    }

    public boolean nextValue() throws IOException {
        if (currentBlock == null) {
            int length = Util.readAsLittleEndian(tapData);
            if (length > 0) {
                LOGGER.debug("Encoding block from TAP of " + length + " bytes");
                currentBlock = new byte[length];
                tapData.read(currentBlock);
                currentBlockPosition = 0;
            }
        }
    }

    private int tStatesToSamples(int tstates) {
        int upper = tstates * format.getSampleRate();
        return upper / SPECTRUM_CLOCK + (upper % SPECTRUM_CLOCK == 0 ? 0 : 1);
    }

    private void writeSamples(int samples, boolean high) throws IOException {
        int highValue = getHighValue();
        int lowValue = getLowValue();
        for (int i = 0; i < samples; i++) {
            wavStream.write(high ? highValue : lowValue);
            if (format.getChannelType() == ChannelType.STEREO) {
                wavStream.write(high ? highValue: lowValue);
            } else if (format.getChannelType() == ChannelType.STEREOINV) {
                wavStream.write(high ? lowValue: highValue);
            }
        }
    }

    private void writeBit(boolean bit) throws IOException {
        int pulseSamples = tStatesToSamples(bit ? format.getOneDurationTStates() :
                format.getZeroDurationTStates());
        writeSamples(pulseSamples, true);
        writeSamples(pulseSamples, false);
    }

    private void writeByte(int value) throws IOException {
        value &= 0xff;
        int mask = 0x80;
        for (int i = 0; i < 8; i++) {
            writeBit((value & mask) != 0);
            mask >>= 1;
        }
    }

    public TapBitInputStream(OutputStream out, StandardWavOutputFormat format) {
        super(out);
        this.format = format;
        buffers = new ArrayList<>();
        currentBuffer = new ByteArrayOutputStream();
        buffers.add(currentBuffer);
    }

    private void writeHeader() throws IOException {
        int headerSamples = new Double(format.getPilotDurationMillis() * format.getSampleRate() / 1000).intValue();
        int pulseSamples = tStatesToSamples(HEADER_PULSE_LENGTH);
        int cycles = headerSamples / (pulseSamples * 2);
        for (int i = 0; i < cycles; i++) {
            writeSamples(pulseSamples, true);
            writeSamples(pulseSamples, false);
        }
    }

    private void writeSync() throws IOException {
        writeSamples(tStatesToSamples(SYNC_P0_LENGTH), true);
        writeSamples(tStatesToSamples(SYNC_P1_LENGTH), false);
    }

}

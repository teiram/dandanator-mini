package com.grelobites.romgenerator.util.player;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CompressedWavOutputStream extends FilterOutputStream {

    private static final int HEADER_LENGTH = 44;

    private static int table1[][] = new int[][] {
            new int[] {1, 2, 2, 3},
            new int[] {2, 2, 3, 3},
            new int[] {2, 3, 3, 4},
            new int[] {3, 3, 4, 4},
            new int[] {1, 2, 3, 4},
            new int[] {2, 3, 4, 5},
            new int[] {2, 3, 4, 5},
            new int[] {3, 4, 5, 6}
    };

    private static int table2[][] = new int[][] {
            new int[] {1, 1, 2, 2},
            new int[] {1, 2, 2, 3},
            new int[] {2, 2, 3, 3},
            new int[] {2, 3, 3, 4},
            new int[] {1, 2, 3, 4},
            new int[] {1, 2, 3, 4},
            new int[] {2, 3, 4, 5},
            new int[] {2, 3, 4, 5}
    };

    private static int byvel[][] = new int[][] {
            new int[] {0xed, 0xde, 0xd2, 0xc3, 0x00, 0x71, 0x62, 0x53},
            new int[] {0xf1, 0xe5, 0xd6, 0xc7, 0x04, 0x78, 0x69, 0x5d}
    };

    private static int termin[][] = new int[][] {
            new int[] {21, 22, 23, 24, 23, 24, 25, 26},
            new int[] {13, 14, 15, 16, 15, 16, 17, 18}
    };

    private byte[] getWavHeader(int wavDataLength) {
        int byteRate = format.getSampleRate() * format.getChannelType().channels();
        int sampleRate = format.getSampleRate();
        short numChannels = (short) format.getChannelType().channels();

        ByteBuffer buffer = ByteBuffer.allocate(HEADER_LENGTH)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".getBytes())                                 //ChunkID
                .putInt(wavDataLength + 36)                             //ChunkSize
                .put("WAVE".getBytes())                                 //Format
                .put("fmt ".getBytes())                                 //Subchunk1ID
                .putInt(0x00000010)                                     //Subchunk1Size (16 for PCM)
                .putShort((short) 1)                                    //AudioFormat 1=PCM
                .putShort(numChannels)                                  //NumChannels
                .putInt(sampleRate)                                     //SampleRate
                .putInt(byteRate)                                       //ByteRate
                .putShort(numChannels)                                  //Block align
                .putShort((short) 8)                                    //Bits per sample
                .put("data".getBytes())                                 //Subchunk2ID
                .putInt(wavDataLength);                                 //Subchunk2Size
        return buffer.array();
    }

    private boolean initialBit;
    private ByteArrayOutputStream buffer;
    private ByteArrayOutputStream wavStream;
    private CompressedWavOutputFormat format;

    private int getLowLevel() {
        return format.isReversePhase() ? format.getHighLevel() : format.getLowLevel();
    }

    private int getHighLevel() {
        return format.isReversePhase() ? format.getLowLevel() : format.getHighLevel();
    }

    private void writeBits(int value) throws IOException {
        value &= 0xffff;
        int level = initialBit ? getHighLevel() : getLowLevel();
        int reverseLevel = initialBit ? getLowLevel() : getHighLevel();
        for (int i = 0; i < value; i++) {
            wavStream.write(level);
            if (format.getChannelType() == ChannelType.STEREO) {
                wavStream.write(level);
            } else if (format.getChannelType() == ChannelType.STEREOINV) {
                wavStream.write(reverseLevel);
            }
        }
        initialBit = !initialBit;
    }

    public CompressedWavOutputStream(OutputStream out, CompressedWavOutputFormat format) {
        super(out);
        this.format = format;
        this.buffer = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) throws IOException {
        buffer.write(b);
    }

    @Override
    public void flush() throws IOException {
        initialBit = false;
        byte[] data = buffer.toByteArray();
        boolean mlow = format.getSampleRate() == CompressedWavOutputFormat.SRATE_48000;
        int pilotts = (short) (mlow ? 875 : 952);
        int pilotpulses = new Double(format.getPilotDurationMillis() * 3500 / pilotts + 0.5).intValue();
        pilotpulses += ((pilotpulses & 1) == 0 ? 1 : 0);
        int pause_samples = new Double(
                format.getSampleRate() *
                        format.getChannelType().channels() *
                        format.getFinalPauseDurationMillis() /
                        1000).intValue();

        wavStream = new ByteArrayOutputStream();

        while (pilotpulses-- > 0) {
            writeBits(12);
        }
        writeBits(28);
        pilotpulses = 6;
        while (pilotpulses-- > 0) {
            writeBits(12);
        }
        writeBits(2);
        writeBits(mlow ? 4 : 8);

        int checksum = 0;
        for (byte aData : data) {
            checksum ^= aData;
        }

        int speed = format.getSpeed();
        int offset = format.getOffset();
        int flag = format.getFlagByte();

        int refconf = (byvel[mlow ? 1 : 0][speed] & 128)
                + (byvel[mlow ? 1 : 0][speed] + 3 * offset & 127);
        int outflag = (refconf & 0xff) | ((flag << 8) & 0xff00) | ((checksum << 16) & 0xff0000);
        for (int j = 0; j < 24; j++, outflag <<= 1) {
            int k = (outflag & 0x800000) == 0 ? 8 : 4;
            writeBits(k);
            writeBits(k);
        }
        writeBits(2);
        writeBits(3);
        for (byte aData : data) {
            int value = Byte.toUnsignedInt(aData);
            writeBits(table1[speed][value >> 6]);
            writeBits(table2[speed][value >> 6]);
            writeBits(table1[speed][value >> 4 & 3]);
            writeBits(table2[speed][value >> 4 & 3]);
            writeBits(table1[speed][value >> 2 & 3]);
            writeBits(table2[speed][value >> 2 & 3]);
            writeBits(table1[speed][value & 3]);
            writeBits(table2[speed][value & 3]);
        }
        writeBits(termin[mlow ? 1 : 0][speed]>>1);
        writeBits(termin[mlow ? 1 : 0][speed]-(termin[mlow ? 1 : 0][speed]>>1));
        writeBits(1);
        writeBits(1);

        for (int i = 0; i < pause_samples; i++) {
            wavStream.write(128);
        }
        wavStream.flush();
        out.write(getWavHeader(wavStream.size()));
        out.write(wavStream.toByteArray());
        out.flush();
        buffer.reset();
        wavStream.reset();
    }
}

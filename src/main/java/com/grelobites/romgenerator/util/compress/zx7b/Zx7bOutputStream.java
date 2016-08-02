package com.grelobites.romgenerator.util.compress.zx7b;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * ZX7 Backwards compressor v1.01 by Einar Saukas/Antonio Villena, 28 Dec 2013
 * Ported to Java by Mad3001 28Jul2016
 */
public class Zx7bOutputStream extends FilterOutputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(Zx7bOutputStream.class);

    private static final Integer MAX_OFFSET = 2176; // Range 1..2176
    private static final Integer MAX_LEN = 65536;   // Range 2..65536

    private ByteArrayOutputStream inputData;

    public Zx7bOutputStream(OutputStream out) {
        super(out);
        this.inputData = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) throws IOException {
        inputData.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        inputData.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        inputData.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        inputData.flush();
        byte[] data = Util.reverseByteArray(inputData.toByteArray());
        Optimal[] optimals = optimize(data);
        byte[] result = compress(optimals, data);
        this.out.write(Util.reverseByteArray(result));
    }

    private static Optimal[] optimize(byte[] data) {
        LOGGER.debug("Optimizing for input size " + data.length);
        int inputSize = data.length;

        int min[] = new int[MAX_OFFSET + 1];
        int max[] = new int[MAX_OFFSET + 1];
        Optimal[] optimals = new Optimal[inputSize];
        Match[] matches = new Match[256 * 256];
        Match[] matchSlots = new Match[inputSize];

        for (int i = 0; i < optimals.length; i++) {
            optimals[i] = new Optimal();
        }
        for (int i = 0; i < matches.length; i++) {
            matches[i] = new Match();
        }
        for (int i = 0; i < matchSlots.length; i++) {
            matchSlots[i] = new Match();
        }

        //First byte is always literal
        optimals[0].bits = 8;

		//Process remaining bytes
        for (int i = 1; i < inputSize; i++) {
            optimals[i].bits = optimals[i - 1].bits + 9;
            int matchIndex = (data[i - 1] < 0 ?
                    data[i - 1] + 256 :
                    data[i - 1]) << 8 | (data[i] < 0 ?
                    data[i] + 256 :
                    data[i]);
            int best_len = 1;
            for (Match match = matches[matchIndex];
                 match.next != null && best_len < MAX_LEN;
                 match = match.next) {
                int offset = i - match.next.index;
                if (offset > MAX_OFFSET) {
                    match.next = null;
                    break;
                }
                int len;
                for (len = 2; len <= MAX_LEN; len++) {
                    if ((len > best_len) && (len & 0xff) != 0) {
                        best_len = len;
                        int bits = optimals[i - len].bits + bitsCount(offset, len);
                        if (optimals[i].bits > bits) {
                            optimals[i].bits = bits;
                            optimals[i].offset = offset;
                            optimals[i].len = len;
                        }
                    } else if (i + 1 == max[offset] + len && max[offset] != 0) {
                        len = i - min[offset];
                        if (len > best_len) {
                            len = best_len;
                        }
                    }
                    if (i < offset + len || data[i - len] != data[i - len - offset]) {
                        break;
                    }
                }
                min[offset] = i + 1 - len;
                max[offset] = i;
            }
            matchSlots[i].index = i;
            matchSlots[i].next = matches[matchIndex].next;
            matches[matchIndex].next = matchSlots[i];
        }
        return optimals;
    }

    public byte[] compress(Optimal[] optimals, byte[] data) throws IOException {
        int inputSize = data.length;
        int outputSize = (optimals[inputSize - 1].bits + 16 + 7) / 8;
        LOGGER.debug("Compressed size will be " + outputSize);
        OutputByteArrayWriter output = new OutputByteArrayWriter(outputSize);

        int offset;
        int inputIndex = inputSize - 1;
        int previousInputIndex;

        optimals[inputIndex].bits = 0;
        while (inputIndex > 0) {
            if (optimals[inputIndex].len == null) {
                previousInputIndex = inputIndex - 1;
            } else {
                previousInputIndex = inputIndex - optimals[inputIndex].len;
            }
            optimals[previousInputIndex].bits = inputIndex;
            inputIndex = previousInputIndex;
        }

	    //First byte is always literal */
        output.write(data[0]);

	    //Process remaining bytes */
        while ((inputIndex = optimals[inputIndex].bits) > 0) {
            if (optimals[inputIndex].len == null) {
                output.writeBit(0);
                output.write(data[inputIndex]);
            } else if (optimals[inputIndex].len == 0) {
                output.writeBit(0);
                output.write(data[inputIndex]);
            } else {
                //Sequence indicator
                output.writeBit(1);
                //Sequence length
                output.writeEliasGamma(optimals[inputIndex].len - 1);
                //Sequence offset
                offset = optimals[inputIndex].offset - 1;
                if (offset < 128) {
                    output.write((byte) offset);
                } else {
                    offset -= 128;
                    output.write((byte) ((offset & 127) | 128));
                    for (int mask = 1024; mask > 127; mask >>= 1) {
                        output.writeBit(offset & mask);
                    }
                }
            }
        }

        //End mark
        output.writeBit(1);
        output.writeEliasGamma(0xff);

        return output.asByteArray();
    }

    @Override
    public void close() throws IOException {
        flush();
        out.close();
    }

    public static int eliasGammaBits(int value) {
        int bits = 1;
        while (value > 1) {
            bits += 2;
            value >>= 1;
        }
        return bits;
    }

    public static int bitsCount(int offset, int len) {
        return 1 + (offset > 128 ? 12 : 8) + eliasGammaBits(len - 1);
    }

}
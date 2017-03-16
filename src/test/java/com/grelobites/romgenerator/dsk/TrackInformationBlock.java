package com.grelobites.romgenerator.dsk;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class TrackInformationBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackInformationBlock.class);
    private static final byte[] TRACK_HEADER = new String("Track-Info\r\n").getBytes();

    public int trackNumber;
    public int sideNumber;
    public int sectorSize;
    public int sectorCount;
    public int gap3Length;
    public int fillerByte;
    public SectorInformationBlock[] sectorInformationList;

    public static TrackInformationBlock fromInputStream(InputStream data) throws IOException {
        TrackInformationBlock block = fromByteArray(Util.fromInputStream(data, 0x100));
        LOGGER.debug("Track information is " + block);

        return block;
    }

    private static TrackInformationBlock fromByteArray(byte[] data) {
        ByteBuffer header = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        byte[] trackHeader = new byte[12];
        header.get(trackHeader);
        if (!Arrays.equals(TRACK_HEADER, trackHeader)) {
            LOGGER.error("Track Header doesn't match expected DSK contents: "
                + new String(trackHeader));
            throw new IllegalArgumentException("Invalid track header");
        }

        header.position(0x10);

        TrackInformationBlock block = new TrackInformationBlock();
        block.trackNumber = header.get();
        block.sideNumber = header.get();
        header.getShort();
        block.sectorSize = 128 << header.get();
        block.sectorCount = header.get();
        block.gap3Length = header.get();
        block.fillerByte = header.get();

        block.sectorInformationList = new SectorInformationBlock[block.sectorCount];
        byte[] sectorInfoBytes = new byte[8];
        for (int i = 0; i < block.sectorCount; i++) {
            header.get(sectorInfoBytes);
            block.sectorInformationList[i] = SectorInformationBlock.fromByteArray(sectorInfoBytes);
        }

        return block;
    }

    @Override
    public String toString() {
        return "TrackInformationBlock{" +
                "trackNumber=" + trackNumber +
                ", sideNumber=" + sideNumber +
                ", sectorSize=" + sectorSize +
                ", sectorCount=" + sectorCount +
                ", gap3Length=" + gap3Length +
                ", fillerByte=" + fillerByte +
                ", sectorInformationList=" + Arrays.toString(sectorInformationList) +
                '}';
    }
}

package com.grelobites.romgenerator.dsk;

import com.grelobites.romgenerator.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class SectorInformationBlock {

    public int track;
    public int side;
    public int sectorId;
    public int sectorSize;
    public int fdcStatusRegister1;
    public int fdcStatusRegister2;

    public static SectorInformationBlock fromInputStream(InputStream data) throws IOException {
        return fromByteArray(Util.fromInputStream(data, 8));
    }

    public static SectorInformationBlock fromByteArray(byte[] data) {
        SectorInformationBlock block = new SectorInformationBlock();
        ByteBuffer header = ByteBuffer.wrap(data);
        block.track = header.get();
        block.side = header.get();
        block.sectorId = header.get();
        block.sectorSize = header.get();
        block.fdcStatusRegister1 = header.get();
        block.fdcStatusRegister2 = header.get();

        return block;
    }

    @Override
    public String toString() {
        return "SectorInformationBlock{" +
                "track=" + track +
                ", side=" + side +
                ", sectorId=" + sectorId +
                ", sectorSize=" + sectorSize +
                ", fdcStatusRegister1=" + fdcStatusRegister1 +
                ", fdcStatusRegister2=" + fdcStatusRegister2 +
                '}';
    }
}

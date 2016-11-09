package com.grelobites.romgenerator.zxspectrum.cpm;

/**
 * $Id: DPBBuffer.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * DPB from one byte array
 * <p>
 * $Log: DPBBuffer.java,v $
 * Revision 1.2  2004/06/20 16:26:45  mviara
 * CVS ----------------------------------------------------------------------
 * Some minor change.
 */
public class DPBBuffer extends DPB {
    private byte buffer[];
    private int offset = 0;

    public DPBBuffer() {
    }

    public DPBBuffer(byte buffer[]) {
        setBuffer(buffer, 0);
    }

    public void setBuffer(byte buffer[], int offset) {
        this.offset = offset;
        this.buffer = buffer;
        sectorTrack = getWord(SPT);
        bsm = getByte(BSM);
        blm = getByte(BLM);
        exm = getByte(EXM);
        dsm = getWord(DSM);
        drm = getWord(DRM);
        exm = getByte(EXM);
        alloc0 = getByte(AL0);
        alloc1 = getByte(AL1);
        trackOffset = getWord(OFF);
        alloc0 = getByte(AL0);
        alloc1 = getByte(AL1);
        setSkew(1);
    }

    private int getByte(int off) {
        //System.out.println("GetByte offset "+offset+" get "+off+" tot "+(off+offset)+" = "+buffer[offset+off]);
        return buffer[offset + off] & 0xff;
    }

    private int getWord(int off) {
        return getByte(off + 0) + getByte(off + 1) * 256;
    }
}
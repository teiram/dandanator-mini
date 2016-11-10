package com.grelobites.romgenerator.zxspectrum.disk;


import com.grelobites.romgenerator.zxspectrum.Disk;

/**
 * $Id: AbstractDisk.java 331 2010-09-14 10:30:13Z mviara $
 * <p>
 * Sample implementation of j80.disk
 * <p>
 * $Log: AbstractDisk.java,v $
 * Revision 1.2  2004/06/20 16:27:04  mviara
 * Some minor change.
 */
public abstract class AbstractDisk implements Disk {
    /**
     * Default sector track is IBM 3270
     */
    private int sectorTrack = 26;

    /**
     * Return sector per track
     */
    public int getSectorTrack() {
        return sectorTrack;
    }

    /**
     * Set sector per track
     */
    public void setSectorTrack(int s) {
        sectorTrack = s;
    }

    /**
     * Calculate offset from track,sector
     *
     * @param track  Track number
     * @param sector Sector number
     */
    protected int getOffset(int track, int sector) {
        return (track * getSectorTrack() + sector - 1) * SECSIZE;
    }

    protected void fillSector(byte buffer[], int b) {
        for (int i = 0; i < SECSIZE; i++)
            buffer[i] = (byte) b;
    }

    protected byte[] getFilledSector(int b) {
        byte buffer[] = new byte[SECSIZE];
        fillSector(buffer, b);

        return buffer;
    }
}	


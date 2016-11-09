package com.grelobites.romgenerator.zxspectrum.disk;


import com.grelobites.dandanator.util.emulator.zxspectrum.cpm.DPB;

/**
 * $Id: MemoryDisk.java 331 2010-09-14 10:30:13Z mviara $
 * <p>
 * Disk stored in allocated memory.
 * <p>
 * $Log: MemoryDisk.java,v $
 * Revision 1.3  2004/11/22 16:50:34  mviara
 * Some cosmetic change.
 * <p>
 * Revision 1.2  2004/06/20 16:27:04  mviara
 * Some minor change.
 */
public class MemoryDisk extends AbstractDisk {
    /**
     * Disk DPB
     */
    protected DPB dpb;
    /**
     * Disk image allocated when the disk is mounted
     */
    private byte memory[] = null;

    /**
     * Standard constructor
     */
    public MemoryDisk(DPB dpb) {
        this.dpb = dpb;
    }

    /**
     * Check if the disk is mounted
     */
    public boolean isMounted() {
        return memory == null ? false : true;
    }

    /**
     * Mount the disk
     */
    public void mount() throws Exception {
        allocMemory();

    }

    /**
     * Alloc the memory for the disk
     */
    private void allocMemory() {
        int size = (dpb.blm + 1) * (dpb.dsm + 1) + dpb.trackOffset * dpb.sectorTrack;
        size *= SECSIZE;
        memory = new byte[size];
    }

    /**
     * Read one sector
     */
    public void read(int track, int sector, byte buffer[]) throws Exception {
        if (isMounted() == false)
            mount();

        int offset = getOffset(track, sector);
        for (int i = 0; i < SECSIZE; i++)
            buffer[i] = memory[offset + i];
    }

    /**
     * Write one sector
     */
    public void write(int track, int sector, byte buffer[]) throws Exception {
        if (isMounted() == false)
            mount();

        int offset = getOffset(track, sector);
        for (int i = 0; i < SECSIZE; i++)
            memory[offset + i] = buffer[i];
    }

    /**
     * Format one track
     */
    public void format(int track) throws Exception {
        if (isMounted() == false)
            allocMemory();

    }

    /**
     * Return the number of sector per track
     */
    public int getSectorTrack() {
        return dpb.sectorTrack;
    }

    /**
     * Dismount the disk
     */
    public void umount() {
        memory = null;
    }


}
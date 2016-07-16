package com.grelobites.dandanator.util.emulator.zxspectrum;

/**
 * $Id: Disk.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Interface to a disk phisical device, the interface must provide the
 * basic I/O for one disk. This version assume fixed sector size at
 * 128 byte.
 * <p>
 * <p>
 * $Log: Disk.java,v $
 * Revision 1.3  2004/06/20 16:27:29  mviara
 * Some minor change.
 * <p>
 * Revision 1.2  2004/06/16 15:24:20  mviara
 * First CVS Revision.
 */
public interface Disk {
    /// Standard CP/M 2.2 Sector size
    static public int SECSIZE = 128;

    /**
     * Check if the disk is mounted
     *
     * @return true if the disk is mounted
     */
    public boolean isMounted() throws Exception;

    /**
     * Mount the disk
     */
    public void mount() throws Exception;

    /**
     * Read one sector
     *
     * @param track  - Track number (0 based)
     * @param sector - Sector number  (1 based)
     * @param buffer - I/O Buffer
     */
    public void read(int track, int sector, byte buffer[]) throws Exception;

    /**
     * Write one sector
     *
     * @param track  - Track number (0 based)
     * @param sector - Sector number  (1 based)
     * @param buffer - I/O Buffer
     */
    public void write(int track, int sector, byte buffer[]) throws Exception;

    /**
     * Format one track
     *
     * @param track - Track number (0 based)
     */
    public void format(int track) throws Exception;


    /**
     * Get the number of sector x track
     *
     * @return number of sector x track
     */
    public int getSectorTrack();

    /**
     * Diskount the disk
     */
    public void umount() throws Exception;
}

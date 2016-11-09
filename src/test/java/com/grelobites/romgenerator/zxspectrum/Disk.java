package com.grelobites.romgenerator.zxspectrum;

/**
 * <p>
 * Interface to a physical disk device, the interface must provide the
 * basic I/O for a disk. This version assumes fixed sector size at
 * 128 byte.
 * <p>
 */
public interface Disk {

    /// Standard CP/M 2.2 Sector size
    int SECSIZE = 128;

    /**
     * Check if the disk is mounted
     *
     * @return true if the disk is mounted
     */
    boolean isMounted() throws Exception;

    /**
     * Mount the disk
     */
    void mount() throws Exception;

    /**
     * Read one sector
     *
     * @param track  - Track number (0 based)
     * @param sector - Sector number  (1 based)
     * @param buffer - I/O Buffer
     */
    void read(int track, int sector, byte buffer[]) throws Exception;

    /**
     * Write one sector
     *
     * @param track  - Track number (0 based)
     * @param sector - Sector number  (1 based)
     * @param buffer - I/O Buffer
     */
    void write(int track, int sector, byte buffer[]) throws Exception;

    /**
     * Format one track
     *
     * @param track - Track number (0 based)
     */
    void format(int track) throws Exception;


    /**
     * Get the number of sector x track
     *
     * @return number of sector x track
     */
    int getSectorTrack();

    /**
     * Diskount the disk
     */
    void umount() throws Exception;
}

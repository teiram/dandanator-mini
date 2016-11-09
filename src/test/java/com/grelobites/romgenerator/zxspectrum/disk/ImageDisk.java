package com.grelobites.romgenerator.zxspectrum.disk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;


/**
 * $Id: ImageDisk.java 331 2010-09-14 10:30:13Z mviara $
 * <p>
 * File image disk the CP/M file is read/writte in one file that is the
 * exact rappresentation of the phisical device.
 * <p>
 * $Log: ImageDisk.java,v $
 * Revision 1.3  2004/11/22 16:50:34  mviara
 * Some cosmetic change.
 * <p>
 * Revision 1.2  2004/06/20 16:27:04  mviara
 * Some minor change.
 */
public class ImageDisk extends AbstractDisk {
    private String name;
    private RandomAccessFile rf;

    /**
     * Constructor used by subclass with only the name of the file.
     */
    protected ImageDisk(String name) {
        this.name = name;
    }

    /**
     * Standard constructor must specify the filename and the number of
     * sector per track.
     *
     * @param name        - File name
     * @param sectorTrack - Number of sector for track
     */
    public ImageDisk(String name, int sectorTrack) {
        rf = null;
        setSectorTrack(sectorTrack);
        this.name = name;
    }

    /**
     * Check if the disk is mounted
     */
    public boolean isMounted() {
        return rf == null ? false : true;
    }

    /**
     * Mount the disk
     */
    public void mount() throws Exception {
        mount(false);
    }

    /**
     * Mount the disk
     *
     * @param create - true if the file can be created
     */
    public void mount(boolean create) throws Exception {
        if (isMounted())
            return;

        File file = new File(name);

        if (file.exists() == false && create == false)
            throw new FileNotFoundException("File not found : " + name);

        rf = new RandomAccessFile(file, "rw");
    }

    /**
     * Read one sector
     */
    public void read(int track, int sector, byte buffer[]) throws Exception {
        if (isMounted() == false)
            mount();
        int offset = getOffset(track, sector);

        if (offset + SECSIZE > rf.length()) {
            fillSector(buffer, 0xe5);
        } else {
            rf.seek(offset);
            rf.read(buffer);
        }

    }

    /**
     * Format one track
     */
    public void format(int track) throws Exception {
        /**
         * If the disk is not mounted try to create the file
         */
        if (isMounted() == false)
            mount(true);
        byte buffer[] = getFilledSector(0);

        for (int sector = 1; sector <= getSectorTrack(); sector++)
            write(track, sector, buffer);
    }

    /**
     * Write one sector
     */
    public void write(int track, int sector, byte buffer[]) throws Exception {
        int offset = getOffset(track, sector);
        if (isMounted() == false)
            mount();


        rf.seek(offset);
        rf.write(buffer);

    }


    /**
     * Dismount the disk
     */
    public void umount() {
        try {
            if (rf != null)
                rf.close();
        } catch (Exception ex) {
            System.out.println(ex);
        }

        rf = null;
    }

    public String toString() {
        return "Filename " + name + " Sector track " + getSectorTrack();
    }
}


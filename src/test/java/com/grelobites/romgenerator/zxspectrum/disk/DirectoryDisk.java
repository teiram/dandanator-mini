package com.grelobites.romgenerator.zxspectrum.disk;


import com.grelobites.romgenerator.zxspectrum.cpm.CpmDisk;
import com.grelobites.romgenerator.zxspectrum.cpm.DPBHD4MB;

import java.io.File;
import java.io.FileInputStream;

/**
 * $Id: DirectoryDisk.java 331 2010-09-14 10:30:13Z mviara $
 * <p>
 * Directory disk<p>
 * <p>
 * This disk mount one file system directory as one CP/M disk, this
 * implementation is read only and based upon MemoryDisk all change to
 * the disk will be lost when the emulation is terminated.<p>
 * This disk always user 4MB Hard disk DPB.
 * <p>
 * $Log: DirectoryDisk.java,v $
 * Revision 1.2  2004/06/20 16:27:04  mviara
 * Some minor change.
 */
public class DirectoryDisk extends MemoryDisk {
    /**
     * File system directory
     */
    private String directory;

    /**
     * Standard constructor
     */
    public DirectoryDisk(String directory) {
        // Always user 4MB HD DPB
        super(new DPBHD4MB());
        this.directory = directory;
    }


    /**
     * Mount the disk
     */
    public void mount() throws Exception {
        /**
         * Return if already mounted
         */
        if (isMounted())
            return;

        /**
         * Create one new cpm disk
         */
        CpmDisk disk = new CpmDisk(dpb, this);

        /**
         * List the directory
         */
        File file = new File(directory);

        try {
            disk.format();
            disk.mount();

            File files[] = file.listFiles();

            for (int i = 0; i < files.length; i++) {
                file = files[i];

                if (file.isDirectory())
                    continue;

                String name = file.getName().toUpperCase();
                disk.putFile(0, name, new FileInputStream(file));
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        //disk.stat();
        //System.exit(1);
    }

    public String toString() {
        return "Directory " + directory;
    }
}

package com.grelobites.dandanator.util.emulator.zxspectrum.cpm;


/**
 * $Id: DPBHD4MB.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * DPB Used for 4 MB Hard Disk
 * <p>
 * $Log: DPBHD4MB.java,v $
 * Revision 1.2  2004/06/20 16:26:45  mviara
 * CVS ----------------------------------------------------------------------
 * Some minor change.
 */
public class DPBHD4MB extends DPB {
    public DPBHD4MB() {
        super(32);
        bsm = 4;
        blm = 15;
        exm = 0;
        dsm = 2047;
        drm = 255;
        alloc0 = 240;
        alloc1 = 0;
        trackOffset = 0;
    }

    public String toString() {
        return "4MB HD";
    }

}


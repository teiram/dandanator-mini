package com.grelobites.romgenerator.zxspectrum.cpm;

/**
 * $Id: DPB.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Abstract class to rappresent a CPM Disk Parameter Block for one
 * detailed description see static field.
 * <p>
 * $Log: DPB.java,v $
 * Revision 1.2  2004/06/20 16:26:45  mviara
 * CVS ----------------------------------------------------------------------
 * Some minor change.
 */
public abstract class DPB {

    static public final int SPT = 0;
    static public final int BSM = 2;
    static public final int BLM = 3;
    static public final int EXM = 4;
    static public final int DSM = 5;
    static public final int DRM = 7;
    static public final int AL0 = 9;
    static public final int AL1 = 10;
    static public final int OFF = 13;

    public int sectorTrack;            // Sector for track
    public int bsm;
    public int blm;
    public int exm = 0;
    public int dsm;
    public int drm;
    public int alloc0;
    public int alloc1;
    public int trackOffset;
    public int translate[] = null;

    DPB() {
    }

    DPB(int sectorTrack) {
        this.sectorTrack = sectorTrack;
        setSkew(1);
    }

    public void setSkew(int skew) {
        translate = new int[sectorTrack];
        int sector = 1;

        for (int i = 0; i < sectorTrack; i++) {
            translate[i] = sector;
            sector += skew;
            if (sector > sectorTrack)
                sector -= sectorTrack;
        }
    }

    void setTranslation(int s1, int s2) {
        translate[s1 - 1] = s2;
    }

    int translateSector(int sector) {
        return translate[sector - 1];
    }

    public void dump() {
        System.out.println(toString());
        System.out.println("BLM (Block size/128 -1) " + blm);
        System.out.println("DSM " + dsm);
        System.out.println("TrackOffset " + trackOffset);
        System.out.println("Sector track " + sectorTrack);
        System.out.println("AL0 " + alloc0);
        System.out.println("AL1 " + alloc1);
        System.out.println("EXM " + exm);
    }
}


package com.grelobites.romgenerator.zxspectrum.cpm;

/**
 * $Id: DPB3270.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * DBP form IBM 3270 8" disk
 * <p>
 * 77 Track
 * 26 Sector
 * Single side
 * Single density
 * <p>
 * $Log: DPB3270.java,v $
 * Revision 1.2  2004/06/20 16:26:45  mviara
 * CVS ----------------------------------------------------------------------
 * Some minor change.
 */
public class DPB3270 extends DPB {
    public DPB3270() {
        super(26);
        bsm = 3;
        blm = 7;
        exm = 0;
        dsm = 242;
        drm = 63;
        alloc0 = 192;
        alloc1 = 0;
        trackOffset = 2;
        setSkew(6);
        setTranslation(1, 1);
        setTranslation(2, 7);
        setTranslation(3, 13);
        setTranslation(4, 19);
        setTranslation(5, 25);
        setTranslation(6, 5);
        setTranslation(7, 11);
        setTranslation(8, 17);
        setTranslation(9, 23);
        setTranslation(10, 3);
        setTranslation(11, 9);
        setTranslation(12, 15);
        setTranslation(13, 21);
        setTranslation(14, 2);
        setTranslation(15, 8);
        setTranslation(16, 14);
        setTranslation(17, 20);
        setTranslation(18, 26);
        setTranslation(19, 6);
        setTranslation(20, 12);
        setTranslation(21, 18);
        setTranslation(22, 24);
        setTranslation(23, 4);
        setTranslation(24, 10);
        setTranslation(25, 16);
        setTranslation(26, 22);
    }

    public String toString() {
        return "IBM3270";
    }
}

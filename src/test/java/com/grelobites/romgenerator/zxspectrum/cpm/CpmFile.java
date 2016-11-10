package com.grelobites.romgenerator.zxspectrum.cpm;


import java.util.Vector;

/**
 * $Id: CpmFile.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * CP/M file rappresentation.
 * <p>
 * $Log: CpmFile.java,v $
 * Revision 1.2  2004/06/20 16:26:45  mviara
 * CVS ----------------------------------------------------------------------
 * Some minor change.
 */

class IntegerVector extends Vector {
    void add(int n) {
        add(new Integer(n));
    }

    int intAt(int i) {
        Integer ii = (Integer) elementAt(i);
        return ii.intValue();
    }

}

public class CpmFile {
    int user;
    String name;
    IntegerVector blocks = new IntegerVector();
    IntegerVector fcbs = new IntegerVector();
    DPB dpb;


    CpmFile(DPB dpb, int user, String name) {
        this.user = user;
        this.name = name;
        this.dpb = dpb;
    }

    public void addBlock(int block) {
        blocks.add(block);
    }

    public int getBlockCount() {
        return blocks.size();
    }

    public int getBlockAt(int i) {
        return blocks.intAt(i);
    }

    public void addFCB(int fcb) {
        fcbs.add(fcb);

    }

    public int getFCBCount() {
        return fcbs.size();
    }

    public int getFCBAt(int i) {
        return fcbs.intAt(i);
    }


}


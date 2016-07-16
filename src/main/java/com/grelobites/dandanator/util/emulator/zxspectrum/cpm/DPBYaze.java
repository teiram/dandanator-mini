package com.grelobites.dandanator.util.emulator.zxspectrum.cpm;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * $Id: DPBYaze.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * DPB user for YAZE CP/M Disks
 * <p>
 * $Log: DPBYaze.java,v $
 * Revision 1.2  2004/06/20 16:26:45  mviara
 * CVS ----------------------------------------------------------------------
 * Some minor change.
 */
public class DPBYaze extends DPBBuffer {
    DPBYaze() {
    }

    public void setBuffer(String name) throws Exception {
        InputStream is = new FileInputStream(name);
        byte buffer[] = new byte[128];
        is.read(buffer);
        setBuffer(buffer, 32);
        is.close();

    }

    public String toString() {
        return "Yaze Cp/m Disk";
    }
}	

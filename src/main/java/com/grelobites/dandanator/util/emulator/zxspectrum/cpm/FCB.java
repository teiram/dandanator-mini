package com.grelobites.dandanator.util.emulator.zxspectrum.cpm;

/**
 * $Id: FCB.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Directory File Control block
 * <p>
 * Space in CP/M is allocated in 128 byte block called record.<p>
 * <p>
 * Every FCB is 32 byte and have the following layout :<p>
 * <p>
 * 0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F<p>
 * UU N0 N1 N2 N3 N4 N5 N6 N7 T0 T1 T2 EX S1 S2 RC<p>
 * A0 A1 A2 A3 A4 A5 A6 A7 A8 A9 AA AB AC AD AE AF<p>
 * <p>
 * UU		User 0 - 15 in CP/M 1.4,2.2 0-31 in CP/M 3.0<p>
 * If the file is deleted this byte is set to 0xE5<p>
 * N0..N7	File name right filled with spaces<p>
 * T0..T2	File type  right filled with spaces<p>
 * EX		Number of extension 0-31, every extension is 128 record<p>
 * S1		Reserved<p>
 * S2		Reserved<p>
 * RC		Number of record used in the last extension.<p>
 * A0..AF	Block allocation map 1 if BSM < 256.<p>
 *
 * @see com.grelobites.dandanator.util.emulator.zxspectrum.cpm.DPB
 * <p>
 * $Log: FCB.java,v $
 * Revision 1.2  2004/06/20 16:26:45  mviara
 * CVS ----------------------------------------------------------------------
 * Some minor change.
 */
public class FCB {
    /**
     * Entry deleted
     */
    static public final int DELETED = 0xe5;

    /**
     * Offset to user number
     **/
    static public final int USER = 0;

    /**
     * Offset to extension number
     */
    static public final int EX = 12;

    /**
     * Offset to record counter
     */
    static public final int RC = 15;

    /**
     * Byte image of FCB
     */
    private byte fcb[] = new byte[32];

    public FCB() {
        clear();

    }

    /**
     * Clear all the data in the FCB
     */
    public void clear() {
        for (int i = 0; i < fcb.length; i++)
            fcb[i] = 0;
    }

    /**
     * Copy the FCB from one buffer
     */
    public void setBuffer(byte buffer[], int from) {
        for (int i = 0; i < 32; i++)
            this.fcb[i] = buffer[i + from];
    }

    /**
     * Get the space used by the FCB
     */
    public byte[] getBytes() {
        return fcb;
    }

    /**
     * Get the user number
     */
    public int getUser() {
        return fcb[USER] & 0xff;
    }

    /**
     * Set the user number
     */
    public void setUser(int user) {
        fcb[USER] = (byte) user;
    }

    /**
     * Get the filename in the FCB
     */
    public String getFileName() {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < 8; i++)
            if (fcb[i + 1] != ' ')
                sb.append(new String(fcb, i + 1, 1));
        sb.append('.');

        for (int i = 0; i < 3; i++)
            if (fcb[i + 9] != ' ')
                sb.append(new String(fcb, i + 9, 1));

        String fileName = sb.toString();

        if (fileName.endsWith("."))
            fileName = fileName.substring(0, fileName.length() - 1);

        return fileName;
    }

    /**
     * Set the filename in the FCB
     */
    public void setFileName(String name) {
        int i, j;

        for (i = 0; i < 11; i++)
            fcb[i + 1] = (byte) ' ';

        for (i = 0, j = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.') {
                j = 8;
                continue;
            }
            fcb[1 + j++] = (byte) c;

        }

    }

    /**
     * Set this FCB as deleted
     */
    public void setDeleted() {
        fcb[0] = (byte) DELETED;
    }

    /**
     * Return true if this FCB is deleted
     */
    public boolean getDeleted() {
        return fcb[USER] == (byte) DELETED ? true : false;
    }

    /**
     * Get record counter
     */
    public int getRC() {
        return fcb[RC] & 0xff;
    }

    /**
     * Set record counter
     */
    public void setRC(int rc) {
        fcb[RC] = (byte) rc;
    }

    /**
     * Get the exrension
     */
    public int getEX() {
        return fcb[EX] & 0xff;
    }

    /**
     * Set the extension
     */
    public void setEX(int ex) {
        fcb[EX] = (byte) ex;
    }

    /**
     * Get one allocation block using 1 byte for block
     */
    public int getBlockByte(int block) {
        return fcb[16 + block] & 0xff;
    }

    /**
     * Get one allocation block using 1 word for block
     */
    public int getBlockWord(int block) {
        return (fcb[16 + block * 2 + 0] & 0xff) +
                ((fcb[16 + block * 2 + 1]) & 0xff) * 256;
    }

    public void setBlockByte(int block, int value) {
        fcb[16 + block] = (byte) value;
    }

    public void setBlockWord(int block, int value) {
        setBlockByte(block * 2 + 0, value & 0xff);
        setBlockByte(block * 2 + 1, value >> 8);
    }

    /**
     * Clear all the allocation block map
     */
    public void clearBlocks() {
        for (int i = 0; i < 16; i++)
            fcb[16 + i] = 0;
    }

}
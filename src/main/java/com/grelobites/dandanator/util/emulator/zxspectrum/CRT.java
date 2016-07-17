package com.grelobites.dandanator.util.emulator.zxspectrum;

/**
 * $Id: CRT.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Physical CRT interface<p>
 * <p>
 * Represents a physical device suitable for terminal emulation.
 * <p>
 */
public interface CRT {
    /**
     * Normal attribute
     */
    byte NORMAL = 0;

    /**
     * Reverse attribute
     */
    byte REVERSE = 1;

    /**
     * Hi light attribute
     */
    byte HI = 2;

    /**
     * Underline attribute
     */
    byte UNDERLINE = 4;

    /**
     * Blinking attribute
     */
    byte BLINKING = 8;

    void scrollDown(int from, int size, int n);

    void scrollUp(int from, int size, int n);

    void setChar(int pos, char c);

    void setAtt(byte a);

    boolean getCursor();

    void setCursor(boolean mode);

    void setCursor(int r, int c);

    int getRow();

    void setRow(int r);

    int getCol();

    void setCol(int c);

    int getNumCol();

    void setNumCol(int col);

    int getNumRow();

    void setNumRow(int row);

    int getScreenSize();

    void setFontSize(int size);

    void init();

    void reset();

    boolean consoleStatus();

    int consoleInput();

    void printStatus(int pos, String status);

    /**
     * Add new key to the console buffer
     */
    void addKey(int key);

    /**
     * Add one string to the console buffer
     */
    void addKey(String s);

    /**
     * Define a new function key
     *
     * @param key - Function key @see java.awt.KeyEvent
     * @param s   - String with the String definition of the key
     */
    void defineKey(int key, String s);

    /**
     * Define a function key
     *
     * @param key  - Function key @see java.awt.KeyEvent
     * @param code - Ascii code of the key
     */
    void defineKey(int key, int code);

    /**
     * Define a function key
     *
     * @param key  - Function key @see java.awt.KeyEvent
     * @param code - Ascii code of the key
     */
    void defineKey(int key, char code);


    /**
     * Return function key definition
     *
     * @param key - Function key
     * @return The string associated of null
     */
    String getDefinedKey(int key);

    void terminate();

    boolean isTerminate();

}
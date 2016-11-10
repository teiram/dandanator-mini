package com.grelobites.romgenerator.zxspectrum;


public interface VDU extends Peripheral {
    void showIdle(boolean mode);

    void showUtilization(int cpu);

    void println(String s);

    void print(String s);

    /**
     * Set the physical device
     */
    void setCRT(CRT crt);

    /**
     * Set the number of requested row
     */
    void setNumRow(int row);

    /**
     * Return the number of screew col
     */
    int getNumCol();

    /**
     * Set the number of requested col
     */
    void setNumCol(int col);

    boolean isTerminate();
}


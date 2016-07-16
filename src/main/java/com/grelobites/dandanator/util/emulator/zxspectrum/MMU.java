package com.grelobites.dandanator.util.emulator.zxspectrum;

/**
 * $Id: MMU.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Memory manager unit for J80 system.
 * <p>
 * Must provide the basic read/write funcione to the phisical Z80
 * memory.
 * <p>
 * $Log: MMU.java,v $
 * Revision 1.2  2004/06/20 16:27:29  mviara
 * Some minor change.
 */
public interface MMU extends Peripheral {
    /**
     * Read one byte
     *
     * @param add - Address Memory 0x0000-0xffff
     * @return byte of memory
     */
    public int peekb(int add);

    /**
     * Write one byte
     *
     * @param add   - Address Memory 0x0000-0xffff
     * @param value - Value to assign
     */
    public void pokeb(int add, int value);
}

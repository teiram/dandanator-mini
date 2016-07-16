package com.grelobites.dandanator.util.emulator.zxspectrum;

/**
 * $Id: Peripheral.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * This interface must be implemented from any Z80 peripheral.
 * <p>
 * $Log: Peripheral.java,v $
 * Revision 1.2  2004/06/20 16:27:29  mviara
 * Some minor change.
 */
public interface Peripheral {
    /**
     * Register the peripheral with the cpu after this call the
     * peripheral must be ready to be used.
     */
    public void connectCPU(J80 cpu) throws Exception;

    /**
     * Disconect the cpu from the peripheral, after this call the
     * peripheral must not have any pending operation and reset all the
     * state to the default.
     */
    public void disconnectCPU(J80 cpu) throws Exception;

    /**
     * Reset the cpu
     */
    public void resetCPU(J80 cpu) throws Exception;

}

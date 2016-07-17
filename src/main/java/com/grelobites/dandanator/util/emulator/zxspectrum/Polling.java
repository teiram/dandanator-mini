package com.grelobites.dandanator.util.emulator.zxspectrum;

/**
 * $Id: Polling.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Polling peripheral.
 * <p>
 * $Log: Polling.java,v $
 * Revision 1.2  2004/06/20 16:27:29  mviara
 * Some minor change.
 */
public interface Polling {
    public void polling(Z80VirtualMachine cpu);
}
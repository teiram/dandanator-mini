package com.grelobites.dandanator.util.emulator.zxspectrum;

/**
 * $Id: OutPort.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Interface to trap one Output port.
 * <p>
 * $Log: OutPort.java,v $
 * Revision 1.2  2004/06/20 16:27:29  mviara
 * Some minor change.
 */
public interface OutPort {
    public void outb(int port, int value, int states) throws Exception;
}

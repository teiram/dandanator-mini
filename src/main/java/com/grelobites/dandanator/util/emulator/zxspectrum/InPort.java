package com.grelobites.dandanator.util.emulator.zxspectrum;

/**
 * $Id: InPort.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Interface to trap one input port
 * <p>
 * $Log: InPort.java,v $
 * Revision 1.2  2004/06/20 16:27:29  mviara
 * Some minor change.
 */
public interface InPort {
    /**
     * Input port
     *
     * @param port - Specify the Z80 input port
     */
    public int inb(int port, int hi) throws Exception;
}

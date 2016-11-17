/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.grelobites.romgenerator.z80core;

/**
 *
 * @author jsanchez
 */
public interface Z80operations {
    int fetchOpcode(int address);

    int peek8(int address);
    void poke8(int address, int value);
    int peek16(int address);
    void poke16(int address, int word);

    int inPort(int port);
    void outPort(int port, int value);

    void contendedStates(int address, int tstates);
    
    void breakpoint();
    void execDone();
}

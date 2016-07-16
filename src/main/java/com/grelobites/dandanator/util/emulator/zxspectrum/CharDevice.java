package com.grelobites.dandanator.util.emulator.zxspectrum;

import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * $Id: CharDevice.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Class to implements a generic character device, one port of the Z80
 * is trap and every output on this port are written to the file
 * specified.
 * <p>
 * $Log: CharDevice.java,v $
 * Revision 1.3  2004/06/20 16:27:29  mviara
 * Some minor change.
 */
public class CharDevice implements Peripheral, OutPort {
    private int port;
    private String device = null;
    private OutputStream os = null;

    /**
     * Constructor with device and port
     *
     * @param device - Device file name
     * @param port   - Z80 output port associated
     */
    public CharDevice(String device, int port) {
        setDevice(device);
        setPort(port);
    }

    public CharDevice() {
    }

    public void setDevice(String device) {
        this.device = device;

    }

    public void setPort(int port) {
        this.port = port;
    }

    public void resetCPU(J80 cpu) {
    }

    public void disconnectCPU(J80 cpu) {
        // Close the output stream if open
        if (os != null) {
            try {
                os.close();
                os = null;
            } catch (Exception e) {
            }
        }
    }

    public void connectCPU(J80 cpu) throws Exception {
        if (port == 0)
            throw new Exception("CharDevice : no port configured");
        if (device == null)
            throw new Exception("CharDevice : no file configured");

        cpu.addOutPort(port, this);
    }

    /**
     * Handle output to the device
     */
    public void outb(int port, int bite, int tstates) throws Exception {
        // Open the output stream if closed
        if (os == null) {
            try {
                os = new FileOutputStream(device, true);
            } catch (Exception e) {
                os = null;
                return;
            }
        }
        os.write(bite);
    }

    public String toString() {
        return "AUX : " + device + " port " + port + " $Revision: 330 $";
    }
}

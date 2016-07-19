package com.grelobites.dandanator.util.emulator.zxspectrum;

import com.grelobites.dandanator.util.emulator.zxspectrum.disk.DirectoryDisk;
import com.grelobites.dandanator.util.emulator.zxspectrum.disk.ImageDisk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * <p>
 * Floppy disk controller peripheral
 * <p>
 * The FDC is a I/O mapped device.
 * <p>
 * To select a drive the DRIVE register must be written and the status read
 * If the disk is ready the status will be SUCCESS.
 * <p>
 * For READ/WRITE operation all registers (DRIVE,TRACK,SECTOR,DMA) must be filled
 * and then the command (READ or WRITE) must be issued in the CMD
 * register. Afterwards the status can be read
 */
public class FDC implements Peripheral, OutputPort, InputPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(FDC.class);

    /**
     * FDC Command READ
     */
    public static final int READ = 0;
    /**
     * FDC Command write
     */
    public static final int WRITE = 1;
    /**
     * FDC Status ok
     */
    public static final int SUCCESS = 0;
    /**
     * FDC Status error
     */
    public static final int ERROR = 1;
    /**
     * Disk select register 0 - 15
     */
    protected int DRIVE = 10;
    /**
     * Track low register (bit 0-7)
     */
    protected int TRACKLOW = 11;
    /**
     * Track hi register (bir 8-15)
     */
    protected int TRACKHI = 12;
    /**
     * Sector register (1-255)
     */
    protected int SECTORLOW = 13;
    protected int SECTORHI = 18;
    /**
     * Command register
     */
    protected int CMD = 14;
    /**
     * Status register
     */
    protected int STATUS = 15;
    /**
     * Dma loh address register (bit 0 -7)
     */
    protected int DMALOW = 16;
    /**
     * Dma hi address register (bit 8-15)
     */
    protected int DMAHI = 17;
    /**
     * Current selection
     */
    protected int drive = -1;
    protected int track = 0;
    protected int sector = 0;
    protected int dma = 0;
    // Last command result
    protected int commandResult = SUCCESS;
    // Flag true when a command has been processed
    private boolean commandInProcess = false;
    // Array with all the supported disk
    private Disk disks[] = new Disk[16];

    // Attached cpy
    private Z80VirtualMachine cpu;

    public FDC() {
        for (int i = 0; i < disks.length; i++)
            disks[i] = null;
    }

    void setDisk(int unit, Disk disk) {
        disks[unit] = disk;
    }

    void setDisk(int unit, String name, int nsector) throws Exception {
        File file = new File(name);

        if (file.isDirectory())
            disks[unit] = new DirectoryDisk(name);
        else
            disks[unit] = new ImageDisk(name, nsector);
    }

    public int inb(int port, int hi) {
        if (commandInProcess) {
            commandInProcess = false;
            return commandResult;
        }

        if (drive < 0 || drive >= 15)
            return ERROR;

        try {
            Disk disk = disks[drive];
            if (disk.isMounted())
                return SUCCESS;
            disk.mount();
        } catch (Exception ex) {
            return ERROR;
        }

        return SUCCESS;
    }

    public void outb(int port, int value, int states) {
        if (port == DRIVE)
            drive = value;
        else if (port == SECTORLOW) {
            sector &= 0xff00;
            sector = value;
        } else if (port == TRACKLOW) {
            track &= 0xff00;
            track |= value;
        } else if (port == TRACKHI) {
            track &= 0x00ff;
            track |= value << 8;
        } else if (port == DMALOW) {
            dma &= 0xff00;
            dma |= value;
        } else if (port == DMAHI) {
            dma &= 0x00ff;
            dma |= value << 8;

        } else if (port == CMD)
            diskio(value);

    }

    void readSector(int drive, int track, int sector, byte buffer[]) throws Exception {
        Disk unit = disks[drive];

        unit.read(track, sector, buffer);

    }

    void diskio(int cmd) {
        byte buffer[] = new byte[128];


        //System.out.println("DISKIO Drive = "+drive+" CMD = "+cmd+" T = "+track+" S = "+sector+" DMA = "+Integer.toHexString(dma));

        commandInProcess = true;
        commandResult = ERROR;

        if (drive < 0 || drive > 15)
            return;

        Disk unit = disks[drive];

        if (unit == null)
            return;


        try {
            switch (cmd) {
                case READ:
                    unit.read(track, sector, buffer);

                    for (int i = 0; i < 128; i++)
                        cpu.pokeb(dma + i, buffer[i] & 0xff);
                    break;

                case WRITE:
                    for (int i = 0; i < 128; i++)
                        buffer[i] = (byte) cpu.peekb(dma + i);

                    unit.write(track, sector, buffer);
                    break;
                default:
                    return;
            }
            commandResult = SUCCESS;

        } catch (Exception e) {
            LOGGER.error("In Disk I/O operation", e);
        }
    }

    public void onCpuReset(Z80VirtualMachine cpu) {
    }

    public void unbind(Z80VirtualMachine cpu) {
    }

    public void bind(Z80VirtualMachine cpu) {
        this.cpu = cpu;
        cpu.addInPort(STATUS, this);
        cpu.addOutPort(DRIVE, this);
        cpu.addOutPort(TRACKLOW, this);
        cpu.addOutPort(TRACKHI, this);
        cpu.addOutPort(SECTORLOW, this);
        cpu.addOutPort(SECTORHI, this);
        cpu.addOutPort(CMD, this);
        cpu.addOutPort(DMALOW, this);
        cpu.addOutPort(DMAHI, this);

    }

    public String toString() {
        StringBuilder buf = new StringBuilder("FDC : Disk image");

        for (int i = 0; i < 16; i++)
            if (disks[i] != null) {
                String d = "ABCDEFGHIJKLMNOP";
                buf.append("\n  Disk ").append(d.charAt(i)).append(" ").append(disks[i].toString());
            }

        return buf.toString();
    }

}




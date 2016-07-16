package com.grelobites.dandanator.util.emulator.zxspectrum;

import com.grelobites.dandanator.util.emulator.zxspectrum.disk.DirectoryDisk;
import com.grelobites.dandanator.util.emulator.zxspectrum.disk.ImageDisk;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 * $Id: J80.java 341 2010-09-15 09:31:54Z mviara $
 * <p>
 * J80 is a complete Z80 extensible  virtual machine.
 * <p>
 * This main class load from one configuration file (default j80.conf)
 * all the configured pheripheral and start the emulator.
 * <p>
 * $Log: J80.java,v $
 * Revision 1.8  2008/05/15 17:07:17  mviara
 * Added preliminary support for Z80Pack. Fixed bug in idle loop detection.
 * <p>
 * Revision 1.7  2008/05/14 16:53:09  mviara
 * Added configuration cmmand bootload.
 * <p>
 * Revision 1.6  2007/06/21 10:23:32  mviara
 * Added support for variable number of lines in CRT.
 * <p>
 * Revision 1.5  2005/03/18 16:40:02  mviara
 * Added support for stepper.
 * <p>
 * Revision 1.4  2004/07/18 11:23:47  mviara
 * Added support for pause,resume and load snapshot on the fly.
 * <p>
 * Revision 1.3  2004/06/20 16:27:29  mviara
 * Some minor change.
 */
public class J80 extends Z80 {
    static public String version = "J80 : Java Z80 Version 1.10a";
    static private boolean exit = false;
    private long numOutput = 0;
    private InPort inport[] = new InPort[64 * 1024];
    private OutPort outport[] = new OutPort[64 * 1024];
    private Vector pollers = new Vector();
    private MMU mmu;
    private boolean initializeMMU = false;
    private VDU vdu = null;
    private FDC fdc = null;
    private CRT crt = null;
    private Snapshot sn = null;
    private Vector snapshots = new Vector();
    private Vector peripherals = new Vector();
    private float mhz = 5;
    private long sleeped = 0;
    private boolean running = false;
    private boolean idle = false;
    private boolean lastIdle = false;
    private boolean paused = false;
    private Vector steppers = new Vector();
    private boolean trapOutput = false;
    private boolean trapInput = false;

    public J80() {
        //super(5.0);


        for (int i = 0; i < 64 * 1024; i++) {
            inport[i] = null;
            outport[i] = null;
        }


    }

    static public void terminate() {
        exit = true;
    }

    static public void main(String argv[]) {
        J80 cpu = null;
        try {
            System.out.println(version);
            cpu = new J80();
            if (argv.length > 0) {
                for (int i = 0; i < argv.length; i++)
                    cpu.config(argv[i]);
            } else
                cpu.config("j80.conf");

            cpu.start();

        } catch (Exception ex) {
            ex.printStackTrace();

            cpu.Error(ex);
            System.out.println(ex);
            ex.printStackTrace();
            System.exit(1);
        }


    }

    /**
     * Initialize the J80 virtual machine
     */
    public void init() throws Exception {
        reset();


        if (vdu != null) {
            vdu.println(version + " " + mhz + " MHz");
        }


        for (int i = 0; i < peripherals.size(); i++) {
            Peripheral p = (Peripheral) peripherals.elementAt(i);
            println(p.toString());
        }


    }


    public void addPeripheral(Peripheral p) throws Exception {

        // Remember peripheral family

        if (p instanceof Snapshot) {
            this.sn = (Snapshot) p;
        }

        if (p instanceof VDU) {
            this.vdu = (VDU) p;
            vdu.setCRT(crt);
        }

        if (p instanceof MMU) {
            this.mmu = (MMU) p;
            initializeMMU = true;
        }

        if (p instanceof FDC)
            this.fdc = (FDC) p;

        peripherals.add(p);

        // Add the cpu
        p.connectCPU(this);

        // If necessary initialize MMU
        if (initializeMMU) {
            initializeMMU = false;
            for (int i = 0; i < 0x10000; i++)
                pokeb(i, 0);
        }

        p.resetCPU(this);
    }

    public void addStepper(Stepper s) {
        steppers.add(s);
    }

    public void step() {
        for (int i = 0; i < steppers.size(); i++) {
            Stepper s = (Stepper) steppers.elementAt(i);
            s.step(this);
        }

    }

    public void addPolling(int interval, Polling polling) {
        Poller p = new Poller();
        p.polling = polling;
        p.interval = interval;
        p.elapsed = 0;
        pollers.add(p);
    }

    public void addInPort(int port, InPort trap) {
        inport[port] = trap;
    }

    public void addOutPort(int port, OutPort trap) {
        outport[port] = trap;
    }

    public long getNumOutput() {
        return numOutput;
    }

    public void outb(int port, int bite, int tstates) {
        int portHi = port + tstates * 256;

        //System.out.println("Outb "+Integer.toHexString(port)+" "+Integer.toHexString(portHi)+" = "+Integer.toHexString(bite));

        numOutput++;

        // Try hi port registreed
        if (outport[portHi] != null)
            port = portHi;

        try {
            if (outport[port] == null) {
                if (trapOutput)
                    Error("out " + Integer.toHexString(port) + " (" + Integer.toHexString(portHi) + ") = " +
                            Integer.toHexString(bite));
                //Error("out "+port+" = "+bite);
            } else
                outport[port].outb(port, bite, tstates);
        } catch (Exception ex) {
            Error(ex);
        }
    }

    public int peekb(int add) {
        return mmu.peekb(add);
    }

    public void pokeb(int add, int value) {
        mmu.pokeb(add, value);
    }

    public int inb(int port, int hi) {
        int result = 0;

        if (hi > 0) {
            int portHi = port + hi * 256;

            if (inport[portHi] != null)
                port = portHi;
        }

        try {
            if (inport[port] == null) {
                result = 0xff;
                if (trapInput) {
                    Error("In " + Integer.toHexString(port) + " " + Integer.toHexString(hi));
                }
            } else
                result = inport[port].inb(port, hi);
        } catch (Exception e) {
            Error(e);
        }

        return result;
    }

    private int getWord(String s, int pos) throws Exception {
        int value = (getByte(s, pos + 0) << 8) + getByte(s, pos + 2);
        return value;
    }

    private int getDigit(String s, int pos) throws Exception {
        String digits = "0123456789ABCDEF";

        int i = digits.indexOf(s.charAt(pos));

        if (i == -1) {
            digits = digits.toLowerCase();
            i = digits.indexOf(s.charAt(pos));
        }

        if (i == -1)
            throw new ParseException(s, pos);

        return i;
    }

    private int getByte(String s, int pos) throws Exception {
        int value = (getDigit(s, pos + 0) << 4) + getDigit(s, pos + 1);

        return value;
    }

    public void loadSnapshot(String name) throws Exception {
        if (sn == null)
            throw new Exception("snapshot require Snapshot peripheral");
        sn.loadSnapshot(this, name);

    }

    /**
     * Load one intel file format in memory
     */
    public void loadIntel(String name) throws Exception {
        BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(name)));
        String s;
        int data[] = new int[256];
        int count = 0;

        System.out.println("Load intel " + name);

        while ((s = rd.readLine()) != null) {
            if (s.charAt(0) != ':')
                throw new ParseException(s, 0);

            int calc;

            int len = getByte(s, 1);
            int offset = getWord(s, 3);
            int type = getByte(s, 7);
            int i;

            for (i = 0; i < len; i++)
                data[i] = getByte(s, 9 + i * 2);

            for (i = 0, calc = 0; i < len + 4; i++)
                calc += getByte(s, 1 + i * 2);

            calc &= 0xff;
            calc = 0x100 - calc;
            calc &= 0xff;
            int chksum = getByte(s, 9 + len * 2);

            if (chksum != calc)
                throw new Exception(s + " invalid chksum");

            if (type == 0) {
                println("loading " + len + " bytes at " + Integer.toHexString(offset));
                for (i = 0; i < len; i++)
                    pokeb(offset + i, data[i]);

                count += len;
            } else if (type == 1) {
                break;
            } else {
                throw new Exception("Invalid record type " + type + " in " + s);
            }
        }

        rd.close();

        println("loaded " + count + " bytes from " + name);
    }


    public void Error(Exception ex) {
        String s = ex.getMessage();
        if (s == null)
            s = "";
        if (s.length() == 0)
            s = ex.toString();

        Error(s);
    }


    public void Error(String s) {
        if (vdu != null) {
            try {
                vdu.disconnectCPU(this);
            } catch (Exception e) {
            }
        }

        System.err.println(s);
        System.exit(1);
    }


    public void println(String s) {
        if (vdu != null) {
            vdu.println(s);
        }
    }

    public void load(InputStream is, int location) throws Exception {
        load(is, location, -1);
    }

    public void load(InputStream is, int location, int counter) throws Exception {
        int c;
        int count = 0;
        System.out.println("Load  at " + Integer.toHexString(location));

        while ((c = is.read()) != -1) {
            pokeb(location++, c);
            count++;
        }

        if (counter > 0 && count != counter)
            throw new Exception("Unexpected end of stream");

        println(" " + Integer.toHexString(count) + " bytes loaded");

    }

    /**
     * Load one binary file in memory.
     *
     * @param name     - File name
     * @param location - Location in memory
     */
    public void load(String name, int location) throws Exception {
        int c;
        println("Loading " + name);
        FileInputStream is = new FileInputStream(name);
        load(is, location);
        is.close();
    }

    private StringTokenizer parseLine(String s) {
        int i = s.indexOf('=');

        if (i < 0) {
            Error("Invalid configuration line : " + s);
        }
        s = s.substring(i + 1);

        return new StringTokenizer(s, ",");
    }

    private void configTrapOutput(String s) {
        StringTokenizer st = parseLine(s);
        trapOutput = st.nextToken().equalsIgnoreCase("yes") ? true : false;

    }

    private void configTrapInput(String s) {
        StringTokenizer st = parseLine(s);
        trapInput = st.nextToken().equalsIgnoreCase("yes") ? true : false;

    }

    private void configCrt(String s) throws Exception {
        StringTokenizer st = parseLine(s);
        if (st.countTokens() < 1) {
            Error("Invalid crt in line :" + s);
        }

        s = st.nextToken();
        Class c = Class.forName(s);
        crt = (CRT) c.newInstance();
        int numCol = 0;
        int numRow = 0;
        int fontSize = 0;

        if (st.hasMoreElements()) {
            numCol = Integer.parseInt(st.nextToken(), 10);
            if (st.hasMoreElements()) {
                numRow = Integer.parseInt(st.nextToken(), 10);
                if (st.hasMoreElements()) {
                    fontSize = Integer.parseInt(st.nextToken(), 10);
                }
            }
        }

        if (numCol != 0)
            crt.setNumCol(numCol);
        if (numRow != 0)
            crt.setNumRow(numRow);
        if (fontSize != 0)
            crt.setFontSize(fontSize);
    }

    private void configPeripheral(String line) throws Exception {
        String s;

        StringTokenizer st = parseLine(line);

        if (st.countTokens() < 1) {
            Error("Invalid peripheral in line :" + line);
        }


        s = st.nextToken();
        Class c = Class.forName(s);
        Peripheral p = (Peripheral) c.newInstance();
        Class stringClass = new String().getClass();

        // Call parameters method
        while (st.hasMoreElements()) {
            Object args[] = new Object[1];
            args[0] = null;
            String method = st.nextToken();
            String param = st.nextToken();
            java.lang.reflect.Method ms[] = c.getMethods();


            for (int i = 0; i < ms.length; i++) {
                java.lang.reflect.Method m = ms[i];

                if (m.getName().equals(method)) {
                    Class classes[] = m.getParameterTypes();
                    if (classes.length > 1)
                        continue;

                    if (classes[0].equals(stringClass)) {
                        args[0] = param;
                    }

                    if (classes[0].equals(Integer.TYPE)) {
                        args[0] = new Integer(Integer.parseInt(param));
                    }

                    if (args[0] != null) {
                        m.invoke(p, args);
                        break;
                    }
                }
            }


            if (args[0] == null) {
                Error("Invalid parameters : " + line);
            }
        }

        addPeripheral(p);

    }

    private void configDisk(int unit, Disk disk) throws Exception {
        if (fdc == null) {
            Error("No FDC installed");
        }
        fdc.setDisk(unit, disk);
    }

    private void configDiskImage(String name) throws Exception {
        StringTokenizer st = parseLine(name);

        if (st.countTokens() != 3) {
            Error("Invalid disk image in : " + name);
        }

        int unit = Integer.parseInt(st.nextToken());
        String file = st.nextToken();
        int sector = Integer.parseInt(st.nextToken());

        configDisk(unit, new ImageDisk(file, sector));
    }

    private void configDiskDir(String name) throws Exception {
        StringTokenizer st = parseLine(name);

        if (st.countTokens() != 2) {
            Error("Invalid disk directory in : " + name);
        }

        int unit = Integer.parseInt(st.nextToken());
        String file = st.nextToken();

        configDisk(unit, new DirectoryDisk(file));
    }

    private void configMhz(String s) throws Exception {
        StringTokenizer st = parseLine(s);
        mhz = Float.parseFloat(st.nextToken());
    }

    public float getMhz() {
        return mhz;
    }

    private void configSnapshot(String s) throws Exception {
        if (sn == null)
            throw new Exception("snapshot require Snapshot peripheral");

        StringTokenizer st = parseLine(s);
        snapshots.add(st.nextToken());
    }

    private void bootLoad(String s) throws Exception {
        vdu.println("Load boot from disk 0, Track 0, Sector 1");
        byte buffer[] = new byte[128];
        fdc.readSector(0, 0, 1, buffer);
        for (int i = 0; i < 128; i++)
            pokeb(i, buffer[i] & 0xff);
    }

    private void configLoad(String s) throws Exception {
        StringTokenizer st = parseLine(s);

        String name = st.nextToken();
        int address = 0;
        if (st.hasMoreElements())
            address = Integer.parseInt(st.nextToken(), 16);
        load(name, address);
    }

    private void configInclude(String s) throws Exception {
        StringTokenizer st = parseLine(s);

        config(st.nextToken());
    }


    private void config(String name) throws Exception {
        BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(name)));
        String s;

        println("Config from " + name);
        while ((s = rd.readLine()) != null) {

            if (s.startsWith("#"))
                continue;
            if (s.startsWith("crt"))
                configCrt(s);
            else if (s.startsWith("peripheral"))
                configPeripheral(s);
            else if (s.startsWith("diskimage"))
                configDiskImage(s);
            else if (s.startsWith("diskdir"))
                configDiskDir(s);
            else if (s.startsWith("load"))
                configLoad(s);
            else if (s.startsWith("bootload"))
                bootLoad(s);
            else if (s.startsWith("mhz"))
                configMhz(s);
            else if (s.startsWith("include"))
                configInclude(s);
            else if (s.startsWith("snapshot"))
                configSnapshot(s);
            else if (s.startsWith("trapoutput"))
                configTrapOutput(s);
            else if (s.startsWith("trapinput"))
                configTrapInput(s);

        }

        rd.close();
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public boolean isRunning() {
        return running;
    }

    void start() throws Exception {
        int numCol = 80;
        if (vdu != null)
            numCol = vdu.getNumCol();

        if (mmu == null) {
            Error("No MMU installed");
        }

        init();

        for (int i = 0; i < snapshots.size(); i++) {
            String name = (String) snapshots.elementAt(i);

            sn.loadSnapshot(this, name);
        }

        running = true;

        long start = System.currentTimeMillis();
        long start100ms = start;
        long lastDisplay = start;

        int counter = 0;


        int tmp = 0;
        int states = (int) (mhz * (float) 1000);
        while (!exit) {
            while (paused) {
                try {
                    Thread.sleep(10);
                } catch (Exception ex) {
                }
            }


            exec(states);
            counter += 1;

            // Check polling device
            for (int i = 0; i < pollers.size(); i++) {
                Poller p = (Poller) pollers.elementAt(i);
                if (p.elapsed++ >= p.interval) {
                    p.elapsed = 0;
                    p.polling.polling(this);
                }
            }


            long now = System.currentTimeMillis();

            if (now - start100ms > 100) {
                if (vdu != null) {
                    if (idle != lastIdle) {
                        vdu.showIdle(idle);
                    }
                }

                lastIdle = idle;
                idle = false;
                start100ms = now;

            }

            long elapsed = now - start;

            if (elapsed < counter) {
                sleep();
            }


            if (elapsed + sleeped > 1000) {

                if (vdu != null) {
                    elapsed = ((elapsed - sleeped) * 100) / (counter);
                    vdu.showUtilization((int) elapsed);
                    start = now;
                    counter = 0;
                    sleeped = 0;
                    start = System.currentTimeMillis();
                    exit = vdu.isTerminate();
                    if (exit) {
                        vdu.println("\nTerminated by user");
                        System.out.println("\nTerminated by user");
                    }

                }

            }


        }


        for (int i = peripherals.size() - 1; i >= 0; i--) {
            Peripheral p = (Peripheral) peripherals.elementAt(i);
            p.disconnectCPU(this);
        }

        System.exit(0);

    }

    public void sleep() {
        long start = System.currentTimeMillis();
        idle = true;
        try {
            Thread.sleep(5);
        } catch (Exception ex) {
        }
        sleeped += System.currentTimeMillis() - start;

    }

    public void PC(int value) {
        System.out.println("Set PC " + Integer.toHexString(value));
        PC = value & 0xffff;
    }

    public void B(int value) {
        B = value & 0xff;
    }

    public void E(int value) {
        E = value & 0xff;
    }

    public void D(int value) {
        D = value & 0xff;
    }

    public void H(int value) {
        H = value & 0xff;
    }

    public void L(int value) {
        L = value & 0xff;
    }

    public void C(int value) {
        C = value & 0xff;
    }

    public void A(int value) {
        A = value & 0xff;
    }

    public void F(int value) {
        F = value & 0xff;
    }

    public void I(int value) {
        I = value & 0xff;
    }

    public void R(int value) {
        R = value & 0xff;
    }

    public void SP(int value) {
        SP = value & 0xffff;
    }

    public void IM(int n) {
        IM = n & 0xff;
    }

    public void poppc() {
        PC = pop();
    }

    /**
     * Helper class to track Polling
     */
    private class Poller {
        Polling polling;
        int interval;
        int elapsed;
    }
}

   
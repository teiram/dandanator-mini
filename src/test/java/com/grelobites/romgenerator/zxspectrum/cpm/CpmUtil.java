package com.grelobites.romgenerator.zxspectrum.cpm;

import com.grelobites.romgenerator.zxspectrum.Disk;
import com.grelobites.romgenerator.zxspectrum.disk.ImageDisk;
import com.grelobites.romgenerator.zxspectrum.disk.YazeDisk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

/**
 * $Id: CpmUtil.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Utility fro read / write CP/M imake disk.
 * <p>
 * $Log: CpmUtil.java,v $
 * Revision 1.2  2004/06/20 16:26:45  mviara
 * CVS ----------------------------------------------------------------------
 * Some minor change.
 */
class WildcardFileFilter implements FileFilter {

    private boolean mAllowdirectories;
    private String mNamePattern;

    private String mExtensionPattern;

    /**
     * Creates a <tt>WildcardFileFilter</tt> with optional allowing
     * directories in the accepted files.
     *
     * @param filter The wildcard string to filter on.
     * @param allow  Whether to allow directories or not.
     */
    public WildcardFileFilter(String filter, boolean allow) {
        mAllowdirectories = allow;

        int lastdot = filter.lastIndexOf('.');
        if (lastdot > -1) {
            mNamePattern = filter.substring(0, lastdot).toLowerCase();
            mExtensionPattern = filter.substring(lastdot + 1).toLowerCase();
        } else {
            mNamePattern = filter.toLowerCase();
            mExtensionPattern = null;
        }
    }

    /**
     * Creates a <tt>WildcardFileFilter</tt> which doesn't allow
     * directories in the accepted files.
     *
     * @param filter The wildcard string to filter on.
     */
    public WildcardFileFilter(String filter) {
        this(filter, false);
    }

    static boolean isWildCard(String name) {
        if (name.indexOf('*') >= 0 || name.indexOf('?') >= 0)
            return true;

        return false;

    }

    /**
     * Compares two strings, one containing <tt>*</tt> and <tt>&#063;</tt>
     * wildcard characters, and checks if they match.
     *
     * @param test    The string to test.
     * @param pattern The string containing wildcards to match on.
     * @return <tt>true</tt> if the strings match, <tt>false</tt> otherwise.
     */
    public static boolean wildcardCompare(String test, String pattern) {
        int i = 0;

        // Check the characters
        while (i < test.length() && i < pattern.length()) {
            // If we hit a *, it doesn't matter what the rest of the string is
            if (pattern.charAt(i) == '*') {
                return true;
            }
            // If the characters differ and the wildcard isn't ?, there is
            // no match
            if (pattern.charAt(i) != '?' && test.charAt(i) != pattern.charAt(i)) {
                return false;
            }
            i++;
        }

        // If there are still characters left in test, there is no match as
        // any *s will have been hit in the loop above
        if (i < test.length()) {
            return false;
        }

        // If there is anything other than ? and * left in pattern, no match
        while (i < pattern.length()) {
            if (pattern.charAt(i) != '?' && pattern.charAt(i) != '*') {
                return false;
            }
            i++;
        }

        // If we got this far, the string must be ok
        return true;
    }

    /**
     * Tests whether or not the specified abstract pathname should be
     * included in a pathname list.
     *
     * @param pathname The abstract pathname to be tested.
     * @return <tt>true</tt> if and only if <tt>pathname</tt> should be
     * included.
     */
    public boolean accept(File pathname) {
        if (pathname.isDirectory()) {
            return mAllowdirectories;
        }
        String filename = pathname.getName().toLowerCase();
        int lastdot = filename.lastIndexOf('.');
        String name;
        String extension;
        if (lastdot > -1) {
            name = filename.substring(0, lastdot);
            extension = filename.substring(lastdot + 1);
        } else {
            name = filename;
            extension = "";
        }

        if (mExtensionPattern == null) {
            return wildcardCompare(name, mNamePattern);
        } else {
            return wildcardCompare(name, mNamePattern) &&
                    wildcardCompare(extension, mExtensionPattern);
        }
    }
}


class AsciiInputStream extends FilterInputStream {
    boolean eof = false;

    AsciiInputStream(InputStream in) {
        super(in);
    }

    public int read() throws IOException {
        if (eof)
            return -1;

        int c = in.read();

        if (c < 0) {
            eof = true;
            return 26;
        }
        return c;
    }

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte b[], int offset, int len) throws IOException {
        int count = 0;

        for (int i = 0; i < len; i++) {
            int c = read();
            if (c < 0)
                break;
            b[count++] = (byte) c;
        }

        return count;
    }

}

class AsciiOutputStream extends FilterOutputStream {
    private boolean eof = false;

    AsciiOutputStream(OutputStream os) {
        super(os);
    }

    public void write(int c) throws IOException {
        if (eof)
            return;

        switch (c) {
            case 26:
                eof = true;
                return;
        }

        super.write(c);
    }
}


public class CpmUtil {
    private final String version = "J80 Disk utility version 0.0";
    BufferedReader rd;
    PrintStream ps = System.out;
    private CpmDisk disk = null;
    private DPB dpb = new DPBHD4MB();
    private int user = 0;
    private boolean binary = true;

    CpmUtil() {
        rd = new BufferedReader(new InputStreamReader(System.in));

    }

    static public void main(String argv[]) {
        CpmUtil cpmUtil = new CpmUtil();
        cpmUtil.start();

    }

    String readLine() {
        ps.print("$");
        try {
            return rd.readLine();
        } catch (Exception ex) {
            return null;
        }

    }

    void start() {
        ps.println(version);
        ps.println("DPB is " + dpb);
        ps.println("Transfer mode mode " + (binary ? "Binary" : "Ascii"));
        ps.println();
        for (; ; ) {
            String s = readLine();
            if (s == null)
                break;

            try {
                cmd(new StringTokenizer(s));
            } catch (Exception ex) {
                String msg = ex.getMessage();
                if (msg == null)
                    msg = "";
                if (msg.length() == 0)
                    msg = ex.toString();
                //ps.println(ex);
                ps.println(msg);
                //ex.printStackTrace(ps);
                //System.exit(0);
            }
        }

    }

    public void cmd(StringTokenizer st) throws Exception {
        String s = st.nextToken();

        if (s.equalsIgnoreCase("stat"))
            cmdStat(st);
        else if (s.equalsIgnoreCase("mount"))
            cmdMount(st);
        else if (s.equalsIgnoreCase("dir"))
            cmdDir(st);
        else if (s.equalsIgnoreCase("delete"))
            cmdDelete(st);
        else if (s.equalsIgnoreCase("type"))
            cmdType(st);
        else if (s.equalsIgnoreCase("binary"))
            cmdBinary(st);
        else if (s.equalsIgnoreCase("ascii"))
            cmdAscii(st);
        else if (s.equalsIgnoreCase("get"))
            cmdGet(st);
        else if (s.equalsIgnoreCase("put"))
            cmdPut(st);
        else if (s.equalsIgnoreCase("quit"))
            cmdExit();
        else if (s.equalsIgnoreCase("exit"))
            cmdExit();
        else if (s.equalsIgnoreCase("umount"))
            cmdUmount();
        else if (s.equalsIgnoreCase("format"))
            cmdFormat(st);
        else if (s.equalsIgnoreCase("ibm3270"))
            cmdDpb(new DPB3270());
        else if (s.equalsIgnoreCase("hd4mb"))
            cmdDpb(new DPBHD4MB());
        else if (s.equalsIgnoreCase("yaze"))
            cmdDpb(new DPBYaze());
        else if (s.equalsIgnoreCase("help"))
            cmdHelp();
        else if (s.equalsIgnoreCase("user"))
            cmdUser(st);
        else
            throw new Exception("Invalid command : " + s + "\nPlease try help");
    }

    public void cmdUser(StringTokenizer st) {
        user = Integer.parseInt(st.nextToken());
        ps.println("New user is " + user);
    }

    public void cmdHelp() {
        ps.println(version);
        ps.println();
        ps.println("format file - Format and mount a new disk");
        ps.println("mount file  - Mount with the current DPB the disk image file");
        ps.println("umount      - Dismount currently mounted disk");
        ps.println("ibm3270     - Select IBM32780 DBP");
        ps.println("yaze        - Select standard Yaze disk");
        ps.println("hd4mb       - Select 4 MB DPB");
        ps.println("stat        - Display statistics abount mounted disk");
        ps.println("dir         - Display current directory");
        ps.println("delete file - Delete one file on the mounted disk");
        ps.println("type file   - Display one standard output one CP/M file");
        ps.println("binary      - Select binary mode");
        ps.println("ascii       - Select ASCII mode");
        ps.println("get file    - Get one file from the disk");
        ps.println("put file    - Put one or more file on the disk");
        ps.println("help        - Display this help");
        ps.println("user nn	    - Change current CP/M user");
        ps.println("exit|quit   - Terminate CpmUtil");

    }

    public void cmdDpb(DPB dpb) throws Exception {
        cmdUmount();

        this.dpb = dpb;
        ps.println("DPB is " + dpb);
    }

    public void cmdFormat(StringTokenizer st) throws Exception {
        cmdUmount();
        String name = st.nextToken();

        disk = new CpmDisk(dpb, new ImageDisk(name, dpb.sectorTrack));
        System.out.println("Formatting " + name);
        disk.format();
        cmdMount(name);

        int track = -1;
        int sector = dpb.sectorTrack;

        while (st.hasMoreElements()) {
            String boot = st.nextToken();
            ps.println("boot " + boot);

            byte buffer[] = new byte[128];
            FileInputStream is = new FileInputStream(boot);

            for (; ; ) {
                int count = is.read(buffer);
                if (count <= 0)
                    break;
                if (++sector > dpb.sectorTrack) {
                    sector = 1;
                    if (++track >= dpb.trackOffset)
                        throw new Exception("Not enaugh reserved track");
                }
                //ps.print("Write sector "+track+" "+sector );
                disk.writeSector(track, sector, buffer);
            }

            is.close();
        }

    }

    public void cmdUmount() throws Exception {
        if (disk != null) {
            ps.println("umounting " + disk);
            disk.umount();
        }
        disk = null;

    }

    public void cmdExit() throws Exception {
        cmdUmount();
        System.exit(0);
    }

    public void cmdBinary(StringTokenizer st) {
        ps.println("binary mode");
        binary = true;
    }

    public void cmdAscii(StringTokenizer st) {
        ps.println("ascii mode");
        binary = false;
    }

    public void cmdGet(StringTokenizer st) throws Exception {
        checkDisk();

        while (st.hasMoreElements()) {
            String name = st.nextToken().toUpperCase();
            ps.println("get " + name + " binary is " + binary);
            OutputStream os = new FileOutputStream(name);
            if (binary == false)
                os = new AsciiOutputStream(os);
            disk.getFile(user, name, os);
            os.close();

        }

    }

    public void cmdPut(File file) throws Exception {

        String dest = file.getName().toUpperCase();
        ps.println("put " + file + " as " + dest + " binary is " + binary);
        InputStream is = new FileInputStream(file);
        if (binary == false)
            is = new AsciiInputStream(is);
        disk.putFile(user, dest, is);
        is.close();

    }

    public void cmdPut(StringTokenizer st) throws Exception {
        checkDisk();

        while (st.hasMoreElements()) {
            String name = st.nextToken();
            File file = new File(name);

            if (WildcardFileFilter.isWildCard(name)) {
                String parent = file.getParent();
                if (parent == null)
                    parent = ".";
                name = file.getName();

                file = new File(parent);


                File files[] = file.listFiles(new WildcardFileFilter(name));
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isFile() == true)
                            cmdPut(files[i]);
                    }
                } else
                    cmdPut(file);
            } else
                cmdPut(file);

        }

    }

    public void cmdDelete(StringTokenizer st) throws Exception {
        checkDisk();

        while (st.hasMoreElements()) {
            String name = st.nextToken();
            ps.println("deleting " + name);
            disk.deleteFile(user, name.toUpperCase());
        }
    }

    public void cmdType(StringTokenizer st) throws Exception {
        checkDisk();

        while (st.hasMoreElements()) {
            String name = st.nextToken().toUpperCase();
            ps.println("type " + name);
            disk.getFile(user, name, new AsciiOutputStream(ps));
        }
    }

    public void cmdDir(StringTokenizer st) throws Exception {
        int count = 0;
        int total = 0;

        checkDisk();

        ps.println("Directory for user " + user);

        for (int i = 0; i < disk.getFileCount(); i++) {
            CpmFile file = disk.getFileAt(i);
            if (file.user == user) {
                String s = file.name;
                while (s.length() < 13)
                    s = s + " ";
                total += disk.getFileSize(file) * 128;
                String ss = "" + (disk.getFileSize(file) * 128);
                while (ss.length() < 6)
                    ss = " " + ss;

                ps.print(s + ss);
                if ((++count) % 4 == 0)
                    ps.println();
                else
                    ps.print(" ");
            }

        }
        ps.println();
        ps.println(total + " Bytes in " + count + " files");
    }

    public void checkDisk() throws Exception {
        if (disk == null)
            throw new Exception("No disk mounted\nPlease use : mount filename\n");
    }

    public void cmdStat(StringTokenizer st) throws Exception {
        checkDisk();
        disk.stat();
    }

    public void cmdMount(StringTokenizer st) throws Exception {
        cmdMount(st.nextToken());
    }

    public void cmdMount(String name) throws Exception {


        if (disk != null) {
            try {
                disk.umount();
            } catch (Exception ex) {
            }
        }

        System.out.println("Mounting " + name);

        Disk d;

        if (dpb instanceof DPBYaze) {
            ((DPBYaze) dpb).setBuffer(name);
            d = new YazeDisk(name, dpb);
        } else
            d = new ImageDisk(name, dpb.sectorTrack);

        disk = new CpmDisk(dpb, d);

        try {
            disk.mount();
        } catch (Exception ex) {
            try {
                disk.umount();
                disk = null;
            } catch (Exception ex1) {
            }
            throw ex;
        }
    }

}
package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;

import com.grelobites.dandanator.util.emulator.zxspectrum.Z80;
import com.grelobites.dandanator.util.emulator.zxspectrum.Z80VirtualMachine;
import com.grelobites.dandanator.util.emulator.zxspectrum.SnapshotLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ZXSnapshotLoader implements SnapshotLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZXSnapshotLoader.class);
    /**
     * Memory map
     */
    public static final int RAM_MEMORY_START = 0x4000;
    public static final int SCREEN_MEMORY_START = 0x4000;
    public static final int SCREEN_MEMORY_END = 0x57FF;
    public static final int SCREEN_ATTRIBUTE_START = 0x5800;
    public static final int SCREEN_ATTRIBUTE_END = 0x5AFF;

    protected Z80VirtualMachine cpu;


    public void onCpuReset(Z80VirtualMachine cpu) throws Exception {
    }

    public void unbind(Z80VirtualMachine cpu) {
    }


    public void bind(Z80VirtualMachine cpu) throws Exception {
        this.cpu = cpu;
    }

    public void load(Z80VirtualMachine cpu, String name) {
        try {
            File file = new File(name);
            int snapshotLength = (int) file.length();

            LOGGER.debug("Snapshot " + name + " length " + snapshotLength);
            FileInputStream is = new FileInputStream(file);

            // Crude check but it'll work (SNA is a fixed size)
            if ((snapshotLength == 49179)) {
                loadSNA(name, is);
            } else {
                loadZ80(name, is, snapshotLength);
            }

            is.close();
        } catch (Exception e) {
            LOGGER.error("Loading snapshot", e);
            throw new RuntimeException("Loading snapshot", e);
        }
    }

    public int readBytes(InputStream is, int mem[], int len) throws Exception {
        for (int i = 0; i < len; i++) {
            int c = is.read();
            if (c < 0)
                return i;

            mem[i] = c & 0xff;
        }

        return len;
    }

    public void loadSNA(String name, InputStream is) throws Exception {
        LOGGER.debug("Before loading SNA. CPU Status: " + cpu.dumpStatus());
        LOGGER.debug("Load SNA " + name);
        int header[] = new int[27];

        readBytes(is, header, 27);
        cpu.load(is, RAM_MEMORY_START, 49152);

        cpu.I(header[0]);

        cpu.HL(header[1] | (header[2] << 8));
        cpu.DE(header[3] | (header[4] << 8));
        cpu.BC(header[5] | (header[6] << 8));
        cpu.AF(header[7] | (header[8] << 8));

        cpu.exx();
        cpu.ex_AF_AF();

        cpu.HL(header[9] | (header[10] << 8));
        cpu.DE(header[11] | (header[12] << 8));
        cpu.BC(header[13] | (header[14] << 8));

        cpu.IY(header[15] | (header[16] << 8));
        cpu.IX(header[17] | (header[18] << 8));

        cpu.IFF1 = (header[19] & 0x04) != 0;

        cpu.R(header[20]);

        cpu.AF(header[21] | (header[22] << 8));
        cpu.SP(header[23] | (header[24] << 8));

        switch (header[25]) {
            case 0:
                cpu.IM(Z80.IM0);
                break;
            case 1:
                cpu.IM(Z80.IM1);
                break;
            default:
                cpu.IM(Z80.IM2);
                break;
        }

        cpu.outb(254, header[26], 0); // border

		/* Emulate RETN to start */
        LOGGER.debug("After loading SNA. CPU Status: " + cpu.dumpStatus());
        cpu.IFF0 = cpu.IFF1;
        cpu.poppc();
        LOGGER.debug("After popping PC. CPU Status: " + cpu.dumpStatus());
    }


    public void loadZ80(String name, InputStream is, int bytesLeft) throws Exception {
        LOGGER.debug("LoadZ80 " + name);

        int header[] = new int[30];
        boolean compressed = false;

        bytesLeft -= readBytes(is, header, 30);

        cpu.A(header[0]);
        cpu.F(header[1]);

        cpu.C(header[2]);
        cpu.B(header[3]);
        cpu.L(header[4]);
        cpu.H(header[5]);

        cpu.PC(header[6] | (header[7] << 8));
        cpu.SP(header[8] | (header[9] << 8));

        cpu.I(header[10]);
        cpu.R(header[11]);

        int tbyte = header[12];
        if (tbyte == 255) {
            tbyte = 1;
        }

        cpu.outb(254, ((tbyte >> 1) & 0x07), 0); // border

        if ((tbyte & 0x01) != 0) {
            cpu.R(cpu.R | 0x80);
        }
        compressed = ((tbyte & 0x20) != 0);

        cpu.E(header[13]);
        cpu.D(header[14]);

        cpu.ex_AF_AF();
        cpu.exx();

        cpu.C(header[15]);
        cpu.B(header[16]);
        cpu.E(header[17]);
        cpu.D(header[18]);
        cpu.L(header[19]);
        cpu.H(header[20]);

        cpu.A(header[21]);
        cpu.F(header[22]);

        cpu.ex_AF_AF();
        cpu.exx();

        cpu.IY(header[23] | (header[24] << 8));
        cpu.IX(header[25] | (header[26] << 8));

        cpu.IFF0 = header[27] != 0;
        cpu.IFF1 = header[28] != 0;

        switch (header[29] & 0x03) {
            case 0:
                cpu.IM(cpu.IM0);
                break;
            case 1:
                cpu.IM(cpu.IM1);
                break;
            default:
                cpu.IM(cpu.IM2);
                break;
        }

        if (cpu.PC == 0) {
            loadZ80_extended(is, bytesLeft);

            return;
        }
        /* Old format Z80 snapshot */

        if (compressed) {
            int data[] = new int[bytesLeft];
            int addr = RAM_MEMORY_START;

            int size = readBytes(is, data, bytesLeft);
            System.out.println("Byte " + size + " at " + Integer.toHexString(addr) + "H");
            int i = 0;

            while ((addr < 65536) && (i < size)) {
                tbyte = data[i++];
                if (tbyte != 0xed) {
                    cpu.pokeb(addr, tbyte);
                    addr++;
                } else {
                    tbyte = data[i++];
                    if (tbyte != 0xed) {
                        cpu.pokeb(addr, 0xed);
                        i--;
                        addr++;
                    } else {
                        int count;
                        count = data[i++];
                        tbyte = data[i++];
                        while ((count--) != 0) {
                            cpu.pokeb(addr, tbyte);
                            addr++;
                        }
                    }
                }
            }
        } else {
            cpu.load(is, RAM_MEMORY_START, 49152);
        }

    }

    private void loadZ80_extended(InputStream is, int bytesLeft) throws Exception {
        int header[] = new int[2];
        bytesLeft -= readBytes(is, header, header.length);

        System.out.println("Loadz80_extended");
        int type = header[0] | (header[1] << 8);

        switch (type) {
            case 23: /* V2.01 */
                loadZ80_v201(is, bytesLeft);
                break;
            case 54: /* V3.00 */
                loadZ80_v300(is, bytesLeft);
                break;
            case 58: /* V3.01 */
                loadZ80_v301(is, bytesLeft);
                break;
            default:
                throw new Exception("Z80 (extended): unsupported type " + type);
        }
    }

    private void loadZ80_v201(InputStream is, int bytesLeft) throws Exception {
        int header[] = new int[23];
        bytesLeft -= readBytes(is, header, header.length);

        cpu.PC(header[0] | (header[1] << 8));

		/* 0 - 48K
		 * 1 - 48K + IF1
		 * 2 - SamRam
		 * 3 - 128K
		 * 4 - 128K + IF1
		 */
        int type = header[2];

        int data[] = new int[bytesLeft];
        readBytes(is, data, bytesLeft);

        for (int offset = 0, j = 0; j < 3; j++) {
            offset = loadZ80_page(data, offset, type);
        }
    }

    private void loadZ80_v300(InputStream is, int bytesLeft) throws Exception {
        int header[] = new int[54];
        bytesLeft -= readBytes(is, header, header.length);

        cpu.PC(header[0] | (header[1] << 8));

		/* 0 - 48K
		 * 1 - 48K + IF1
		 * 2 - 48K + MGT
		 * 3 - SamRam
		 * 4 - 128K
		 * 5 - 128K + IF1
		 * 6 - 128K + MGT
		 */
        int type = header[2];

        if (type > 6) {
            throw new Exception("Z80 (v300): unsupported type " + type);
        }

        int data[] = new int[bytesLeft];
        readBytes(is, data, bytesLeft);

        for (int offset = 0, j = 0; j < 3; j++) {
            offset = loadZ80_page(data, offset, type);
        }
    }

    private void loadZ80_v301(InputStream is, int bytesLeft) throws Exception {
        int header[] = new int[58];
        bytesLeft -= readBytes(is, header, header.length);

        cpu.PC(header[0] | (header[1] << 8));

		/* 0 - 48K
		 * 1 - 48K + IF1
		 * 2 - 48K + MGT
		 * 3 - SamRam
		 * 4 - 128K
		 * 5 - 128K + IF1
		 * 6 - 128K + MGT
		 * 7 - +3
		 */
        int type = header[2];

        if (type > 7) {
            throw new Exception("Z80 (v301): unsupported type " + type);
        }

        int data[] = new int[bytesLeft];
        readBytes(is, data, bytesLeft);

        for (int offset = 0, j = 0; j < 3; j++) {
            offset = loadZ80_page(data, offset, type);
        }
    }

    private int page2address(int type, int page) throws Exception {
        int addr = -1;

        if (type == 0) // Spectrum 48k
        {
            switch (page) {
                case 4:
                    addr = 0x8000;
                    break;
                case 5:
                    addr = 0xc000;
                    break;
                case 8:
                    addr = 0x4000;
                    break;
            }

        } else if (type == 4) // spectrum 128k
        {
            if (page >= 3 && page <= 10) {
                addr = 0xc000;
                cpu.outb(0xfd, page - 3, 0x7f);
            }
        }

        if (addr == -1) {
            throw new Exception("z80 page " + page + " type " + type + " unsupported");
        }

        System.out.println("z80 page " + page + " type " + type + " at " + Integer.toHexString(addr));

        return addr;

    }

    private int loadZ80_page(int data[], int i, int type) throws Exception {
        int blocklen;
        int page;

        blocklen = data[i++];
        blocklen |= (data[i++]) << 8;
        page = data[i++];

        int addr = page2address(type, page);

        int k = 0;
        while (k < blocklen) {
            int tbyte = data[i++];
            k++;
            if (tbyte != 0xed) {
                cpu.pokeb(addr, ~tbyte);
                cpu.pokeb(addr, tbyte);
                addr++;
            } else {
                tbyte = data[i++];
                k++;
                if (tbyte != 0xed) {
                    cpu.pokeb(addr, 0);
                    cpu.pokeb(addr, 0xed);
                    addr++;
                    i--;
                    k--;
                } else {
                    int count;
                    count = data[i++];
                    k++;
                    tbyte = data[i++];
                    k++;
                    while (count-- > 0) {
                        cpu.pokeb(addr, ~tbyte);
                        cpu.pokeb(addr, tbyte);
                        addr++;
                    }
                }
            }
        }

        if ((addr & 16383) != 0) {
            throw new Exception("Z80 (page): overrun");
        }

        return i;
    }
}

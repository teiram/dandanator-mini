package com.grelobites.dandanator.util.emulator.zxspectrum.cpm;


import com.grelobites.dandanator.util.emulator.zxspectrum.Disk;

import java.util.Vector;

public class CpmDisk {
    private Disk disk;
    private DPB dpb;
    private boolean directoryUsed[];
    private boolean blockUsed[];
    private byte fcbBuffer[] = new byte[128];

    private byte block[];
    private Vector files = new Vector();

    public CpmDisk(DPB dpb, Disk disk) {
        this.dpb = dpb;
        this.disk = disk;
        directoryUsed = new boolean[dpb.drm + 1];
        blockUsed = new boolean[dpb.dsm + 1];
        block = new byte[getBlockSize()];
    }

    public void format() throws Exception {
        for (int i = 0; i <= dpb.trackOffset; i++)
            disk.format(i);

        for (int i = 0; i < block.length; i++) {
            block[i] = (byte) 0xe5;
        }
        for (int i = 0; i < getNumDirBlock() * 2; i++) {
            writeBlock(i, block);
        }
    }

    public CpmFile getFileAt(int index) {
        return (CpmFile) files.elementAt(index);
    }

    public int getFileCount() {
        return files.size();
    }

    private CpmFile searchFileException(int user, String name) throws Exception {
        CpmFile file = searchFile(user, name);

        if (file == null)
            throw new Exception("File not found " + user + ":" + name);

        return file;
    }

    private CpmFile searchFile(int user, String name) {
        for (int i = 0; i < getFileCount(); i++) {
            CpmFile file = getFileAt(i);

            if (file.user == user && file.name.equals(name))
                return file;
        }

        return null;
    }

    private int getFCBBlockLength() {
        return dpb.dsm < 0x100 ? 1 : 2;
    }

    private int getFCBBlockCount() {
        return 16 / getFCBBlockLength();
    }

    private int getFCBBlock(FCB fcb, int block) {
        if (getFCBBlockLength() == 1)
            return fcb.getBlockByte(block);
        else
            return fcb.getBlockWord(block);
    }

    private void putFCBBlock(FCB fcb, int n, int block) {
        if (getFCBBlockLength() == 1)
            fcb.setBlockByte(n, block);
        else {
            fcb.setBlockWord(n, block);
        }

    }

    private void writeFCB(int dir, FCB fcb) throws Exception {
        int offset = dir * 32;
        int sector = offset / 128;
        offset = offset % 128;

        readSector(sector, fcbBuffer);

        System.arraycopy(fcb.getBytes(), 0, fcbBuffer, offset + 0, 32);

        writeSector(sector, fcbBuffer);

    }

    private void readFCB(int dir, FCB fcb) throws Exception {
        int offset = dir * 32;
        int sector = offset / 128;
        offset = offset % 128;

        readSector(sector, fcbBuffer);

        fcb.setBuffer(fcbBuffer, offset);
    }

    public void writeSector(int track, int sector, byte buffer[]) throws Exception {

        disk.write(track, sector, buffer);

    }

    public void writeSector(int secno, byte buffer[]) throws Exception {
        int track, sector;

        track = secno / dpb.sectorTrack + dpb.trackOffset;
        sector = secno % dpb.sectorTrack + 1;
        sector = dpb.translateSector(sector);

        writeSector(track, sector, buffer);
    }

    public void readSector(int track, int sector, byte buffer[]) throws Exception {
        disk.read(track, sector, buffer);
    }

    public void readSector(int secno, byte buffer[]) throws Exception {
        int track, sector;

        track = secno / dpb.sectorTrack + dpb.trackOffset;
        sector = secno % dpb.sectorTrack + 1;

        sector = dpb.translateSector(sector);

        readSector(track, sector, buffer);
    }

    public int getNumDirBlock() {
        int count = 0;

        for (int i = 0; i < 8; i++) {
            int mask = 1 << i;
            if ((dpb.alloc0 & mask) != 0)
                count++;
            if ((dpb.alloc1 & mask) != 0)
                count++;
        }

        return count;
    }

    public void mount() throws Exception {
        FCB fcb = new FCB();

        disk.mount();

        // Mark all block as not used
        for (int i = 0; i < blockUsed.length; i++)
            blockUsed[i] = false;

        // Mark directory block as used
        for (int i = 0; i < 8; i++) {
            if ((dpb.alloc0 & (1 << (7 - i))) != 0)
                blockUsed[i] = true;
            if ((dpb.alloc1 & (1 << (7 - i))) != 0)
                blockUsed[8 + i] = true;
        }

        files = new Vector();

        for (int i = 0; i <= dpb.drm; i++) {
            readFCB(i, fcb);

            if (fcb.getDeleted()) {
                directoryUsed[i] = false;
                continue;
            }

            int user = fcb.getUser();
            int ext = fcb.getEX();
            int record = fcb.getRC();

            directoryUsed[i] = true;

            // Skip not valid user
            if (user > 31) {
                continue;
            }

            // Mark directory entry used
            directoryUsed[i] = true;


            String fileName = fcb.getFileName();


            CpmFile file = searchFile(user, fileName);

            if (file == null) {
                file = new CpmFile(dpb, user, fileName);
                files.add(file);
            }

            file.addFCB(i);

            // Read block number from FCB
            for (int j = 0; j < getFCBBlockCount(); j++) {

                int b = getFCBBlock(fcb, j);
                if (b == 0)
                    break;
                blockUsed[b] = true;
                file.addBlock(b);
            }
        }


    }

    public int getNumBlockUsed() {
        int block = 0;

        for (int i = 0; i < blockUsed.length; i++)
            if (blockUsed[i] == true)
                block++;

        return block;
    }

    int getBlockSize() {

        return (dpb.blm + 1) * 128;
    }

    public int getNumDirectoryUsed() {
        int dir = 0;

        for (int i = 0; i < directoryUsed.length; i++)
            if (directoryUsed[i] == true)
                dir++;

        return dir;

    }

    public void stat(String name, int value, String unit) {
        while (name.length() < 32)
            name = " " + name;

        String s = "" + value;
        while (s.length() < 10)
            s = " " + s;

        System.out.println(name + " = " + s + " " + unit);

    }

    private void stat(String name, int value) {
        stat(name, value, "");
    }

    public void stat() {
        int usedBlock;
        dpb.dump();
        stat("Reserved track", dpb.trackOffset);
        stat("Total directory", dpb.drm + 1);
        stat("Directory used", getNumDirectoryUsed());
        stat("Directory free", dpb.drm + 1 - getNumDirectoryUsed());

        usedBlock = getNumBlockUsed();

        stat("Block size", getBlockSize(), "Bytes");
        stat("Space configured ", dpb.dsm + 1, "Block");
        stat("Space used", usedBlock, "Block");
        stat("Space available", dpb.dsm + 1 - usedBlock, "Block");


        stat("Space configured", ((dpb.dsm + 1) * getBlockSize()) / 1024, "KB");
        stat("Space used", ((usedBlock * getBlockSize())) / 1024, "KB");
        stat("Space free", (((dpb.dsm + 1 - usedBlock) * getBlockSize())) / 1024, "KB");

    }

    public void readBlock(int block, byte buffer[]) throws Exception {
        int sector = (dpb.blm + 1) * block;

        //System.out.println("Block # "+block + " sector "+sector);
        for (int i = 0; i <= dpb.blm; i++) {
            readSector(sector++, fcbBuffer);
            for (int j = 0; j < 128; j++)
                buffer[i * 128 + j] = fcbBuffer[j];
        }
    }

    public void writeBlock(int block, byte buffer[]) throws Exception {
        int sector = (dpb.blm + 1) * block;

        for (int i = 0; i <= dpb.blm; i++) {
            for (int j = 0; j < 128; j++)
                fcbBuffer[j] = buffer[i * 128 + j];

            writeSector(sector++, fcbBuffer);
        }
    }

    private int allocateBlock() throws Exception {
        for (int i = 0; i < block.length; i++)
            if (blockUsed[i] == false) {
                //System.out.println("Allocate block # "+i);
                blockUsed[i] = true;
                return i;
            }

        throw new Exception("Out of disk space");

    }

    private int allocateFCB() throws Exception {
        for (int i = 0; i < directoryUsed.length; i++)
            if (directoryUsed[i] == false) {
                //System.out.println("Allocate fcb # "+i);
                directoryUsed[i] = true;
                return i;
            }

        throw new Exception("No free FCB entry");

    }

    public int getRecordForFCB() {
        return (dpb.exm + 1) * 128;
    }

    public void putFile(int user, String name, java.io.InputStream is) throws Exception {
        FCB fcb = new FCB();
        int i, j;
        int entry;
        int blockCount = 0;
        int record = 0;

        CpmFile file = searchFile(user, name);
        if (file != null)
            throw new Exception(name + " Already exist");

        file = new CpmFile(dpb, user, name);
        fcb.clear();
        fcb.setUser(user);
        fcb.setFileName(name);
        fcb.setEX(0);
        fcb.setRC(0);

        entry = allocateFCB();

        files.add(file);

        // Write empty directory
        writeFCB(entry, fcb);

        file.addFCB(entry);


        for (; ; ) {
            int byteCount = is.read(block, 0, getBlockSize());
            if (byteCount <= 0)
                break;
            //System.out.println("Writing "+byteCount+" bytes");
            int recordCount = (byteCount + 127) / 128;

            while (recordCount > 0) {
                if (blockCount >= getFCBBlockCount()) {
                    entry = allocateFCB();
                    fcb.clearBlocks();
                    fcb.setRC(0);
                    fcb.setEX(fcb.getEX() + 1);
                    //fcb[12] = (byte)(fcb[12]+getRecordForFCB()/128);
                    writeFCB(entry, fcb);
                    file.addFCB(entry);
                    blockCount = 0;
                }

                int blockNo = allocateBlock();
                //System.out.print("block "+blockNo+" at "+blockCount);
                putFCBBlock(fcb, blockCount++, blockNo);
                if (recordCount > dpb.blm + 1)
                    record = dpb.blm + 1;
                else
                    record = recordCount;

                recordCount -= record;

                record += fcb.getRC();

                record &= 0xff;

                if (record > 128) {
                    fcb.setEX(fcb.getEX() + 1);
                    if (record > 128)
                        record -= 128;
                }
                fcb.setRC(record);
                //System.out.print(" ex "+fcb[12]+" rc "+(fcb[15] & 0xff)+" ");
                writeFCB(entry, fcb);
                file.addBlock(blockNo);
                writeBlock(blockNo, block);
            }

        }


    }

    public int getFileSize(CpmFile file) throws Exception {
        int size = 0;
        FCB fcb = new FCB();

        for (int i = 0; i < file.getFCBCount(); i++) {
            readFCB(file.getFCBAt(i), fcb);
            int ex = (fcb.getEX() & 0x1f) & ~dpb.exm;
            int record = fcb.getRC() & 0xff;
            size = ex * 128 + ((dpb.exm & fcb.getEX()) + 1) * 128 - (128 - record);
            //System.out.println("name "+file.name+" ext "+ex+" record "+record);
        }

        return size;
    }

    public void getFile(int user, String name, java.io.OutputStream os) throws Exception {
        CpmFile file = searchFileException(user, name);
        int length = getFileSize(file) * 128;

        //System.out.println("name "+name+" size "+file.size+" block "+file.getBlockCount()+" r "+file.record);
        for (int i = 0; i < file.getBlockCount(); i++) {
            readBlock(file.getBlockAt(i), block);
            int count = getBlockSize();
            if (length < count)
                count = length;

            os.write(block, 0, count);

            length -= count;
        }

    }

    public void deleteFile(int user, String name) throws Exception {
        CpmFile file = searchFileException(user, name);

        for (int i = 0; i < file.getBlockCount(); i++) {
            //System.out.println("Free block "+file.getBlockAt(i));
            blockUsed[file.getBlockAt(i)] = false;
        }

        FCB fcb = new FCB();

        for (int i = 0; i < file.getFCBCount(); i++) {
            int entry = file.getFCBAt(i);
            //System.out.println("free FCB "+entry);
            readFCB(entry, fcb);
            fcb.setDeleted();
            writeFCB(entry, fcb);
            directoryUsed[entry] = false;
        }

        files.remove(file);
    }

    public void umount() throws Exception {
        disk.umount();
    }

    public String toString() {
        return "CP/M Disk " + disk;
    }
}

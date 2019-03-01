package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.zx7.Zx7InputStream;
import com.grelobites.romgenerator.util.player.AudioDataPlayerSupport;
import com.grelobites.romgenerator.util.tap.TapOutputStream;
import com.grelobites.romgenerator.util.tap.TapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class RomSetUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(RomSetUtil.class);
    private static final String LOADER_NAME = "DivIDELoader";
    private static final String PAUSE_FILENAME = "pause.wav";
    private static final String PAUSE_RESOURCE = "/player/" + PAUSE_FILENAME;
    private static final String PLAYLIST_NAME = "loader.m3u";
    private static final int LOAD_ADDRESS = 0x6f00;
    private static final int BLOCK_SIZE = 0x8000;
    private static final int BLOCK_COUNT = 16;
    private static final String BLOCK_NAME_PREFIX = "block";
    private static final String MULTILOADER_SIGNATURE = "MLD";

    public static void exportToDivideAsTap(InputStream romsetStream, OutputStream out) throws IOException {
        TapOutputStream tos = TapUtil.getLoaderTap(new ByteArrayInputStream(DandanatorMiniConstants
                .getDivIdeLoader()), out, 0);

        byte[] buffer = new byte[BLOCK_SIZE + 3];
        for (int i = 0; i <  BLOCK_COUNT; i++) {
            System.arraycopy(Util.fromInputStream(romsetStream, BLOCK_SIZE), 0, buffer, 0, BLOCK_SIZE);
            buffer[BLOCK_SIZE] = Integer.valueOf(i + 1).byteValue();
            Util.writeAsLittleEndian(buffer, BLOCK_SIZE + 1, Util.getBlockCrc16(buffer, BLOCK_SIZE + 1));


            tos.addCodeStream(
                    String.format("%s%02d", BLOCK_NAME_PREFIX, i),
                    LOAD_ADDRESS, false, buffer);
        }
        out.flush();
    }

    public static void upgradeDivideTapLoader(Path divideFile) throws IOException {
        Path backup = Files.move(divideFile, divideFile
                .resolveSibling(divideFile.getFileName() + ".back"));
        try (FileInputStream oldTap = new FileInputStream(backup.toFile());
                FileOutputStream newTap = new FileOutputStream(divideFile.toFile())) {
            TapUtil.upgradeTapLoader(oldTap, newTap);
        } catch (Exception e) {
            Files.move(backup, divideFile, StandardCopyOption.REPLACE_EXISTING);
            throw e;
        }
    }

    public static void exportToZippedWavFiles(InputStream romsetStream, OutputStream out) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(out);
        ByteArrayOutputStream playList = new ByteArrayOutputStream();
        PrintWriter playListWriter = new PrintWriter(playList, true);

        AudioDataPlayerSupport support = new AudioDataPlayerSupport();
        int index = 0;
        String entryName = String.format("%s%02d.wav", BLOCK_NAME_PREFIX, index++);
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(Files.readAllBytes(support.getBootstrapAudioFile().toPath()));
        zos.closeEntry();
        playListWriter.println(entryName);
        playListWriter.println(PAUSE_FILENAME);
        int blockSize = PlayerConfiguration.getInstance().getBlockSize();
        byte[] buffer = new byte[blockSize];


        for (int block = 0; block < BLOCK_COUNT; block++) {
            LOGGER.debug("Adding block " + block + " of size " + blockSize);
            System.arraycopy(Util.fromInputStream(romsetStream, blockSize), 0, buffer, 0, blockSize);
            entryName = String.format("%s%02d.wav", BLOCK_NAME_PREFIX, index++);
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write(Files.readAllBytes(support.getBlockAudioFile(block, buffer).toPath()));
            zos.closeEntry();
            playListWriter.println(entryName);
            if (block < BLOCK_COUNT - 1) {
                playListWriter.println(PAUSE_FILENAME);
            }
        }

        zos.putNextEntry(new ZipEntry(PLAYLIST_NAME));
        zos.write(playList.toByteArray());
        zos.closeEntry();

        zos.putNextEntry(new ZipEntry(PAUSE_FILENAME));
        zos.write(Util.fromInputStream(RomSetUtil.class.getResourceAsStream(PAUSE_RESOURCE)));
        zos.closeEntry();

        zos.flush();
        zos.close();
    }

    private static Optional<InputStream> getRomScreenResource(ByteBuffer buffer, int slot) {
        buffer.position(Constants.SLOT_SIZE * slot);
        byte[] magic = new byte[3];
        buffer.get(magic);
        if (MULTILOADER_SIGNATURE.equals(new String(magic))) {
            int version = Byte.toUnsignedInt(buffer.get());
            int offset = Short.toUnsignedInt(buffer.getShort());
            int size = Short.toUnsignedInt(buffer.getShort());
            LOGGER.debug("Detected Multiload ROMSet with version " + version);
            LOGGER.debug("Compressed screen at offset " + offset + ", size " + size);
            return Optional.of(new Zx7InputStream(new ByteArrayInputStream(buffer.array(),
                    offset + Constants.SLOT_SIZE * slot, size)));
        } else {
            return Optional.empty();
        }
    }

    public static Optional<InputStream> getRomScreenResource(File file) {
        if (file.isFile() && file.length() == Constants.SLOT_SIZE * 32) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(file.toPath()))
                    .order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 31; i > 0; i--) {
                    Optional<InputStream> screen = getRomScreenResource(buffer, i);
                    if (screen.isPresent()) {
                        LOGGER.debug("Found Multiload in slot " + i);
                        return screen;
                    }
                }
            } catch (IOException ioe) {
                LOGGER.error("Reading ROM file " + file, ioe);
            }
        }
        //Fallback to MD5 method
        return getKnownRomScreenResource(file);
    }

    private static Optional<InputStream> getKnownRomScreenResource(File file) {
        String md5 = Util.getMD5(file);
        for (String[] candidate : Constants.KNOWN_ROMS) {
            if (candidate[0].equals(md5)) {
                return Optional.of(Constants.getScreenFromResource(candidate[1]));
            }
        }
        return Optional.empty();
    }
}

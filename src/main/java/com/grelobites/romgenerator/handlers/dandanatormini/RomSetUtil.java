package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.player.AudioDataPlayerSupport;
import com.grelobites.romgenerator.util.player.TapOutputStream;
import com.grelobites.romgenerator.util.player.TapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
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
}

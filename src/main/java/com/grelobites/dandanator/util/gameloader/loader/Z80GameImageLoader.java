package com.grelobites.dandanator.util.gameloader.loader;

import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.util.Util;
import com.grelobites.dandanator.util.gameloader.GameImageLoader;
import com.grelobites.dandanator.util.SNAHeader;
import com.grelobites.dandanator.util.Z80CompressedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class Z80GameImageLoader implements GameImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z80GameImageLoader.class);

    private static final int GAME_IMAGE_SIZE = 0xC000;
    private static final int PAGE_SIZE = 0x4000;

    private static byte[] getGameImageV1(InputStream is, boolean compressed) throws IOException {
        LOGGER.debug("Loading Z80 version 1 image, compressed " + compressed);
        byte[] gameData = new byte[GAME_IMAGE_SIZE];
        InputStream z80is = compressed ? new Z80CompressedInputStream(is) : is;
        int len = z80is.read(gameData);
        LOGGER.debug("Read " + len + " bytes for game");
        return gameData;
    }

    private static void getCompressedChunk(InputStream is, byte[] gameData, int offset) throws IOException {
        Z80CompressedInputStream z80is = new Z80CompressedInputStream(is);
        int len = z80is.read(gameData, offset, PAGE_SIZE);
        LOGGER.debug("Read " + len + " bytes for offset " + offset);
    }

    private static byte[] getGameImageV23(InputStream is) throws IOException {
        byte[] gameData = new byte[GAME_IMAGE_SIZE];
        int pagesRead = 0;
        boolean eof = false;
        while (pagesRead < 3 && !eof) {
            int compressedBlockLen = Util.asLittleEndian(is);
            int pageNumber = is.read();
            LOGGER.debug("Reading page number " + pageNumber +
                    " of compressed size " + compressedBlockLen +
                    " from stream with " + is.available() +
                    " available bytes");
            switch (pageNumber) {
                case 4:
                    getCompressedChunk(is, gameData, PAGE_SIZE);
                    pagesRead++;
                    break;
                case 5:
                    getCompressedChunk(is, gameData, PAGE_SIZE << 1);
                    pagesRead++;
                    break;
                case 8:
                    getCompressedChunk(is, gameData, 0);
                    pagesRead++;
                    break;
                case -1:
                    LOGGER.warn("Found EOF while reading pages");
                    eof = true;
                default:
                    long skipped = is.skip(compressedBlockLen);
                    LOGGER.info("Skipped " + skipped + " bytes");
            }
        }
        return gameData;
    }

    private static byte[] getGameImage(InputStream is, boolean compressed, boolean version1) throws IOException {
        return version1 ? getGameImageV1(is, compressed) : getGameImageV23(is);
    }

    @Override
    public byte[] load(InputStream is) throws IOException {
        SNAHeader header = new SNAHeader();
        header.setWordSwapped(SNAHeader.REG_AF, (byte) is.read(), (byte) is.read());
        header.setWord(SNAHeader.REG_BC, (byte) is.read(), (byte) is.read());
        header.setWord(SNAHeader.REG_HL, (byte) is.read(), (byte) is.read());
        int pc = Util.asLittleEndian(is);
        int sp = Util.asLittleEndian(is) - 2;
        header.setWord(SNAHeader.REG_SP, (byte)(sp & 0xff), (byte) ((sp >> 8) & 0xff));
        header.setByte(SNAHeader.REG_I, (byte) is.read());
        LOGGER.debug(String.format("PC: %04x, SP: %04x", pc, sp));
        byte r = (byte) (is.read() & 0xEF);
        byte info = (byte) is.read();
        LOGGER.debug("Info byte: " + info);
        if (info == (byte) 0xff) info = 1; //Compatibility issue
        header.setByte(SNAHeader.REG_R, (byte) (r | ((info & 0x01) << 7)));
        header.setByte(SNAHeader.BORDER_COLOR, (byte) ((info & 0x0E) >> 1));

        boolean compressed = (info & 0x20) != 0;

        header.setWord(SNAHeader.REG_DE, (byte) is.read(), (byte) is.read());
        header.setWord(SNAHeader.REG_BC_alt, (byte) is.read(), (byte) is.read());
        header.setWord(SNAHeader.REG_DE_alt, (byte) is.read(), (byte) is.read());
        header.setWord(SNAHeader.REG_HL_alt, (byte) is.read(), (byte) is.read());
        header.setWordSwapped(SNAHeader.REG_AF_alt, (byte) is.read(), (byte) is.read());
        header.setWord(SNAHeader.REG_IX, (byte) is.read(), (byte) is.read());
        header.setWord(SNAHeader.REG_IY, (byte) is.read(), (byte) is.read());

        header.setByte(SNAHeader.INTERRUPT_ENABLE, (byte) (is.read() == 0 ? 0x0 : 0xff));
        is.read(); //Skip IFF2 byte

        header.setByte(SNAHeader.INTERRUPT_MODE, (byte) (is.read() & 0x3));

        boolean version1 = (pc != 0);

        if (!version1) {
            //Version 2 or 3
            int headerLength = Util.asLittleEndian(is);
            pc = Util.asLittleEndian(is);
            is.skip(headerLength - 2);
        }
        byte [] data = getGameImage(is, compressed, version1);
        int pcLocation = sp - 0x4000; //Offset of PC in game image (Starts at 0x4000)
        data[pcLocation] = (byte) (pc & 0xFF);
        data[pcLocation + 1] = (byte) ((pc >> 8) & 0xFF);

        if (!header.validate()) {
            throw new IllegalArgumentException("Header doesn't pass validations");
        }
        byte[] gameData = new byte[GameImageLoader.IMAGE_SIZE];
        System.arraycopy(header.asByteArray(), 0, gameData, 0, Constants.SNA_HEADER_SIZE);
        System.arraycopy(data, 0, gameData, Constants.SNA_HEADER_SIZE, GAME_IMAGE_SIZE);

        return gameData;
    }

}

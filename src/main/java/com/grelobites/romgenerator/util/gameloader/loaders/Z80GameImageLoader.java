package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.Z80CompressedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Z80GameImageLoader implements GameImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z80GameImageLoader.class);

    private static final int GAME_IMAGE_SIZE = 0xC000;
    private static final int PAGE_SIZE = 0x4000;

    private static List<byte[]> getGameImageV1(InputStream is, boolean compressed) throws IOException {
        LOGGER.debug("Loading Z80 version 1 image, compressed " + compressed);
        InputStream z80is = compressed ? new Z80CompressedInputStream(is) : is;
        List<byte[]> gameSlots = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            gameSlots.add(Util.fromInputStream(z80is, Constants.SLOT_SIZE));
        }
        return gameSlots;
    }

    private static byte[] getCompressedChunk(InputStream is) throws IOException {
        Z80CompressedInputStream z80is = new Z80CompressedInputStream(is);
        return Util.fromInputStream(z80is, Constants.SLOT_SIZE);
    }

    private static List<byte[]> getGameImageV23(InputStream is) throws IOException {
        List<byte[]> gameSlots = new ArrayList<>(3);
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
                    gameSlots.set(1, getCompressedChunk(is));
                    pagesRead++;
                    break;
                case 5:
                    gameSlots.set(2, getCompressedChunk(is));
                    pagesRead++;
                    break;
                case 8:
                    gameSlots.set(0, getCompressedChunk(is));
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
        return gameSlots;
    }

    private static List<byte[]> getGameImage(InputStream is, boolean compressed, boolean version1) throws IOException {
        return version1 ? getGameImageV1(is, compressed) : getGameImageV23(is);
    }

    @Override
    public Game load(InputStream is) throws IOException {
        SNAHeader header = new SNAHeader(Constants.SNA_HEADER_SIZE);
        header.setWordSwapped(SNAHeader.REG_AF, (byte) is.read(), (byte) is.read());
        header.setWord(SNAHeader.REG_BC, (byte) is.read(), (byte) is.read());
        header.setWord(SNAHeader.REG_HL, (byte) is.read(), (byte) is.read());
        int pc = Util.asLittleEndian(is);
        int sp = Util.asLittleEndian(is) - 2;
        header.setWord(SNAHeader.REG_SP, (byte)(sp & 0xff), (byte) ((sp >> 8) & 0xff));
        header.setByte(SNAHeader.REG_I, (byte) is.read());
        LOGGER.debug(String.format("PC: %04x, SP: %04x", pc, sp));
        byte r = (byte) (is.read() & 0x7F);
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
        header.setWord(SNAHeader.REG_IY, (byte) is.read(), (byte) is.read());
        header.setWord(SNAHeader.REG_IX, (byte) is.read(), (byte) is.read());

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
        List<byte[]> gameSlots = getGameImage(is, compressed, version1);
        int pcLocation = sp - 0x4000; //Offset of PC in game image (Starts at 0x4000)
        int pcSlot = pcLocation / Constants.SLOT_SIZE;
        int pcOffset = pcLocation % Constants.SLOT_SIZE;
        gameSlots.get(pcSlot)[pcOffset] = (byte) (pc & 0xFF);
        gameSlots.get(pcSlot)[pcOffset + 1] = (byte) ((pc >> 8) & 0xFF);

        if (!header.validate()) {
            throw new IllegalArgumentException("Header doesn't pass validations");
        }
        LOGGER.debug("Loaded Z80 game. SNAHeader: " + header);
        RamGame game = new RamGame(GameType.RAM48, gameSlots);
        game.setSnaHeader(header);
        return game;
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        throw new IllegalStateException("Not implemented yet");
    }

}

package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.compress.z80.Z80InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Z80GameImageLoader implements GameImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z80GameImageLoader.class);

    private static final int HWMODE_V23_48K = 0;
    private static final int HWMODE_V23_48K_IF1 = 1;
    private static final int HWMODE_V23_SAMRAM = 2;
    private static final int HWMODE_V2_128K = 3;
    private static final int HWMODE_V2_128K_IF1 = 4;

    private static final int HWMODE_V3_48K_MGT = 3;
    private static final int HWMODE_V3_128K = 4;
    private static final int HWMODE_V3_128_IF1 = 5;
    private static final int HWMODE_V3_128_MGT = 6;

    private static final int HWMODE_V23_PLUS3 = 7;
    private static final int HWMODE_V23_WRONG_PLUS3 = 8;
    private static final int HWMODE_V23_PENTAGON = 9;
    private static final int HWMODE_V23_SCORPION = 10;
    private static final int HWMODE_V23_DIDAKTIK = 11;
    private static final int HWMODE_V23_PLUS2 = 12;
    private static final int HWMODE_V23_PLUS2A = 13;
    private static final int HWMODE_V23_TC2048 = 14;
    private static final int HWMODE_V23_TC2068 = 15;
    private static final int HWMODE_V23_TS2068 = 128;

    private static byte[][] getGameImageV1(InputStream is, boolean compressed) throws IOException {
        LOGGER.debug("Loading Z80 version 1 image, compressed " + compressed);
        InputStream z80is = compressed ? new Z80InputStream(is) : is;
        byte[][] gameSlots = new byte[3][];
        for (int i = 0; i < 3; i++) {
            gameSlots[i] = Util.fromInputStream(z80is, Constants.SLOT_SIZE);
        }
        return gameSlots;
    }

    private static byte[] getCompressedChunk(InputStream is) throws IOException {
        Z80InputStream z80is = new Z80InputStream(is);
        return Util.fromInputStream(z80is, Constants.SLOT_SIZE);
    }

    private static byte[][] getGameImageV23(InputStream is) throws IOException {
        byte[][] gameSlots = new byte[12][];
        int pagesRead = 0;
        boolean eof = false;
        while (pagesRead < 8 && !eof) {
            int compressedBlockLen = Util.asLittleEndian(is);
            int pageNumber = is.read();
            LOGGER.debug("Reading page number " + pageNumber +
                    " of compressed size " + compressedBlockLen +
                    " from stream with " + is.available() +
                    " available bytes");
            if (pageNumber == -1) {
                LOGGER.info("EOF reading game pages");
                eof = true;
            } else if (pageNumber < gameSlots.length) {
                gameSlots[pageNumber] = getCompressedChunk(is);
                pagesRead++;
            } else {
                long skipped = is.skip(compressedBlockLen);
                LOGGER.info("Skipped " + skipped + " bytes due to unexpected page number "
                        + pageNumber);
            }
        }
        LOGGER.debug("Pages read " + pagesRead);
        return gameSlots;
    }

    private static byte[][] getGameImage(InputStream is, boolean compressed, boolean version1) throws IOException {
        return version1 ? getGameImageV1(is, compressed) : getGameImageV23(is);
    }

    private static boolean is48KGame(int version, int hwmode) {
        return version == 1 ||
                (version == 2 && (hwmode < HWMODE_V2_128K)) ||
                (version == 3 && (hwmode < HWMODE_V3_128K));
    }

    private static void injectPCintoStack(List<byte[]> gameSlots, int sp, int pc) {
        int pcLocation = sp - 0x4000; //Offset of PC in game image (Starts at 0x4000)
        int pcSlot = pcLocation / Constants.SLOT_SIZE;
        int pcOffset = pcLocation % Constants.SLOT_SIZE;
        gameSlots.get(pcSlot)[pcOffset] = (byte) (pc & 0xFF);
        gameSlots.get(pcSlot)[pcOffset + 1] = (byte) ((pc >> 8) & 0xFF);
    }

    private static RamGame createRamGameFromData(int version,
                                          int hwMode,
                                          int pc, int sp,
                                          byte[][] gameData) {
        if (version == 1) {
            LOGGER.debug("Assembling game as version 1 48K game");
            return new RamGame(GameType.RAM48, Arrays.asList(gameData));
        } else {
            ArrayList<byte[]> arrangedBlocks = new ArrayList<>();
            final int pageOffset = 3; //To map array positions to page numbers
            GameType gameType;
            if (is48KGame(version, hwMode)) {
                LOGGER.debug("Assembling game as version 2/3 48K game");
                arrangedBlocks.add(gameData[8]);
                arrangedBlocks.add(gameData[4]);
                arrangedBlocks.add(gameData[5]);
                gameType = GameType.RAM48;
                //injectPCintoStack(arrangedBlocks, sp, pc);
            } else {
                LOGGER.debug("Assembling game as version 2/3 128K game");
                arrangedBlocks.add(gameData[5 + pageOffset]);
                arrangedBlocks.add(gameData[2 + pageOffset]);
                for (int page : new Integer[] {0, 1, 3, 4, 6, 7}) {
                    arrangedBlocks.add(gameData[page + pageOffset]);
                }
                gameType = GameType.RAM128;
            }
            return new RamGame(gameType, arrangedBlocks);
        }
    }

    @Override
    public Game load(InputStream is) throws IOException {
        SNAHeader header = new SNAHeader(Constants.SNA_EXTENDED_HEADER_SIZE);
        header.setWordSwapped(SNAHeader.REG_AF, (byte) is.read(), (byte) is.read());
        header.setWord(SNAHeader.REG_BC, (byte) is.read(), (byte) is.read());
        header.setWord(SNAHeader.REG_HL, (byte) is.read(), (byte) is.read());
        int pc = Util.asLittleEndian(is);
        int sp = Util.asLittleEndian(is);
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

        int version = 1;
        boolean version1 = (pc != 0);
        int hwMode = -1;
        int c000MappedPage = -1;
        if (!version1) {
            //Version 2 or 3
            int headerLength = Util.asLittleEndian(is);
            version = headerLength == 23 ? 2 : 3;
            pc = Util.asLittleEndian(is);
            hwMode = is.read();
            c000MappedPage = is.read();
            header.setByte(SNAHeader.PORT_7FFD, (byte) c000MappedPage);
            c000MappedPage &= 3;
            is.skip(headerLength - 4);
        }
        header.setWord(SNAHeader.REG_PC, (byte) (pc & 0xff), (byte) ((pc >> 8) & 0xff));

        LOGGER.debug("Version is " + version + ", hardware mode is " + hwMode
                + ", c000MappedPage is " + c000MappedPage);
        byte[][] gameSlots = getGameImage(is, compressed, version1);

        if (!header.validate()) {
            throw new IllegalArgumentException("Header doesn't pass validations");
        }
        RamGame game = createRamGameFromData(version, hwMode, pc, sp, gameSlots);
        game.setSnaHeader(header);
        GameUtil.injectPCIntoStack(game);
        LOGGER.debug("Loaded Z80 game. SNAHeader: " + header);
        return game;
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        throw new IllegalStateException("Not implemented yet");
    }

}

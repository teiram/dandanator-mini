package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import com.grelobites.romgenerator.util.compress.z80.Z80InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

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

    private static byte[] getCompressedChunk(InputStream is, int compressedBlockLen) throws IOException {
        Z80InputStream z80is = new Z80InputStream(is, compressedBlockLen);
        return Util.fromInputStream(z80is, Constants.SLOT_SIZE);
    }

    private static byte[][] getGameImageV23(InputStream is) throws IOException {
        byte[][] gameSlots = new byte[12][];
        int pagesRead = 0;
        boolean eof = false;
        while (pagesRead < 8 && !eof) {
            int compressedBlockLen = Util.readAsLittleEndian(is);
            int pageNumber = is.read();
            LOGGER.debug("Reading page number " + pageNumber +
                    " of compressed size " + compressedBlockLen +
                    " from stream with " + is.available() +
                    " available bytes");
            if (pageNumber == -1) {
                LOGGER.info("EOF reading game pages");
                eof = true;
            } else if (pageNumber < gameSlots.length) {
                gameSlots[pageNumber] = getCompressedChunk(is, compressedBlockLen);
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

    private static RamGame createRamGameFromData(int version,
                                          int hwMode,
                                          GameHeader header,
                                          byte[][] gameData) {
        RamGame game;
        if (version == 1) {
            LOGGER.debug("Assembling game as version 1 48K game");
            game =  new RamGame(GameType.RAM48, Arrays.asList(gameData));
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
            } else {
                LOGGER.debug("Assembling game as version 2/3 128K game");
                arrangedBlocks.add(gameData[5 + pageOffset]);
                arrangedBlocks.add(gameData[2 + pageOffset]);
                for (int page : new Integer[]{0, 1, 3, 4, 6, 7}) {
                    arrangedBlocks.add(gameData[page + pageOffset]);
                }
                gameType = GameType.RAM128;
            }
            game = new RamGame(gameType, arrangedBlocks);
        }
        game.setGameHeader(header);
        return game;
    }

    @Override
    public Game load(InputStream is) throws IOException {
        GameHeader header = new GameHeader();
        header.setAFRegister(Util.readAsBigEndian(is));
        header.setBCRegister(Util.readAsLittleEndian(is));
        header.setHLRegister(Util.readAsLittleEndian(is));
        header.setPCRegister(Util.readAsLittleEndian(is));
        header.setSPRegister(Util.readAsLittleEndian(is));
        header.setIRegister(is.read());
        LOGGER.debug(String.format("PC: %04x, SP: %04x", header.getPCRegister(), header.getSPRegister()));
        byte r = (byte) (is.read() & 0x7F);
        byte info = (byte) is.read();
        LOGGER.debug("Info byte: " + info);
        if (info == (byte) 0xff) info = 1; //Compatibility issue
        header.setRRegister((byte) (r | ((info & 0x01) << 7)));
        header.setBorderColor((byte) ((info & 0x0E) >> 1));

        boolean compressed = (info & 0x20) != 0;

        header.setDERegister(Util.readAsLittleEndian(is));
        header.setAlternateBCRegister(Util.readAsLittleEndian(is));
        header.setAlternateDERegister(Util.readAsLittleEndian(is));
        header.setAlternateHLRegister(Util.readAsLittleEndian(is));
        header.setAlternateAFRegister(Util.readAsBigEndian(is));
        header.setIYRegister(Util.readAsLittleEndian(is));
        header.setIXRegister(Util.readAsLittleEndian(is));
        header.setInterruptEnable(is.read() == 0 ? 0x00: 0xff);
        is.read(); //Skip IFF2 byte

        header.setInterruptMode(is.read() & 0x3);

        int version = 1;
        boolean version1 = (header.getPCRegister() != 0);
        int hwMode = -1;
        int c000MappedPage = -1;
        if (!version1) {
            //Version 2 or 3
            int headerLength = Util.readAsLittleEndian(is);
            version = headerLength == 23 ? 2 : 3;
            header.setPCRegister(Util.readAsLittleEndian(is));
            hwMode = is.read();
            c000MappedPage = is.read();
            header.setPort7ffdValue(c000MappedPage);
            c000MappedPage &= 3;
            if (headerLength < 55) {
                is.skip(headerLength - 4);
            } else {
                is.skip(headerLength - 5);
                header.setPort1ffdValue(is.read());
                if ((header.getPort1ffdValue() & 1) != 0) {
                    throw new IllegalArgumentException("Unsupported 1FFD port value");
                }
            }
        }

        LOGGER.debug("Version is " + version + ", hardware mode is " + hwMode
                + ", c000MappedPage is " + c000MappedPage);
        byte[][] gameSlots = getGameImage(is, compressed, version1);
        RamGame game = createRamGameFromData(version, hwMode, header, gameSlots);
        GameUtil.pushPC(game);
        LOGGER.debug("Loaded Z80 game. Header: " + header);
        return game;
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        throw new IllegalStateException("Not implemented yet");
    }

}

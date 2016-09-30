package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.z80.Z80OutputStream;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import com.grelobites.romgenerator.util.compress.z80.Z80InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

public class Z80GameImageLoader implements GameImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z80GameImageLoader.class);

    private static final int HEADER_BASE_LENGTH = 30;
    private static final int HEADER_V3_EXTENSION_LENGTH = 55;
    private static final int HEADER_V3_LENGTH = HEADER_BASE_LENGTH + 2 + HEADER_V3_EXTENSION_LENGTH;

    private static final int Z80_PAGE_OFFSET = 3;

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

    private static byte[] getCompressedChunkDebug(InputStream is, int compressedBlockLen) throws IOException {
        byte[] data = Util.fromInputStream(is, compressedBlockLen);
        FileOutputStream fos = new FileOutputStream("/Users/mteira/Desktop/" + compressedBlockLen + ".bin");
        fos.write(data);
        fos.close();
        Z80InputStream z80is = new Z80InputStream(new ByteArrayInputStream(data));
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

    private static boolean is48KGame(HardwareMode hardwareMode) {
        return hardwareMode.intValue() == 0;
    }

    private static RamGame createRamGameFromData(int version,
                                          HardwareMode hardwareMode,
                                          GameHeader header,
                                          byte[][] gameData) {
        RamGame game;
        if (version == 1) {
            LOGGER.debug("Assembling game as version 1 48K game");
            game =  new RamGame(GameType.RAM48, Arrays.asList(gameData));
        } else {
            ArrayList<byte[]> arrangedBlocks = new ArrayList<>();
            GameType gameType;
            if (is48KGame(hardwareMode)) {
                LOGGER.debug("Assembling game as version 2/3 48K game");
                arrangedBlocks.add(gameData[8]);
                arrangedBlocks.add(gameData[4]);
                arrangedBlocks.add(gameData[5]);
                gameType = GameType.RAM48;
            } else {
                LOGGER.debug("Assembling game as version 2/3 128K game");
                arrangedBlocks.add(gameData[5 + Z80_PAGE_OFFSET]);
                arrangedBlocks.add(gameData[2 + Z80_PAGE_OFFSET]);
                for (int page : new Integer[]{0, 1, 3, 4, 6, 7}) {
                    arrangedBlocks.add(gameData[page + Z80_PAGE_OFFSET]);
                }
                gameType = GameType.RAM128;
            }
            game = new RamGame(gameType, arrangedBlocks);
        }
        game.setGameHeader(header);
        game.setHardwareMode(hardwareMode);
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
        HardwareMode hardwareMode = HardwareMode.fromZ80Mode(version, hwMode);
        if (Configuration.getInstance().isAllowExperimentalGames() || hardwareMode.supported()) {
            RamGame game = createRamGameFromData(version, hardwareMode, header, gameSlots);
            GameUtil.pushPC(game);
            LOGGER.debug("Loaded Z80 game. Header: " + header);
            return game;
        } else {
            LOGGER.warn("Game captured on unsupported hardware " + hardwareMode);
            throw new IllegalArgumentException("Unsupported Z80 hardware");
        }
    }

    private static byte[] getGameZ80Header(RamGame game) {
        GameHeader header = game.getGameHeader();
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_V3_LENGTH)
                .order(ByteOrder.BIG_ENDIAN)
                .putShort(header.getAFRegister().shortValue())
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(header.getBCRegister().shortValue())
                .putShort(header.getHLRegister().shortValue())
                .putShort(Integer.valueOf(0).shortValue())      //PC is stored in extra header in Z80 v3
                .putShort(header.getSPRegister().shortValue())
                .put(header.getIRegister().byteValue())
                .put((byte) (header.getRRegister() & 0x7f))
                .put((byte) ((header.getRRegister() >> 7) | (header.getBorderColor() << 1) | 0x20))
                .putShort(header.getDERegister().shortValue())
                .putShort(header.getAlternateBCRegister().shortValue())
                .putShort(header.getAlternateDERegister().shortValue())
                .putShort(header.getAlternateHLRegister().shortValue())
                .order(ByteOrder.BIG_ENDIAN)
                .putShort(header.getAlternateAFRegister().shortValue())
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(header.getIYRegister().shortValue())
                .putShort(header.getIXRegister().shortValue())
                .put(header.getInterruptEnable().byteValue())
                .put(Integer.valueOf(0).byteValue())
                .put(header.getInterruptMode().byteValue()) //First part of the header

                .putShort((short) HEADER_V3_EXTENSION_LENGTH)
                .putShort(header.getPCRegister().shortValue())
                .put(Integer.valueOf(game.getHardwareMode().supported() ?
                        game.getHardwareMode().intValue() : HardwareMode.HW_128K.intValue()).byteValue())
                .put(GameUtil.decodeAsAuthentic(header
                        .getPort7ffdValue(DandanatorMiniConstants.PORT7FFD_DEFAULT_VALUE |
                        (game.getForce48kMode() ? DandanatorMiniConstants.PORT7FFD_FORCED_48KMODE_BITS : 0)))
                        .byteValue())
                .put(86, GameUtil.decodeAsAuthentic(header
                        .getPort1ffdValue(DandanatorMiniConstants.PORT1FFD_DEFAULT_VALUE))
                        .byteValue());
        return buffer.array();
    }

    private static byte[] getCompressedZ80Block(byte[] data, int page) throws IOException {
        ByteArrayOutputStream compressedBlock = new ByteArrayOutputStream();
        Z80OutputStream zos = new Z80OutputStream(compressedBlock);
        zos.write(data);
        zos.close();
        LOGGER.debug("Game page " + page + " compressed to " + compressedBlock.size());
        return ByteBuffer.allocate(compressedBlock.size() + 3)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(Integer.valueOf(compressedBlock.size()).shortValue())
                .put(Integer.valueOf(page).byteValue())
                .put(compressedBlock.toByteArray())
                .array();
    }

    private static void saveAsZ80(RamGame game, OutputStream os) throws IOException {
        os.write(getGameZ80Header(game));
        LOGGER.debug("Saving as Z80 game with " + game.getSlotCount() + " slots");
        if (game.getType() == GameType.RAM48) {
            os.write(getCompressedZ80Block(game.getSlot(0), 8));
            os.write(getCompressedZ80Block(game.getSlot(1), 4));
            os.write(getCompressedZ80Block(game.getSlot(2), 5));
        } else {
            for (int i = 0; i < game.getSlotCount(); i++) {
                os.write(getCompressedZ80Block(game.getSlot(game.getSlotForBank(i)), i + Z80_PAGE_OFFSET));
            }
        }
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            GameUtil.popPC(ramGame);
            try {
                saveAsZ80((RamGame) game, os);
            } finally {
                GameUtil.pushPC(ramGame);
            }
        } else {
            throw new IllegalArgumentException("Non RAM Games cannot be saved as Z80");
        }
    }
}

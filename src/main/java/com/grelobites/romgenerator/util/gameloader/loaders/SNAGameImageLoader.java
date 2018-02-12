package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.*;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SNAGameImageLoader implements GameImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(SNAGameImageLoader.class);

    private static final int SNA_48K_SIZE = Constants.SNA_HEADER_SIZE + Constants.SLOT_SIZE * 3;
    private static final int SNA_128KLO_SIZE = Constants.SNA_EXTENDED_HEADER_SIZE + Constants.SLOT_SIZE * 8;
    private static final int SNA_128KHI_SIZE = Constants.SNA_EXTENDED_HEADER_SIZE + Constants.SLOT_SIZE * 9;
    private static final int[] INDEX_MAP = new int[]{2, 3, 1, 4, 5, 0, 6, 7};

    @Override
    public Game load(InputStream is) throws IOException {
        byte[] gameImage = Util.fromInputStream(is);
        GameHeader header;
        List<byte[]> gameSlots;
        GameType gameType;
        LOGGER.debug("Read " + gameImage.length + " bytes from game image");
        if (gameImage.length == SNA_48K_SIZE) {
            header = GameHeader.from48kSnaGameByteArray(gameImage);
            gameSlots = get48kGameSlots(gameImage);
            gameType = GameType.RAM48;
        } else if (gameImage.length == SNA_128KLO_SIZE || gameImage.length == SNA_128KHI_SIZE) {
            header = GameHeader.from128kSnaGameByteArray(gameImage);
            gameSlots = get128kGameSlots(gameImage, header);
            gameType = GameType.RAM128;
        } else {
            throw new IllegalArgumentException("Unsupported SNA size: " + gameImage.length);
        }
        SnapshotGame game = new SnapshotGame(gameType, gameSlots);
        game.setGameHeader(header);
        if (gameType == GameType.RAM128) {
            GameUtil.pushPC(game);
            game.setHardwareMode(HardwareMode.HW_128K);
        } else {
            game.setHardwareMode(HardwareMode.HW_48K);
        }
        return game;
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        if (game instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) game;

            switch (snapshotGame.getType()) {
                case RAM48:
                    save48kSna(snapshotGame, os);
                    break;
                case RAM128:
                    save128kSna(snapshotGame, os);
                    break;
            }
        } else {
            throw new IllegalArgumentException("Non RAM games cannot be saved as SNA");
        }
    }

    private static void writeSnaHeader(GameHeader header, OutputStream os) throws IOException {
        os.write(header.getIRegister());
        Util.writeAsLittleEndian(os, header.getAlternateHLRegister());
        Util.writeAsLittleEndian(os, header.getAlternateDERegister());
        Util.writeAsLittleEndian(os, header.getAlternateBCRegister());
        Util.writeAsLittleEndian(os, header.getAlternateAFRegister());
        Util.writeAsLittleEndian(os, header.getHLRegister());
        Util.writeAsLittleEndian(os, header.getDERegister());
        Util.writeAsLittleEndian(os, header.getBCRegister());
        Util.writeAsLittleEndian(os, header.getIYRegister());
        Util.writeAsLittleEndian(os, header.getIXRegister());
        os.write(header.getInterruptEnable());
        os.write(header.getRRegister());
        Util.writeAsLittleEndian(os, header.getAFRegister());
        Util.writeAsLittleEndian(os, header.getSPRegister());
        os.write(header.getInterruptMode());
        os.write(header.getBorderColor());
    }

    private static void save48kSna(SnapshotGame game, OutputStream os) throws IOException {
        writeSnaHeader(game.getGameHeader(), os);
        for (int i = 0; i < 3; i++) {
            os.write(game.getSlot(i));
        }
    }

    private static void save128kSna(SnapshotGame game, OutputStream os) throws IOException {
        try {
            GameUtil.popPC(game);
            writeSnaHeader(game.getGameHeader(), os);
            for (int i = 0; i < 2; i++) {
                os.write(game.getSlot(i));
            }
            int mappedBankIndex = game.getGameHeader().getPort7ffdValue(GameHeader.DEFAULT_PORT_7FFD_VALUE) & 0x03;
            os.write(game.getSlot(INDEX_MAP[mappedBankIndex]));
            Util.writeAsLittleEndian(os, game.getGameHeader().getPCRegister());
            os.write(GameUtil.decodeAsAuthentic(game.getGameHeader()
                    .getPort7ffdValue(GameHeader.DEFAULT_PORT_7FFD_VALUE)));
            os.write(Constants.B_00); //TRDOS_MAPPED_ROM

            for (int bank : new Integer[]{0, 1, 3, 4, 6, 7}) {
                if (bank != mappedBankIndex) {
                    os.write(game.getSlot(INDEX_MAP[bank]));
                }
            }
        } finally {
            GameUtil.pushPC(game);
        }
    }

    private static List<byte[]> get48kGameSlots(byte[] gameImage) {
        ArrayList<byte[]> slots = new ArrayList<>();
        int offset = Constants.SNA_HEADER_SIZE;
        for (int i = 0; i < 3; i++) {
            slots.add(Arrays.copyOfRange(gameImage, offset, offset + Constants.SLOT_SIZE));
            offset += Constants.SLOT_SIZE;
        }
        return slots;
    }

    private static List<byte[]> get128kGameSlots(byte[] gameImage, GameHeader header) {
        ArrayList<byte[]> slots = new ArrayList<>();
        int offset = Constants.SNA_HEADER_SIZE;
        boolean bigImage = gameImage.length == SNA_128KHI_SIZE;
        int mappedBankIndex = header.getPort7ffdValue(GameHeader.DEFAULT_PORT_7FFD_VALUE) & 0x03;
        LOGGER.debug("Mapped bank index is " + mappedBankIndex);
        byte[] mappedBank = null;
        for (int i = 0; i < 3; i++) {
            if (i < 2) {
                slots.add(Arrays.copyOfRange(gameImage, offset, offset + Constants.SLOT_SIZE));
            } else {
                if (!bigImage) {
                    LOGGER.debug("Storing mapped bank from SNA index " + i);
                    mappedBank = Arrays.copyOfRange(gameImage, offset, offset + Constants.SLOT_SIZE);
                }
            }
            offset += Constants.SLOT_SIZE;
        }
        offset +=  4; //4 bytes for the extra header bits
        for (int bank : new Integer[] {0, 1, 3, 4, 6, 7}) {
            LOGGER.debug("Adding bank " + bank + " to game. Offset is " + offset);
            if (mappedBankIndex == bank && !bigImage) {
                LOGGER.debug("Added saved mapped bank!");
                slots.add(mappedBank);
            } else {
                slots.add(Arrays.copyOfRange(gameImage, offset, offset + Constants.SLOT_SIZE));
                offset += Constants.SLOT_SIZE;
            }
        }
        return slots;
    }
}

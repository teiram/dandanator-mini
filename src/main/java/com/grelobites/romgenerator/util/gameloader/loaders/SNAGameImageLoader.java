package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.SNAHeader;
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
        SNAHeader header;
        List<byte[]> gameSlots;
        GameType gameType;
        LOGGER.debug("Read " + gameImage.length + " bytes from game image");
        if (gameImage.length == SNA_48K_SIZE) {
            header = SNAHeader.from48kSNAGameByteArray(gameImage);
            gameSlots = get48kGameSlots(gameImage);
            gameType = GameType.RAM48;
        } else if (gameImage.length == SNA_128KLO_SIZE || gameImage.length == SNA_128KHI_SIZE) {
            header = SNAHeader.from128kSNAGameByteArray(gameImage);
            gameSlots = get128kGameSlots(gameImage, header);
            gameType = GameType.RAM128;
        } else {
            throw new IllegalArgumentException("Unsupported SNA size: " + gameImage.length);
        }
        boolean isValidSnaImage = header.validate();
        if (isValidSnaImage) {
            RamGame game = new RamGame(gameType, gameSlots);
            game.setSnaHeader(header);
            return game;
        } else {
            throw new IllegalArgumentException("SNA doesn't pass validations");
        }
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;

            switch (ramGame.getType()) {
                case RAM48:
                    save48kSna(ramGame, os);
                    break;
                case RAM128:
                    save128kSna(ramGame, os);
                    break;
            }
        } else {
            throw new IllegalArgumentException("Non RAM games cannot be saved as SNA");
        }
    }

    private static void save48kSna(RamGame game, OutputStream os) throws IOException {
        os.write(game.getSnaHeader().asByteArray(), 0, Constants.SNA_HEADER_SIZE);
        for (int i = 0; i < 3; i++) {
            os.write(game.getSlot(i));
        }
    }

    private static void save128kSna(RamGame game, OutputStream os) throws IOException {
        byte[] snaHeader = game.getSnaHeader().asByteArray();
        os.write(snaHeader, 0, Constants.SNA_HEADER_SIZE);
        for (int i = 0; i < 2; i++) {
            os.write(game.getSlot(i));
        }
        int mappedBankIndex = game.getSnaHeader().getValue(SNAHeader.PORT_7FFD) & 0x03;
        os.write(game.getSlot(INDEX_MAP[mappedBankIndex]));

        os.write(snaHeader, Constants.SNA_HEADER_SIZE,
                Constants.SNA_EXTENDED_HEADER_SIZE - Constants.SNA_HEADER_SIZE);

        for (int bank : new Integer[] {0, 1, 3, 4, 6, 7}) {
            if (bank != mappedBankIndex) {
                os.write(game.getSlot(INDEX_MAP[bank]));
            }
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

    private static List<byte[]> get128kGameSlots(byte[] gameImage, SNAHeader header) {
        ArrayList<byte[]> slots = new ArrayList<>();
        int offset = Constants.SNA_HEADER_SIZE;
        boolean bigImage = gameImage.length == SNA_128KHI_SIZE;
        int mappedBankIndex = header.getValue(SNAHeader.PORT_7FFD) & 0x03;
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

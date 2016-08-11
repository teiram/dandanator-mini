package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.Z80Opcode;
import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.view.ApplicationContext;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DandanatorMiniCompressedRomSetHandler extends DandanatorMiniRomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniCompressedRomSetHandler.class);

    private static final int CBLOCKS_TABLE_OFFSET = 6642;
    private static final int CBLOCKS_TABLE_SIZE = 20;

    private int currentSize = 0;

    public DandanatorMiniCompressedRomSetHandler() throws IOException {
        super();
    }

    private static Compressor getCompressor() {
        return DandanatorMiniConfiguration.getInstance()
                .getCompressor();
    }

    private static byte[] compress(byte[]... sources) throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        OutputStream os = getCompressor().getCompressingOutputStream(target);
        for (byte[] source : sources) {
            os.write(source);
        }
        os.close();
        return target.toByteArray();
    }

    private static byte[] getGamePaddedSnaHeader(Game game) {
        byte[] paddedHeader = new byte[32];
        Arrays.fill(paddedHeader, (byte) 0xff);
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            byte[] snaHeader = ramGame.getSnaHeader().asByteArray();
            System.arraycopy(snaHeader, 0, paddedHeader, 0, snaHeader.length);
        }
        return paddedHeader;
    }

    private int dumpUncompressedGameCBlocks(OutputStream os, Game game, int offset)
        throws IOException {
        LOGGER.debug("Writing CBlocks for uncompressed game " + game.getName()
                + ", of type " + game.getType()
                + ", at offset " + offset);
        ByteArrayOutputStream gameCBlocks = new ByteArrayOutputStream();

        for (int i = 0; i < game.getSlotCount(); i++) {
            byte[] block = game.getSlot(i);
            offset -= Constants.SLOT_SIZE;
            LOGGER.debug("Writing CBlock with offset " + offset + " and length " + block.length);
            gameCBlocks.write(offset / Constants.SLOT_SIZE + 1);
            gameCBlocks.write(asLittleEndianWord(Constants.B_00)); //Blocks always at offset 0 (uncompressed)
            gameCBlocks.write(asLittleEndianWord(Constants.SLOT_SIZE));
        }
        byte[] cBlocksArray = Util.paddedByteArray(gameCBlocks.toByteArray(), 5 * 9, (byte) 0xff);
        LOGGER.debug("CBlocks array calculated as " + Util.dumpAsHexString(cBlocksArray));
        os.write(cBlocksArray);
        return offset;
    }

    protected static int dumpGameLaunchCode(OutputStream os, Game game) throws IOException {
        byte[] header = new byte[16];
        os.write(header);
        LOGGER.warn("Adding empty game launch code!!");
        return header.length;
    }

    private int dumpCompressedGameCBlocks(OutputStream os, Game game, int offset)
        throws IOException {
        LOGGER.debug("Writing CBlocks for compressed game " + game.getName()
                + ", of type " + game.getType()
                + ", at offset " + offset);
        ByteArrayOutputStream gameCBlocks = new ByteArrayOutputStream();
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            List<byte[]> compressedBlocks = ramGame.getCompressedData(getCompressor());
            for (byte[] block: compressedBlocks) {
                LOGGER.debug("Writing CBlock with offset " + offset + " and length " + block.length);
                gameCBlocks.write(offset / Constants.SLOT_SIZE + 1);
                gameCBlocks.write(asLittleEndianWord(offset % Constants.SLOT_SIZE));
                gameCBlocks.write(asLittleEndianWord(block.length));
                offset += block.length;
            }
        } else {
            throw new IllegalArgumentException("Cannot extract compressed blocks from a non-RAM game");
        }
        //Fill the remaining space with 0xFF
        byte[] cBlocksArray = Util.paddedByteArray(gameCBlocks.toByteArray(), 5 * 9, (byte) 0xff);
        LOGGER.debug("CBlocks array calculated as " + Util.dumpAsHexString(cBlocksArray));
        os.write(cBlocksArray);
        return offset;
    }

    private int dumpGameHeader(OutputStream os, int index, Game game,
                               GameChunk gameChunk, int offset) throws IOException {
        os.write(getGamePaddedSnaHeader(game));
        dumpGameName(os, game, index);
        os.write(isGameCompressed(game)? Constants.B_01 : Constants.B_00);
        os.write(game.getType().typeId());
        os.write(isGameScreenHold(game) ? Constants.B_01 : Constants.B_00);
        os.write(isGameRom(game) ? Constants.B_10 : Constants.B_00);
        int codeSize = dumpGameLaunchCode(os, game);
        dumpGameRamCodeLocation(os, game, codeSize);
        os.write(asLittleEndianWord(gameChunk.addr));
        os.write(asLittleEndianWord(gameChunk.data.length));
        return isGameCompressed(game) ?
                dumpCompressedGameCBlocks(os, game, offset) :
                dumpUncompressedGameCBlocks(os, game, offset);
    }

    private void dumpGameHeaders(OutputStream os, GameChunk[] gameChunkTable) throws IOException {
        int index = 0;
        int forwardOffset = 0;
        int backwardsOffset = Constants.SLOT_SIZE * (DandanatorMiniConstants.GAME_SLOTS + 1);
        for (Game game: getApplicationContext().getGameList()) {
            if (isGameCompressed(game)) {
                forwardOffset = dumpGameHeader(os, index, game, gameChunkTable[index], forwardOffset);
            } else {
                backwardsOffset = dumpGameHeader(os, index, game, gameChunkTable[index], backwardsOffset);
            }
            index++;
        }
        os.write(DandanatorMiniConstants.MAX_GAMES);
    }

    private static byte[] getScreenThirdSection(byte[] fullScreen) {
        byte[] result = new byte[SCREEN_THIRD_PIXEL_SIZE + SCREEN_THIRD_ATTRINFO_SIZE];
        System.arraycopy(fullScreen, 0, result, 0, SCREEN_THIRD_PIXEL_SIZE);
        System.arraycopy(fullScreen, Constants.SPECTRUM_SCREEN_SIZE, result, SCREEN_THIRD_PIXEL_SIZE,
                SCREEN_THIRD_ATTRINFO_SIZE);
        return result;
    }

    private static byte[] getScreenTexts(DandanatorMiniConfiguration configuration) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            dumpScreenTexts(os, configuration);
            return os.toByteArray();
        }
    }

    private static byte[] getPokeStructureData(Collection<Game> games) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            for (Game game : games) {
                os.write(getGamePokeCount(game));
            }
            int basePokeAddress = DandanatorMiniConstants.POKE_TARGET_ADDRESS + 30 + 60;

            for (Game game : games) {
                os.write(asLittleEndianWord(basePokeAddress));
                basePokeAddress += pokeRequiredSize(game);
            }
            for (Game game : games) {
                dumpGamePokeData(os, game);
            }
            return os.toByteArray();
        }
    }

    private static GameChunk getCompressedGameChunk(RamGame game, int cBlockOffset) throws IOException {
        try (ByteArrayOutputStream compressedChunk = new ByteArrayOutputStream()) {
            OutputStream compressingOs = getCompressor().getCompressingOutputStream(compressedChunk);
            compressingOs.write(game.getSlot(1), Constants.SLOT_SIZE - 256, 256);
            compressingOs.flush();
            GameChunk gameChunk = new GameChunk();
            gameChunk.addr = cBlockOffset;
            gameChunk.data = compressedChunk.toByteArray();
            return gameChunk;
        }
    }

    private static GameChunk[] calculateGameChunkTable(Collection<Game> games, int cBlockOffset) throws IOException {
        List<GameChunk> chunkList = new ArrayList<>();
        for (Game game: games) {
            if (game instanceof RamGame) {
                RamGame ramGame = (RamGame) game;
                GameChunk gameChunk = getCompressedGameChunk(ramGame, cBlockOffset);
                cBlockOffset += gameChunk.data.length;
                chunkList.add(gameChunk);
            }
        }
        return chunkList.toArray(new GameChunk[0]);
    }

    private void dumpCompressedGameData(OutputStream os, Game game) throws IOException {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            for (byte[] compressedChunk: ramGame.getCompressedData(getCompressor())) {
                os.write(compressedChunk);
                LOGGER.debug("Dumped compressed chunk for game " + ramGame.getName()
                        + " of size: " + compressedChunk.length);
            }
        }
    }

    private void dumpUncompressedGameData(OutputStream os, Game game) throws IOException {
        for (int i = 0; i < game.getSlotCount(); i++) {
            os.write(game.getSlot(i));
            LOGGER.debug("Dumped uncompressed chunk for game " + game.getName());
        }
    }

    @Override
    public void exportRomSet(OutputStream stream) {
        try {
            Configuration configuration = Configuration.getInstance();
            DandanatorMiniConfiguration dmConfiguration = DandanatorMiniConfiguration.getInstance();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Collection<Game> games = getApplicationContext().getGameList();
            os.write(dmConfiguration.getDandanatorRom(), 0, DandanatorMiniConstants.BASEROM_SIZE);
            LOGGER.debug("Dumped base ROM. Offset: " + os.size());

            os.write((byte) games.size());
            LOGGER.debug("Dumped game count. Offset: " + os.size());

            ByteArrayOutputStream cBlocksTable = new ByteArrayOutputStream();
            int cBlockOffset = CBLOCKS_TABLE_OFFSET + CBLOCKS_TABLE_SIZE;

            byte[] compressedScreen = compress(getScreenThirdSection(configuration.getBackgroundImage()));
            cBlocksTable.write(asLittleEndianWord(cBlockOffset));
            cBlocksTable.write(asLittleEndianWord(compressedScreen.length));
            cBlockOffset += compressedScreen.length;

            byte[] compressedScreenTexts = compress(getScreenTexts(dmConfiguration));
            cBlocksTable.write(asLittleEndianWord(cBlockOffset));
            cBlocksTable.write(asLittleEndianWord(compressedScreenTexts.length));
            cBlockOffset += compressedScreenTexts.length;

            byte[] compressedPokeData = compress(getPokeStructureData(games));
            cBlocksTable.write(asLittleEndianWord(cBlockOffset));
            cBlocksTable.write(asLittleEndianWord(compressedPokeData.length));
            cBlockOffset += compressedPokeData.length;

            byte[] compressedCharSetAndFirmware = compress(configuration.getCharSet(),
                    dmConfiguration.getDandanatorPicFirmware());
            cBlocksTable.write(asLittleEndianWord(cBlockOffset));
            cBlocksTable.write(asLittleEndianWord(compressedCharSetAndFirmware.length));
            cBlockOffset += compressedCharSetAndFirmware.length;

            GameChunk[] gameChunkTable = calculateGameChunkTable(games, cBlockOffset);

            dumpGameHeaders(os, gameChunkTable);

            os.write(cBlocksTable.toByteArray());
            LOGGER.debug("Dumped CBlocks table. Offset " + os.size());
            os.write(compressedScreen);
            os.write(compressedScreenTexts);
            os.write(compressedPokeData);
            os.write(compressedCharSetAndFirmware);

            LOGGER.debug("Dumped compressed data. Offset: " + os.size());

            for (GameChunk gameChunk : gameChunkTable) {
                os.write(gameChunk.data);
                LOGGER.debug("Dumped game chunk. Offset: " + os.size());
            }
            LOGGER.debug("Dumped all game chunks. Offset: " + os.size());

            fillWithValue(os, (byte) 0, Constants.SLOT_SIZE - os.size() - VERSION_SIZE);
            LOGGER.debug("Dumped padding zone. Offset: " + os.size());

            dumpVersionInfo(os);
            LOGGER.debug("Dumped version info. Offset: " + os.size());


            for (Game game : games) {
                if (isGameCompressed(game)) {
                    dumpCompressedGameData(os, game);
                    LOGGER.debug("Dumped compressed game. Offset: " + os.size());
                }
            }

            fillWithValue(os, Constants.B_00, os.size() % Constants.SLOT_SIZE);
            LOGGER.debug("Dumped alignment zone. Offset: " + os.size());

            for (Game game : games) {
                if (!isGameCompressed(game)) {
                    dumpUncompressedGameData(os, game);
                    LOGGER.debug("Dumped uncompressed game. Offset: " + os.size());
                }
            }

            os.write(dmConfiguration.getExtraRom());
            LOGGER.debug("Dumped custom rom. Offset: " + os.size());

            os.flush();
            LOGGER.debug("All parts dumped and flushed. Offset: " + os.size());

            stream.write(os.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Creating RomSet", e);
        }
    }

    private int getGameSize(Game game) throws IOException {
        if (game.getType() == GameType.ROM) {
            return game.getSlotCount() * Constants.SLOT_SIZE;
        } else {
            if (game instanceof RamGame) {
                RamGame ramGame = (RamGame) game;
                if (ramGame.getCompressed()) {
                    return ramGame.getCompressedSize(getCompressor());
                } else {
                    return ramGame.getSlotCount() * Constants.SLOT_SIZE;
                }
            } else {
                throw new IllegalArgumentException("Unable to calculate size for game " + game);
            }
        }
    }

    protected BooleanBinding getGenerationAllowedBinding(ApplicationContext ctx) {
        return Bindings.size(ctx.getGameList())
                .isNotEqualTo(0);
    }

    @Override
    protected double calculateRomUsage() {
        int size = 0;
        for (Game game: getApplicationContext().getGameList()) {
            try {
                size += getGameSize(game);
                LOGGER.debug("After adding size of game " + game.getName() + ", subtotal: " + size);
            } catch (Exception e) {
                LOGGER.warn("Calculating game size usage", e);
            }
        }
        LOGGER.debug("Used size: " + size + ", total size: "
                + DandanatorMiniConstants.GAME_SLOTS * Constants.SLOT_SIZE);
        return ((double) size / (DandanatorMiniConstants.GAME_SLOTS * Constants.SLOT_SIZE));
    }

    @Override
    public boolean addGame(Game game) {
        getApplicationContext().addBackgroundTask(() -> {
            if (getApplicationContext().getGameList().size() < DandanatorMiniConstants.MAX_GAMES) {
                try {
                    int gameSize = getGameSize(game);
                    final int maxSize = DandanatorMiniConstants.GAME_SLOTS * Constants.SLOT_SIZE;
                    if ((currentSize + gameSize) < maxSize) {
                        currentSize += gameSize;
                        Platform.runLater(() -> {
                            getApplicationContext().getGameList().add(game);
                        });
                        LOGGER.debug("After adding game " + game.getName() + ", used size: " + currentSize);
                        //return true;
                    } else {
                        LOGGER.warn("Unable to add game of size " + gameSize + ". Currently used: " + currentSize);
                    }
                } catch (Exception e) {
                    LOGGER.error("Calculating game size", e);
                }
            } else {
                LOGGER.warn("Unable to add game. Game limit reached. Currently used size: " + currentSize);
            }
            return null;
        });
        return true;
    }

    @Override
    public void importRomSet(InputStream stream) {
        throw new IllegalStateException("Not implemented yet");
    }
}

class GameChunk {
    public int addr;
    public byte[] data;
}
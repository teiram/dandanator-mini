package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.util.compress.CompressorFactory;
import com.grelobites.romgenerator.util.compress.CompressorType;
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

    private static Compressor compressor = CompressorFactory.getCompressor(CompressorType.ZX7B);

    public DandanatorMiniCompressedRomSetHandler() throws IOException {
        super();
    }

    private static byte[] compress(byte [] source) throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        OutputStream os = compressor.getCompressingOutputStream(target);
        os.write(source);
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

    private int dumpGameCBlocks(OutputStream os, Game game, int offset)
        throws IOException {
        ByteArrayOutputStream gameCBlocks = new ByteArrayOutputStream();
        int compressedSize = 0;
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            List<byte[]> compressedBlocks = ramGame.getCompressedData(compressor);
            for (byte[] block: compressedBlocks) {
                gameCBlocks.write(offset / Constants.SLOT_SIZE + 1);
                gameCBlocks.write(asLittleEndianWord(offset % Constants.SLOT_SIZE));
                gameCBlocks.write(asLittleEndianWord(block.length));
            }
            compressedSize = ramGame.getCompressedSize(compressor);
        }
        //Fill the remaining space with 0xFF
        os.write(Util.paddedByteArray(gameCBlocks.toByteArray(), 5 * 9, (byte) 0xff));
        LOGGER.debug("Compressed size of " + game.getName() + " is " + compressedSize);
        return compressedSize;
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
        return dumpGameCBlocks(os, game, offset);
    }

    private void dumpGameHeaders(OutputStream os, GameChunk[] gameChunkTable) throws IOException {
        int index = 0;
        int offset = 0;
        for (Game game: controller.getGameList()) {
            offset += dumpGameHeader(os, index, game, gameChunkTable[index], offset);
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
            OutputStream compressingOs = compressor.getCompressingOutputStream(compressedChunk);
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

    private void dumpGameData(OutputStream os, Game game) throws IOException {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            for (byte[] compressedChunk: ramGame.getCompressedData(compressor)) {
                os.write(compressedChunk);
                LOGGER.debug("Dumped compressed chunk for game " + ramGame.getName()
                        + " of size: " + compressedChunk.length);
            }
        }
    }

    @Override
    public void exportRomSet(OutputStream stream) {
        try {
            Configuration configuration = Configuration.getInstance();
            DandanatorMiniConfiguration dmConfiguration = DandanatorMiniConfiguration.getInstance();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Collection<Game> games = controller.getGameList();
            os.write(dmConfiguration.getDandanatorRom(), 0, DandanatorMiniConstants.BASEROM_SIZE);
            LOGGER.debug("Dumped base ROM. Offset: " + os.size());

            os.write((byte) games.size());
            LOGGER.debug("Dumped game headers. Offset: " + os.size());

            ByteArrayOutputStream cBlocksTable = new ByteArrayOutputStream();
            int cBlockOffset = CBLOCKS_TABLE_OFFSET + CBLOCKS_TABLE_SIZE;
            byte[] compressedCharset = compress(configuration.getCharSet());
            cBlocksTable.write(asLittleEndianWord(cBlockOffset));
            cBlocksTable.write(asLittleEndianWord(compressedCharset.length));
            cBlockOffset += compressedCharset.length;

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

            byte[] compressedFirmware = compress(dmConfiguration.getDandanatorPicFirmware());
            cBlocksTable.write(asLittleEndianWord(cBlockOffset));
            cBlocksTable.write(asLittleEndianWord(compressedFirmware.length));
            cBlockOffset += compressedFirmware.length;

            GameChunk[] gameChunkTable = calculateGameChunkTable(games, cBlockOffset);

            dumpGameHeaders(os, gameChunkTable);

            os.write(cBlocksTable.toByteArray());
            LOGGER.debug("Dumped CBlocks table. Offset " + os.size());
            os.write(compressedCharset);
            os.write(compressedScreen);
            os.write(compressedScreenTexts);
            os.write(compressedFirmware);

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
                dumpGameData(os, game);
                LOGGER.debug("Dumped game. Offset: " + os.size());
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

    @Override
    public void importRomSet(InputStream stream) {
        throw new IllegalStateException("Not implemented yet");
    }
}

class GameChunk {
    public int addr;
    public byte[] data;
}
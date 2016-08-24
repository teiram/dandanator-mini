package com.grelobites.romgenerator.handlers.dandanatormini.v5;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.handlers.dandanatormini.v4.DandanatorMiniV4RomSetHandler;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.RamGameCompressor;
import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.Z80Opcode;
import com.grelobites.romgenerator.util.ZxColor;
import com.grelobites.romgenerator.util.ZxScreen;
import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandlerType;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

public class DandanatorMiniV5RomSetHandler extends DandanatorMiniV4RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniV5RomSetHandler.class);

    private static final int CBLOCKS_TABLE_OFFSET = 6641;
    private static final int CBLOCKS_TABLE_SIZE = 16;
    private static final int GAME_STRUCT_OFFSET = 2561;
    private static final int GAME_STRUCT_SIZE = 136;
    private static final int MAX_MENU_PAGES = 3;
    protected static final int GAME_LAUNCH_SIZE = 18;
    protected static final int SNA_HEADER_SIZE = 32;
    private static RamGameCompressor ramGameCompressor = new DandanatorMiniRamGameCompressor();
    private DoubleProperty currentRomUsage;

    private ZxScreen[] menuImages;
    private AnimationTimer previewUpdateTimer;
    private static final long SCREEN_UPDATE_PERIOD_NANOS = 3 * 1000000000L;

    private static void initializeMenuImages(ZxScreen[] menuImages) throws IOException {
        for (int i = 0; i < menuImages.length; i++) {
            menuImages[i] = new ZxScreen();
            updateBackgroundImage(menuImages[i]);
        }
    }

    public DandanatorMiniV5RomSetHandler() throws IOException {
        menuImages = new ZxScreen[MAX_MENU_PAGES];
        initializeMenuImages(menuImages);
        currentRomUsage = new SimpleDoubleProperty();
        previewUpdateTimer = new AnimationTimer() {
            int currentFrame = 0;
            long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate > SCREEN_UPDATE_PERIOD_NANOS) {
                    if (applicationContext != null) {
                        int nextFrame;
                        int gameCount = applicationContext.getGameList().size();
                        if (gameCount > ((currentFrame + 1) * DandanatorMiniConstants.SLOT_COUNT)) {
                            nextFrame = currentFrame + 1;
                        } else {
                            nextFrame = 0;
                        }
                        if (nextFrame >= menuImages.length) {
                            LOGGER.warn("Out of bounds calculated next frame " + nextFrame);
                            nextFrame = 0;
                        }
                        applicationContext.getMenuPreview().setImage(menuImages[nextFrame]);
                        currentFrame = nextFrame;
                        lastUpdate = now;
                    }
                }
            }
        };
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
        byte[] paddedHeader = new byte[SNA_HEADER_SIZE];
        Arrays.fill(paddedHeader, (byte) 0xff);
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            byte[] snaHeader = ramGame.getSnaHeader().asByteArray();
            System.arraycopy(snaHeader, 0, paddedHeader, 0, snaHeader.length);
        }
        return paddedHeader;
    }

    protected static int dumpGameLaunchCode(OutputStream os, Game game, int index) throws IOException {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;

            int baseAddress = GAME_STRUCT_OFFSET + GAME_STRUCT_SIZE * index;
            os.write(Z80Opcode.LD_IX_NN(baseAddress + SNAHeader.REG_IX));
            os.write(Z80Opcode.LD_SP_NN(baseAddress + SNAHeader.REG_SP));
            os.write(Z80Opcode.LD_NN_A(0));
            os.write(Z80Opcode.NOP);
            os.write(Z80Opcode.NOP);
            os.write(Z80Opcode.NOP);
            os.write(Z80Opcode.NOP);
            os.write(Z80Opcode.DEC_HL);
            os.write(
                    (ramGame.getSnaHeader().getValue(SNAHeader.INTERRUPT_ENABLE) & 0x04) == 0 ?
                        Z80Opcode.DI : Z80Opcode.EI);
            os.write(Z80Opcode.RET);
        } else {
            os.write(new byte[GAME_LAUNCH_SIZE]);
        }
        return GAME_LAUNCH_SIZE;
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

    private int dumpCompressedGameCBlocks(OutputStream os, Game game, int offset)
            throws IOException {
        LOGGER.debug("Writing CBlocks for compressed game " + game.getName()
                + ", of type " + game.getType()
                + ", at offset " + offset);
        ByteArrayOutputStream gameCBlocks = new ByteArrayOutputStream();
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            List<byte[]> compressedBlocks = ramGame.getCompressedData(ramGameCompressor);
            for (byte[] block : compressedBlocks) {
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
        os.write(isGameCompressed(game) ? Constants.B_01 : Constants.B_00);
        os.write(game.getType().typeId());
        os.write(isGameScreenHold(game) ? Constants.B_01 : Constants.B_00);
        os.write(isGameRom(game) ? Constants.B_10 : Constants.B_00);
        dumpGameLaunchCode(os, game, index);
        os.write(asLittleEndianWord(gameChunk.getAddress()));
        os.write(asLittleEndianWord(gameChunk.getData().length));
        return isGameCompressed(game) ?
                dumpCompressedGameCBlocks(os, game, offset) :
                dumpUncompressedGameCBlocks(os, game, offset);
    }

    private void dumpGameHeaders(ByteArrayOutputStream os, GameChunk[] gameChunkTable) throws IOException {
        int index = 0;
        int forwardOffset = 0;
        //backwardsOffset starts before the test ROM
        int backwardsOffset = Constants.SLOT_SIZE * (DandanatorMiniConstants.GAME_SLOTS + 1);
        for (Game game : getApplicationContext().getGameList()) {
            if (isGameCompressed(game)) {
                forwardOffset = dumpGameHeader(os, index, game, gameChunkTable[index], forwardOffset);
            } else {
                backwardsOffset = dumpGameHeader(os, index, game, gameChunkTable[index], backwardsOffset);
            }
            LOGGER.debug("Dumped gamestruct for " + game.getName() + ". Offset: " + os.size());
            index++;
        }
        fillWithValue(os, (byte) 0, GAME_STRUCT_SIZE * (DandanatorMiniConstants.MAX_GAMES - index));
        LOGGER.debug("Filled to end of gamestruct. Offset: " + os.size());
    }

    private static byte[] getScreenThirdSection(byte[] fullScreen) {
        byte[] result = new byte[Constants.SPECTRUM_FULLSCREEN_SIZE];
        System.arraycopy(fullScreen, 0, result, 0, SCREEN_THIRD_PIXEL_SIZE);
        System.arraycopy(fullScreen, Constants.SPECTRUM_SCREEN_SIZE, result, Constants.SPECTRUM_SCREEN_SIZE,
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
            fillWithValue(os, Constants.B_00, DandanatorMiniConstants.MAX_GAMES - games.size());

            int basePokeAddress = DandanatorMiniConstants.POKE_TARGET_ADDRESS +
                    DandanatorMiniConstants.MAX_GAMES * 3;

            for (Game game : games) {
                os.write(asLittleEndianWord(basePokeAddress));
                basePokeAddress += pokeRequiredSize(game);
            }
            fillWithValue(os, Constants.B_00, (DandanatorMiniConstants.MAX_GAMES - games.size()) * 2);

            for (Game game : games) {
                dumpGamePokeData(os, game);
            }
            LOGGER.debug("Poke Structure before compressing: " + Util.dumpAsHexString(os.toByteArray()));
            return os.toByteArray();

        }
    }

    private static GameChunk getCompressedGameChunk(RamGame game, int cBlockOffset) throws IOException {
        try (ByteArrayOutputStream compressedChunk = new ByteArrayOutputStream()) {
            OutputStream compressingOs = getCompressor().getCompressingOutputStream(compressedChunk);
            compressingOs.write(game.getSlot(DandanatorMiniConstants.GAME_CHUNK_SLOT),
                    Constants.SLOT_SIZE - DandanatorMiniConstants.GAME_CHUNK_SIZE,
                    DandanatorMiniConstants.GAME_CHUNK_SIZE);
            compressingOs.flush();
            GameChunk gameChunk = new GameChunk();
            gameChunk.setAddress(cBlockOffset);
            gameChunk.setData(compressedChunk.toByteArray());
            LOGGER.debug("Compressed chunk for game " + game.getName() + " calculated offset " +
                    gameChunk.getAddress());
            return gameChunk;
        }
    }

    private static GameChunk getUncompressedGameChunk(Game game, int cBlockOffset) throws IOException {
        GameChunk gameChunk = new GameChunk();
        gameChunk.setData(new byte[6]);
        LOGGER.warn("Using unimplemented getUncompressedChunk");
        return gameChunk;
    }

    private static GameChunk[] calculateGameChunkTable(Collection<Game> games, int cBlockOffset) throws IOException {
        List<GameChunk> chunkList = new ArrayList<>();
        for (Game game : games) {
            if (game instanceof RamGame) {
                RamGame ramGame = (RamGame) game;
                GameChunk gameChunk = getCompressedGameChunk(ramGame, cBlockOffset);
                cBlockOffset += gameChunk.getData().length;
                chunkList.add(gameChunk);
            } else {
                GameChunk gameChunk = getUncompressedGameChunk(game, cBlockOffset);
                cBlockOffset += gameChunk.getData().length;
                chunkList.add(gameChunk);
            }
        }
        return chunkList.toArray(new GameChunk[0]);
    }

    private void dumpCompressedGameData(OutputStream os, Game game) throws IOException {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            for (byte[] compressedChunk : ramGame.getCompressedData(ramGameCompressor)) {
                os.write(compressedChunk);
                LOGGER.debug("Dumped compressed chunk for game " + ramGame.getName()
                        + " of size: " + compressedChunk.length);
            }
        }
    }

    private void dumpUncompressedGameData(OutputStream os, Game game) throws IOException {
        for (int i = game.getSlotCount() - 1; i >= 0; i--) {
            os.write(game.getSlot(i));
            LOGGER.debug("Dumped uncompressed slot " + i + " for game " + game.getName());
        }
    }

    @Override
    public void exportRomSet(OutputStream stream) {
        try {
            Configuration configuration = Configuration.getInstance();
            DandanatorMiniConfiguration dmConfiguration = DandanatorMiniConfiguration.getInstance();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            List<Game> games = getApplicationContext().getGameList();
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
                    DandanatorMiniConstants.DANDANATOR_PIC_FW_HEADER.getBytes(),
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
                os.write(gameChunk.getData());
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

            ByteArrayOutputStream uncompressedStream = new ByteArrayOutputStream();
            for (int i = games.size() - 1; i >= 0; i--) {
                Game game = games.get(i);
                if (!isGameCompressed(game)) {
                    dumpUncompressedGameData(uncompressedStream, game);
                }
            }

            //Uncompressed data goes at the end minus the extra ROM size
            //and grows backwards
            int uncompressedOffset = Constants.SLOT_SIZE * (DandanatorMiniConstants.GAME_SLOTS + 1)
                    - uncompressedStream.size();
            int gapSize = uncompressedOffset - os.size();
            LOGGER.debug("Gap to uncompressed zone: " + gapSize);
            fillWithValue(os, Constants.B_00, gapSize);

            os.write(uncompressedStream.toByteArray());
            LOGGER.debug("Dumped uncompressed game data. Offset: " + os.size());

            os.write(dmConfiguration.getExtraRom());
            LOGGER.debug("Dumped custom rom. Offset: " + os.size());

            os.flush();
            LOGGER.debug("All parts dumped and flushed. Offset: " + os.size());

            stream.write(os.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Creating RomSet", e);
        }
    }

    private static int getGameSize(Game game) throws IOException {
        if (game.getType() == GameType.ROM) {
            return game.getSlotCount() * Constants.SLOT_SIZE;
        } else {
            if (game instanceof RamGame) {
                RamGame ramGame = (RamGame) game;
                if (ramGame.getCompressed()) {
                    return ramGame.getCompressedSize(ramGameCompressor);
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
                .isNotEqualTo(0).and(currentRomUsage.lessThan(1.0));
    }

    @Override
    protected double calculateRomUsage() {
        int size = 0;
        for (Game game : getApplicationContext().getGameList()) {
            try {
                size += getGameSize(game);
                LOGGER.debug("After adding size of game " + game.getName() + ", subtotal: " + size);
            } catch (Exception e) {
                LOGGER.warn("Calculating game size usage", e);
            }
        }
        LOGGER.debug("Used size: " + size + ", total size: "
                + DandanatorMiniConstants.GAME_SLOTS * Constants.SLOT_SIZE);
        currentRomUsage.set(((double) size /
                (DandanatorMiniConstants.GAME_SLOTS * Constants.SLOT_SIZE)));
        return currentRomUsage.get();
    }

    @Override
    public RomSetHandlerType type() {
        return RomSetHandlerType.DDNTR_V5;
    }

    protected String generateRomUsageDetail() {
        return String.format(LocaleUtil.i18n("romUsageV5Detail"),
                getApplicationContext().getGameList().size(),
                DandanatorMiniConstants.MAX_GAMES,
                calculateRomUsage() * 100);
    }

    private static int getCurrentSize(List<Game> gameList) throws IOException {
        int currentSize = 0;
        for (Game game : gameList) {
            currentSize += getGameSize(game);
        }
        return currentSize;
    }

    @Override
    public Future<OperationResult> addGame(Game game) {
        return getApplicationContext().addBackgroundTask(() -> {
            if (getApplicationContext().getGameList().size() < DandanatorMiniConstants.MAX_GAMES) {
                try {
                    int gameSize = getGameSize(game);
                    final int maxSize = DandanatorMiniConstants.GAME_SLOTS * Constants.SLOT_SIZE;
                    final int currentSize = getCurrentSize(getApplicationContext().getGameList());
                    if ((currentSize + gameSize) < maxSize) {
                        Platform.runLater(() -> getApplicationContext().getGameList().add(game));
                        LOGGER.debug("After adding game " + game.getName() + ", used size: " + currentSize);
                    } else {
                        LOGGER.warn("Unable to add game of size " + gameSize + ". Currently used: " + currentSize);
                        return OperationResult.errorWithDetailResult(LocaleUtil.i18n("gameImportError"),
                                String.format(LocaleUtil.i18n("gameImportErrorNoSpaceHeader"), game.getName()),
                                String.format(LocaleUtil.i18n("gameImportErrorNoSpaceContext"), currentSize));
                    }
                } catch (Exception e) {
                    LOGGER.error("Calculating game size", e);
                }
            } else {
                LOGGER.warn("Unable to add game. Game limit reached.");
                return OperationResult.errorResult(LocaleUtil.i18n("gameImportError"),
                        LocaleUtil.i18n("gameImportErrorNoSlotHeader"));
            }
            return OperationResult.successResult();
        });
    }

    private static void printVersionAndPageInfo(ZxScreen screen, int line, int page, int numPages) {
        String versionInfo = getVersionInfo();
        screen.setInk(ZxColor.BLACK);
        screen.setPen(ZxColor.BRIGHTMAGENTA);
        screen.printLine(versionInfo, line, 0);
        if (numPages > 1) {
            screen.setPen(ZxColor.WHITE);
            String pageInfo = numPages > 1 ?
                    String.format("%d/%d", page, numPages) : "";
            String keyInfo = "SPC - ";
            screen.printLine(keyInfo, line, screen.getColumns() - pageInfo.length() - keyInfo.length());
            screen.setPen(ZxColor.YELLOW);
            screen.printLine(pageInfo, line, screen.getColumns() - pageInfo.length());
        }
    }

    private void updateMenuPage(List<Game> gameList, int pageIndex, int numPages) throws IOException {
        DandanatorMiniConfiguration configuration = DandanatorMiniConfiguration.getInstance();
        ZxScreen page = menuImages[pageIndex];
        updateBackgroundImage(page);
        page.setCharSet(Configuration.getInstance().getCharSet());

        page.setInk(ZxColor.BLACK);
        page.setPen(ZxColor.BRIGHTMAGENTA);
        for (int line = page.getLines() - 1; line >= 8; line--) {
            page.deleteLine(line);
        }

        printVersionAndPageInfo(page, 8, pageIndex + 1, numPages);
        int line = 10;
        int gameIndex = pageIndex * DandanatorMiniConstants.SLOT_COUNT;
        int gameCount = 0;
        while (gameIndex < gameList.size() && gameCount < DandanatorMiniConstants.SLOT_COUNT) {
            Game game = gameList.get(gameIndex);
            page.setPen(
                    isGameScreenHold(game) ? ZxColor.BRIGHTCYAN : ZxColor.BRIGHTGREEN);
            page.deleteLine(line);
            page.printLine(
                    String.format("%d%c %s", (gameCount + 1) % DandanatorMiniConstants.SLOT_COUNT,
                            isGameRom(game) ? 'r' : '.',
                            game.getName()),
                    line++, 0);
            gameIndex++;
            gameCount++;
        }

        page.setPen(ZxColor.BRIGHTBLUE);
        page.printLine(String.format("P. %s", configuration.getTogglePokesMessage()), 21, 0);
        page.setPen(ZxColor.BRIGHTRED);
        page.printLine(String.format("R. %s", configuration.getExtraRomMessage()), 23, 0);
    }

    @Override
    public void updateMenuPreview() {
        LOGGER.debug("updateMenuPreview");
        try {
            List<Game> gameList = getApplicationContext().getGameList();
            int numPages = 1 + gameList.size() / (DandanatorMiniConstants.SLOT_COUNT + 1);
            for (int i = 0; i < numPages; i++) {
                updateMenuPage(gameList, i, numPages);
            }
        } catch (Exception e) {
            LOGGER.error("Updating background screen", e);
        }
    }

    @Override
    public void bind(ApplicationContext context) {
        super.bind(context);
        previewUpdateTimer.start();
    }

    @Override
    public void unbind() {
        super.unbind();
        previewUpdateTimer.stop();
    }
}
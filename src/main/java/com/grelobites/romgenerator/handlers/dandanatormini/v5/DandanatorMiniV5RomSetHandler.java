package com.grelobites.romgenerator.handlers.dandanatormini.v5;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.handlers.dandanatormini.ExtendedCharSet;
import com.grelobites.romgenerator.handlers.dandanatormini.v4.DandanatorMiniV4RomSetHandler;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.model.RomGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.ImageUtil;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

public class DandanatorMiniV5RomSetHandler extends DandanatorMiniV4RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniV5RomSetHandler.class);

    private static final byte[] EMPTY_CBLOCK = new byte[5];
    private static final int CBLOCKS_TABLE_OFFSET = 6348;
    private static final int CBLOCKS_TABLE_SIZE = 16;
    private static final int GAME_STRUCT_OFFSET = 3073;
    private static final int GAME_STRUCT_SIZE = 131;
    private static final int MAX_MENU_PAGES = 3;
    protected static final int GAME_LAUNCH_SIZE = 18;
    protected static final int SNA_HEADER_SIZE = 31;
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

    private static byte[] getEepromLoader(int offset) throws IOException {
        PlayerConfiguration configuration = PlayerConfiguration.getInstance();
        byte[] screen = Util.fromInputStream(configuration.getScreenStream());
        byte[] eewriter = Util.fromInputStream(configuration.getRomsetLoaderStream());
        byte[] compressedScreen = compress(screen);
        byte[] compressedWriter = compress(eewriter);
        return ByteBuffer.allocate(2 + compressedScreen.length + compressedWriter.length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(Integer.valueOf(compressedScreen.length + offset + 2).shortValue())
                .put(compressedScreen)
                .put(compressedWriter).array();
    }

    private static byte[] getGamePaddedSnaHeader(Game game) throws IOException {
        byte[] paddedHeader = new byte[SNA_HEADER_SIZE];
        Arrays.fill(paddedHeader, Constants.B_00);
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            GameHeaderV5Serializer.serialize(ramGame, os);
            byte[] snaHeader = os.toByteArray();
            System.arraycopy(snaHeader, 0, paddedHeader, 0, snaHeader.length);
        }
        return paddedHeader;
    }

    private static int getAnyRetCodeLocation(RamGame game) {
        int location = GameUtil.findInGameRam(game, Z80Opcode.RET);
        LOGGER.debug("Found RET opcode at offset 0x" + Integer.toHexString(location));
        if (location < 0) {
            byte[] screenSlot = game.getSlot(0);
            location = ImageUtil.getHiddenDisplayOffset(screenSlot, 1)
                    .orElse(Constants.SPECTRUM_SCREEN_SIZE - 1);
            LOGGER.debug("Injecting RET opcode at screen offset 0x" + Integer.toHexString(location));
            screenSlot[location] = Z80Opcode.RET;
            location += Constants.SLOT_SIZE;
        }
        LOGGER.debug("RET location calculated as 0x" + Integer.toHexString(location));
        return location;
    }

    protected static int dumpGameLaunchCode(OutputStream os, Game game, int index) throws IOException {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;

            int baseAddress = GAME_STRUCT_OFFSET + GAME_STRUCT_SIZE * index;
            int retLocation = getAnyRetCodeLocation(ramGame);
            os.write(Z80Opcode.LD_IX_NN(baseAddress + SNAHeader.REG_IX));
            os.write(Z80Opcode.LD_SP_NN(baseAddress + SNAHeader.REG_SP));
            os.write(Z80Opcode.LD_NN_A(0));
            boolean interruptDisable = (ramGame.getGameHeader().getInterruptEnable() & 0x04) == 0;

            os.write(Z80Opcode.NOP);
            os.write(Z80Opcode.NOP);
            os.write(Z80Opcode.NOP);
            os.write(interruptDisable ? Z80Opcode.DI : Z80Opcode.EI);
            os.write(Z80Opcode.JP_NN(retLocation));

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
            if (!game.isSlotZeroed(i)) {
                byte[] block = game.getSlot(i);
                offset -= Constants.SLOT_SIZE;
                LOGGER.debug("Writing CBlock with offset " + offset + " and length " + block.length);
                gameCBlocks.write(offset / Constants.SLOT_SIZE);
                gameCBlocks.write(asLittleEndianWord(Constants.B_00)); //Blocks always at offset 0 (uncompressed)
                //The chunk slot reports its size subtracting the chunk size (we are dumping the whole slot though)
                gameCBlocks.write(asLittleEndianWord(i == DandanatorMiniConstants.GAME_CHUNK_SLOT ?
                        Constants.SLOT_SIZE - DandanatorMiniConstants.GAME_CHUNK_SIZE : Constants.SLOT_SIZE));
            } else {
                LOGGER.debug("Writing empty CBlock");
                gameCBlocks.write(EMPTY_CBLOCK);
            }
        }
        byte[] cBlocksArray = Util.paddedByteArray(gameCBlocks.toByteArray(), 5 * 8, (byte) DandanatorMiniConstants.FILLER_BYTE);
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
                if (block != null) {
                    LOGGER.debug("Writing CBlock with offset " + offset + " and length " + block.length);
                    gameCBlocks.write(offset / Constants.SLOT_SIZE);
                    gameCBlocks.write(asLittleEndianWord(offset % Constants.SLOT_SIZE));
                    gameCBlocks.write(asLittleEndianWord(block.length));
                    offset += block.length;
                } else {
                    LOGGER.debug("Writing empty CBlock");
                    gameCBlocks.write(EMPTY_CBLOCK);
                }
            }
        } else {
            throw new IllegalArgumentException("Cannot extract compressed blocks from a non-RAM game");
        }
        //Fill the remaining space with 0xFF
        byte[] cBlocksArray = Util.paddedByteArray(gameCBlocks.toByteArray(), 5 * 8, (byte) DandanatorMiniConstants.FILLER_BYTE);
        LOGGER.debug("CBlocks array calculated as " + Util.dumpAsHexString(cBlocksArray));
        os.write(cBlocksArray);
        return offset;
    }

    protected static void dumpGameName(OutputStream os, Game game, int index) throws IOException {
        int gameSymbolCode = getGameSymbolCode(game);
        String gameName = String.format("%1d%c%c%s", (index + 1) % DandanatorMiniConstants.SLOT_COUNT,
                gameSymbolCode, gameSymbolCode + 1,
                game.getName());
        os.write(asNullTerminatedByteArray(gameName, DandanatorMiniConstants.GAMENAME_SIZE));
    }

    private static int getGameHardwareMode(Game game) {
        if (game instanceof RamGame) {
            return ((RamGame) game).getHardwareMode().intValue();
        } else {
            return 0;
        }
    }

    private int dumpGameHeader(OutputStream os, int index, Game game,
                               GameChunk gameChunk, int offset) throws IOException {
        os.write(getGamePaddedSnaHeader(game));
        dumpGameName(os, game, index);
        os.write(getGameHardwareMode(game));
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
        //forwardOffset after the slot zero
        int forwardOffset = Constants.SLOT_SIZE;
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
            byte[] compressedData = compressedChunk.toByteArray();
            if (compressedData.length > (DandanatorMiniConstants.GAME_CHUNK_SIZE - 6)) {
                LOGGER.debug("Compressed chunk for " + game.getName() + " exceeds boundaries");
                return getUncompressedGameChunk(game, cBlockOffset);
            } else {
                GameChunk gameChunk = new GameChunk();
                gameChunk.setAddress(cBlockOffset);
                gameChunk.setData(compressedData);
                LOGGER.debug("Compressed chunk for game " + game.getName() + " calculated offset " +
                    gameChunk.getAddress());
                return gameChunk;
            }
        }
    }

    private static GameChunk getUncompressedGameChunk(Game game, int cBlockOffset) throws IOException {
        GameChunk gameChunk = new GameChunk();
        if (game instanceof RamGame) {
            gameChunk.setData(Arrays.copyOfRange(game.getSlot(DandanatorMiniConstants.GAME_CHUNK_SLOT),
                    Constants.SLOT_SIZE - DandanatorMiniConstants.GAME_CHUNK_SIZE,
                    Constants.SLOT_SIZE));
            gameChunk.setAddress(cBlockOffset);
        } else if (game instanceof RomGame) {
            gameChunk.setData(new byte[0]);
            gameChunk.setAddress(cBlockOffset);
        }
        LOGGER.debug("Uncompressed chunk for game " + game.getName() + " calculated offset " +
                gameChunk.getAddress());
        return gameChunk;
    }

    private static GameChunk[] calculateGameChunkTable(Collection<Game> games, int cBlockOffset) throws IOException {
        List<GameChunk> chunkList = new ArrayList<>();
        for (Game game : games) {
            if (game instanceof RamGame) {
                RamGame ramGame = (RamGame) game;
                GameChunk gameChunk = ramGame.getCompressed() ?
                        getCompressedGameChunk(ramGame, cBlockOffset) :
                        getUncompressedGameChunk(game, cBlockOffset);
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
            for (byte[] compressedSlot : ramGame.getCompressedData(ramGameCompressor)) {
                if (compressedSlot != null) {
                    os.write(compressedSlot);
                    LOGGER.debug("Dumped compressed slot for game " + ramGame.getName()
                            + " of size: " + compressedSlot.length);
                } else {
                    LOGGER.debug("Skipped zeroed slot");
                }
            }
        }
    }

    private void dumpUncompressedGameData(OutputStream os, Game game) throws IOException {
        for (int i = game.getSlotCount() - 1; i >= 0; i--) {
            if (!game.isSlotZeroed(i)) {
                os.write(game.getSlot(i));
                LOGGER.debug("Dumped uncompressed slot " + i + " for game " + game.getName());
            } else {
                LOGGER.debug("Skipped zeroed slot");
            }
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

            ExtendedCharSet extendedCharset = new ExtendedCharSet(configuration.getCharSet());
            byte[] compressedCharSetAndFirmware = compress(extendedCharset.getCharSet(),
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

            int eepromLocation = 0;
            int freeSpace = Constants.SLOT_SIZE - os.size() - DandanatorMiniConstants.VERSION_SIZE - 1;
            byte[] eepromLoader = getEepromLoader(os.size());
            if (eepromLoader.length <= freeSpace) {
                eepromLocation = os.size();
                LOGGER.debug("Dumping EEPROM Loader with size " + eepromLoader.length
                    + " at offset " + eepromLocation + ". Free space was " + freeSpace);
                os.write(eepromLoader);
            } else {
                LOGGER.debug("Skipping EEPROM Loader. Not enough free space: " +
                    freeSpace + ". Needed: " + eepromLoader.length);
            }
            fillWithValue(os, (byte) 0, Constants.SLOT_SIZE - os.size() - DandanatorMiniConstants.VERSION_SIZE - 1);
            LOGGER.debug("Dumped padding zone. Offset: " + os.size());

            os.write(dmConfiguration.isDisableBorderEffect() ? 1 : 0);
            os.write(asNullTerminatedByteArray(getVersionInfo(), VERSION_SIZE - 2));
            LOGGER.debug("Dumped version info. Offset: " + os.size());
            Util.writeAsLittleEndian(os, eepromLocation);
            LOGGER.debug("Dumped EEPROM location. Offset: " + os.size());


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
                //Calculate compression always here, to avoid locking the GUI later
                int compressedSize = ramGame.getCompressedSize(ramGameCompressor);
                return ramGame.getCompressed() ? compressedSize : ramGame.getSize();
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

    @Override
    public Future<OperationResult> addGame(Game game) {
        return getApplicationContext().addBackgroundTask(() -> {
                try {
                    //Force compression calculation
                    getGameSize(game);
                    Platform.runLater(() -> getApplicationContext().getGameList().add(game));
                } catch (Exception e) {
                    LOGGER.error("Calculating game size", e);
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

    private static int getGameSymbolCode(Game game) {
        switch (game.getType()) {
            case ROM :
                return ExtendedCharSet.SYMBOL_ROM_0_CODE;
            case RAM16:
                return ExtendedCharSet.SYMBOL_16K_0_CODE;
            case RAM48:
                return ExtendedCharSet.SYMBOL_48K_0_CODE;
            case RAM128:
                return ExtendedCharSet.SYMBOL_128K_0_CODE;
            default:
                LOGGER.error("Unable to get a symbol for game of type " + game.getType());
                return 32;
        }
    }

    private static void printGameNameLine(ZxScreen screen, Game game, int index, int line) {
        screen.setPen(
                isGameScreenHold(game) ? ZxColor.BRIGHTCYAN : ZxColor.BRIGHTGREEN);
        screen.deleteLine(line);
        screen.printLine(String.format("%1d", (index + 1) % DandanatorMiniConstants.SLOT_COUNT), line, 0);
        screen.setPen(ZxColor.BRIGHTWHITE);
        int gameSymbolCode = getGameSymbolCode(game);
        screen.printLine(String.format("%c", gameSymbolCode), line, 1);
        if (isGameCompressed(game)) {
            screen.setPen(ZxColor.BRIGHTYELLOW);
        }
        screen.printLine(String.format("%c", gameSymbolCode + 1), line, 2);
        screen.setPen(isGameScreenHold(game) ? ZxColor.BRIGHTCYAN : ZxColor.BRIGHTGREEN);
        screen.printLine(
                String.format("%s", game.getName()), line, 3);
    }

    private void updateMenuPage(List<Game> gameList, int pageIndex, int numPages) throws IOException {
        DandanatorMiniConfiguration configuration = DandanatorMiniConfiguration.getInstance();
        ZxScreen page = menuImages[pageIndex];
        updateBackgroundImage(page);
        page.setCharSet(new ExtendedCharSet(Configuration.getInstance().getCharSet()).getCharSet());

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
            printGameNameLine(page, game, gameCount++, line++);
            gameIndex++;
        }

        page.setPen(ZxColor.BRIGHTWHITE);
        page.printLine(String.format("P. %s", configuration.getTogglePokesMessage()), 21, 0);
        page.setPen(ZxColor.BRIGHTRED);
        page.printLine(String.format("R. %s", configuration.getExtraRomMessage()), 23, 0);
    }

    @Override
    public void updateMenuPreview() {
        LOGGER.debug("updateMenuPreview");
        try {
            List<Game> gameList = getApplicationContext().getGameList();
            int numPages = 1 + ((gameList.size() - 1) / DandanatorMiniConstants.SLOT_COUNT);
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
        Configuration.getInstance().setRamGameCompressor(ramGameCompressor);
    }

    @Override
    public void unbind() {
        super.unbind();
        previewUpdateTimer.stop();
    }
}